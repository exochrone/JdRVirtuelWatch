# MODULE 12 : Compteurs d'accueil et sauvegarde

Statut : à faire
Prérequis : module 11 validé

## Objectif

Trois évolutions indépendantes :

1. enrichir les compteurs affichés sur les cartes de l'écran d'accueil ;
2. réorganiser l'écran de réglages ;
3. permettre d'exporter et de réimporter le contenu des forums avec les états
   utilisateur, afin de ne rien perdre lors d'un changement de version.

La troisième est la plus structurante : elle répond au fait qu'une réinstallation
efface aujourd'hui tous les masquages, suivis et marquages de lecture accumulés.

## Partie 1 : compteurs de l'écran d'accueil

### Contenu attendu

Chaque carte de forum affiche trois lignes de compteurs, sous le nom du forum, en
`bodySmall` et en couleur secondaire.

| Ligne | Format | Contenu |
|---|---|---|
| 1 | `Sujets : x/y` | `x` sujets non masqués, `y` total en base |
| 2 | `w nouveaux` | `w` sujets non lus |
| 3 | `Suivis : z` | `z` sujets sous surveillance |

Le mot « Sujet » s'accorde selon `y`, « nouveau » selon `w`, « Suivi » selon `z`. Les
trois accords passent par des ressources `plurals`, jamais par une concaténation.

Le libellé actuel « N sujets » de la carte est remplacé par la ligne 1.

### Règles

- `y` compte tous les sujets du forum en base, masqués et complets compris.
- `x` compte les sujets dont `isHidden` est faux.
- `w` compte les sujets dont `isRead` est faux **et** `isHidden` est faux, ce qui
  correspond au compteur déjà utilisé par le badge.
- `z` compte les sujets dont `isWatched` est vrai. Un sujet masqué ayant
  nécessairement `isWatched` à faux, la question du cumul ne se pose pas.
- Une ligne dont la valeur est zéro reste affichée, afin que la carte garde une
  hauteur stable d'un forum à l'autre. Seule exception : la ligne 2 est masquée quand
  `w` vaut zéro, puisqu'elle fait double emploi avec l'absence de badge.

### Architecture

`ForumUiModel` reçoit trois champs supplémentaires : `visibleCount`, `totalCount`,
`watchedCount`. Le champ `unreadCount` existe déjà.

Les compteurs viennent de `TopicRepository`. Si les requêtes correspondantes n'existent
pas, elles sont ajoutées à `TopicDao` et `TopicRepository` sous forme de `Flow<Int>`,
sur le modèle de `observeUnreadCount`. C'est la seule modification autorisée dans les
couches `data` et `domain` pour cette partie.

Le `HomeViewModel` combine ces flux. Attention au nombre de flux combinés : au-delà de
cinq, imbriquer plusieurs `combine` plutôt que d'employer la variante à tableau, qui
produit des avertissements de cast non vérifié.

## Partie 2 : réorganisation de l'écran de réglages

L'ordre des sections devient :

1. **Données** : nombre de sujets stockés, export et import (partie 3), effacement des
   données locales, et la section dépliable « Journal des synchronisations » ;
2. **Surveillance** : forums surveillés, URL, dates de dernière synchronisation,
   période de rafraîchissement ;
3. **Notifications** : bouton d'accès aux réglages système et section dépliable
   « Journal des notifications » ;
4. **Navigation** : sélecteur du navigateur ;
5. **Diagnostic** : lien vers l'écran de diagnostic ;
6. **À propos** : nom, version, mention sur la consultation de pages publiques ;
7. **Développement**, visible uniquement si `BuildConfig.DEBUG` : accès à l'écran de
   debug.

Aucun contenu n'est ajouté ni retiré, seul l'ordre change.

## Partie 3 : export et import

### Objectif

Permettre de conserver les états utilisateur au travers d'une réinstallation. Ces
états représentent un travail accumulé que rien ne permet aujourd'hui de récupérer.

### Contenu du fichier

Format JSON, sérialisé avec `kotlinx.serialization`, déjà présent depuis le module 00.

```json
{
  "formatVersion": 1,
  "exportedAt": 1786000000000,
  "appVersion": "1.0",
  "forums": [
    {
      "id": 15,
      "name": "Oneshots",
      "topics": [
        {
          "id": 41234,
          "title": "[Friponnes RPG][Discord][12/08][1/3 places]",
          "url": "https://www.jdrvirtuel.com/viewtopic.php?f=15&t=41234",
          "author": "Etienneb",
          "createdAt": 1785000000000,
          "replyCount": 10,
          "lastPostAuthor": "Weyland-Yutani Corp",
          "lastPostAt": 1785964854000,
          "isFull": false,
          "isHidden": false,
          "isWatched": true,
          "isRead": true,
          "firstSeenAt": 1785000000000,
          "lastSeenAt": 1785964854000
        }
      ]
    }
  ]
}
```

Le champ `formatVersion` est obligatoire. Il permettra à une version ultérieure de
refuser ou de convertir un fichier ancien plutôt que de l'interpréter de travers.

Les préférences ne sont pas exportées : navigateur choisi, journaux, compteurs
Cloudflare et réglages de diagnostic restent locaux. Seuls les forums et leurs sujets
sont concernés.

### Export

- Bouton « Exporter les données » dans la section Données.
- Ouvre un sélecteur d'enregistrement via `Intent.ACTION_CREATE_DOCUMENT`, type
  `application/json`, nom proposé
  `jdrvirtuelwatcher-AAAAMMJJ-HHmmss.json`.
- Aucune permission n'est requise : le Storage Access Framework donne un accès ponctuel
  au fichier choisi par l'utilisateur.
- L'écriture se fait sur `Dispatchers.IO`, via le `ContentResolver`.
- À la fin, un message temporaire indique le nombre de sujets exportés.
- En cas d'échec, un message d'erreur explicite, sans plantage.

### Import

- Bouton « Importer des données » dans la section Données.
- Ouvre `Intent.ACTION_OPEN_DOCUMENT`, filtre `application/json`.
- Le fichier est lu, désérialisé et validé avant toute écriture en base.
- Un `AlertDialog` de confirmation résume ce qui va se passer : nombre de forums,
  nombre de sujets, date d'export du fichier, et le mode de fusion retenu.
- Rien n'est écrit avant confirmation.

### Règles de fusion

C'est le point le plus délicat de ce module. L'import ne remplace jamais la base : il
**restaure les états utilisateur**, sans écraser les données fraîches issues du forum.

Pour chaque sujet du fichier :

| Situation | Comportement |
|---|---|
| Le sujet existe en base | Seuls `isHidden`, `isWatched`, `isRead` et `firstSeenAt` sont restaurés depuis le fichier. Tous les autres champs conservent leur valeur locale, plus récente |
| Le sujet n'existe pas en base | Il est inséré intégralement depuis le fichier |
| Un sujet est en base mais absent du fichier | Il est laissé strictement intact |

Les forums ne sont jamais créés ni supprimés par un import : seuls les sujets
rattachés à un forum déjà présent en base sont traités. Un forum inconnu du fichier
est ignoré, et son cas est signalé dans le compte rendu affiché à l'utilisateur.

L'invariant du module 01 reste prioritaire : un sujet importé avec `isHidden` à vrai et
`isWatched` à vrai voit sa surveillance forcée à faux.

Un import ne déclenche aucune notification, quelles que soient les différences
constatées. Il ne modifie pas non plus `isBootstrapped`.

### Compte rendu d'import

À l'issue de l'opération, un `AlertDialog` récapitule :

- nombre de sujets dont les états ont été restaurés ;
- nombre de sujets insérés ;
- nombre de sujets du fichier ignorés faute de forum correspondant ;
- nombre de sujets locaux laissés intacts.

### Validation du fichier

Un fichier est refusé, avec un message clair et sans aucune écriture, si :

- il n'est pas un JSON valide ;
- `formatVersion` est absent ou supérieur à la version prise en charge ;
- la structure attendue est absente.

Un champ optionnel manquant sur un sujet ne fait pas échouer l'import : le sujet
concerné est ignoré et compté comme tel.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `BackupData` | `domain.model` | Racine du format, `@Serializable` |
| `BackupForum` | `domain.model` | Forum et ses sujets |
| `BackupTopic` | `domain.model` | Sujet exporté |
| `BackupResult` | `domain.model` | Compte rendu d'import |
| `BackupRepository` | `domain.repository` | Interface d'export et d'import |
| `BackupRepositoryImpl` | `data.repository` | Implémentation |
| `ExportBackupUseCase` | `domain.usecase` | Construction du contenu à exporter |
| `ImportBackupUseCase` | `domain.usecase` | Validation et fusion |

L'accès au `ContentResolver` reste dans la couche `feature`, via les contrats
`ActivityResultContracts.CreateDocument` et `OpenDocument`. Les cas d'usage
manipulent des chaînes de caractères et non des URI, afin que la couche `domain` reste
sans dépendance Android.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Base vide au moment de l'export | Fichier valide contenant zéro sujet, message explicite |
| Import d'un fichier vide | Aucune modification, compte rendu à zéro |
| Import du fichier que l'on vient d'exporter | Aucune modification visible, tous les états identiques |
| Import annulé au sélecteur | Aucune écriture, aucun message d'erreur |
| Fichier corrompu ou tronqué | Message d'erreur, aucune écriture |
| `formatVersion` inconnue | Refus explicite mentionnant la version |
| Sujet importé masqué et surveillé | La surveillance est forcée à faux |
| Import pendant une synchronisation | Le `Mutex` existant sérialise les deux opérations |
| Fichier volumineux | L'opération reste sur `Dispatchers.IO`, un indicateur de progression est affiché |

## Fichiers autorisés

```
app/src/main/java/com/jdrvirtuel/watcher/domain/model/BackupData.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/model/BackupResult.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/repository/BackupRepository.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/usecase/ExportBackupUseCase.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/usecase/ImportBackupUseCase.kt
app/src/main/java/com/jdrvirtuel/watcher/data/repository/BackupRepositoryImpl.kt
app/src/main/java/com/jdrvirtuel/watcher/core/di/BackupModule.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsContract.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeContract.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/ForumCard.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/dao/TopicDao.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/repository/TopicRepository.kt
app/src/main/java/com/jdrvirtuel/watcher/data/repository/TopicRepositoryImpl.kt
app/src/main/res/values/strings.xml
```

Aucune dépendance nouvelle. Aucune modification du schéma Room, donc aucune migration.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
gradlew testDebugUnitTest
```

### Scénario manuel

**Compteurs**

1. Ouvrir l'accueil après une synchronisation.
   Attendu : chaque carte affiche les trois lignes, avec `x` égal à `y` si aucun sujet
   n'est masqué, et `z` à zéro.
2. Masquer trois sujets dans un forum, revenir à l'accueil.
   Attendu : `x` a diminué de trois, `y` est inchangé.
3. Mettre deux sujets sous surveillance.
   Attendu : `z` vaut deux.
4. Ouvrir un sujet non lu dans le navigateur, revenir.
   Attendu : `w` a diminué de un.
5. Vérifier les accords au singulier avec exactement un sujet suivi.

**Réglages**

6. Ouvrir l'écran de réglages.
   Attendu : la section Données apparaît en premier, avec le journal des
   synchronisations à l'intérieur.

**Export et import**

7. Masquer deux sujets, en suivre un, en lire un autre. Noter précisément lesquels.
8. Exporter les données, choisir un emplacement.
   Attendu : message indiquant le nombre de sujets exportés, fichier présent sur
   l'appareil.
9. Désinstaller l'application, la réinstaller, la lancer, synchroniser.
   Attendu : les sujets sont là, mais tous visibles, non suivis, non lus.
10. Importer le fichier exporté.
    Attendu : confirmation annonçant le bon nombre de sujets, puis compte rendu, puis
    les masquages, suivis et lectures notés à l'étape 7 sont rétablis à l'identique.
11. Réimporter le même fichier une seconde fois.
    Attendu : aucun changement, compte rendu cohérent, aucun doublon.
12. Importer un fichier texte quelconque renommé en `.json`.
    Attendu : message d'erreur explicite, aucune modification de la base.
13. Lancer un import puis l'annuler au sélecteur de fichier.
    Attendu : aucun message d'erreur, aucune modification.
14. Vérifier qu'aucune notification n'a été émise pendant les imports.

## Travail attendu de Gemini

Traiter les trois parties. La partie 3 est la plus délicate : la règle de fusion doit
être respectée à la lettre, en particulier le fait qu'un import ne remplace jamais les
données issues du forum mais uniquement les états utilisateur.

Ne pas modifier le schéma Room. Ne pas écrire de migration.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 11 est validé. Lis `specs/00_SPECIFICATIONS_GENERALES.md` puis
> `specs/MODULE_12_COMPTEURS_ET_SAUVEGARDE.md` et implémente uniquement le module 12.
> Point d'attention principal : l'import ne remplace jamais la base, il restaure
> uniquement `isHidden`, `isWatched`, `isRead` et `firstSeenAt` sur les sujets déjà
> présents, et insère ceux qui manquent. Aucune notification ne doit être émise
> pendant un import. Respecte la liste des fichiers autorisés et termine par le compte
> rendu demandé.
