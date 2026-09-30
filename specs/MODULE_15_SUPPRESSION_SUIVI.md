# MODULE 15 : Suppression du suivi et mise en avant des réponses

Statut : à faire
Prérequis : module 14 validé

## Objectif

Simplifier le modèle en supprimant la notion de sujet suivi. Désormais, **toute
nouvelle réponse sur n'importe quel sujet** le met en avant, et la notification
signale les réponses par forum.

Seule la notion de sujet masqué est conservée, avec une règle nouvelle : une réponse
démasque le sujet.

## Justification

L'icône « Complet » dépend de la rigueur des meneurs de jeu et ne reflète pas toujours
l'état réel d'une partie. De vieux sujets se réactivent parfois après un message. Une
réponse est un fait objectif : un sujet qui en reçoit une mérite de revenir sous les
yeux de l'utilisateur, même s'il l'avait masqué.

## Ce qui disparaît

- L'icône de cloche sur les cartes de sujet.
- Le tri plaçant les sujets suivis en tête de liste.
- La ligne « Suivis : z » des cartes de l'écran d'accueil.
- La protection des sujets suivis contre la purge des trente jours.
- L'exclusion des sujets suivis dans « Masquer les COMPLET ».
- La coupure du suivi lors du masquage, et sa restauration par « Annuler ».
- Les lignes de notification par sujet, du type « 3 réponses à Recrutement Vermines ».
- Les outils de l'écran de debug liés au suivi, notamment « Basculer surveillé ».

La colonne `isWatched` **reste en base**, afin d'éviter une migration Room. Plus aucun
code ne la lit ni ne l'utilise pour décider d'un comportement. L'export du module 12
l'écrit à `false`, l'import l'ignore.

## Règle centrale : une nouvelle réponse

Une nouvelle réponse est détectée lorsque le nombre de réponses analysé d'un sujet
déjà présent en base est **strictement supérieur** à celui stocké. La règle s'applique
désormais à **tous** les sujets, et non plus aux seuls sujets suivis.

Lorsqu'une nouvelle réponse est détectée, la synchronisation :

1. passe `isRead` à faux : le sujet apparaît en gras ;
2. passe `isHidden` à faux : un sujet masqué redevient visible ;
3. met à jour `lastPostAt` comme d'habitude, ce qui le fait remonter en tête de liste
   par le tri existant.

Ces trois effets sont appliqués dans `SyncForumUseCase`, au moment de la fusion, là où
était auparavant appliquée la règle réservée aux sujets suivis.

### Ce qui ne démasque pas

Seule une nouvelle réponse démasque un sujet. Les changements suivants ne le font
**pas** :

- un changement de titre ;
- l'apparition ou la disparition de l'icône « Complet » ;
- un changement d'auteur du dernier message sans augmentation du nombre de réponses.

### Amorçage

Pendant l'amorçage d'un forum, aucune réponse n'est signalée et aucun sujet n'est
démasqué, comme pour les nouveaux sujets.

### Préservation des états utilisateur

La règle du module 04 est assouplie sur un seul point : `isHidden` n'est plus
systématiquement préservé, il repasse à faux en cas de nouvelle réponse. Dans tous les
autres cas, il reste inchangé.

## Écran de détail d'un forum

### Carte de sujet

- L'icône de cloche est retirée.
- L'icône d'oeil reste, avec sa logique d'état : oeil ouvert pour un sujet visible,
  oeil barré pour un sujet masqué.
- La pastille « Complet » reste à gauche, sur la ligne des icônes.

### Tri

Tri unique par `lastPostAt` décroissant. La règle « sujets suivis en premier » est
supprimée.

### Masquage

Masquer un sujet ne touche plus qu'à `isHidden`. L'action « Annuler » ne restaure donc
plus que la visibilité.

### Masquer les COMPLET

L'action masque **tous** les sujets complets non déjà masqués, sans exception.
L'annulation continue de rétablir exactement les sujets concernés par cette action.

## Écran d'accueil

Chaque carte de forum affiche désormais deux lignes de compteurs :

| Ligne | Format | Contenu |
|---|---|---|
| 1 | `Sujets : x/y` | `x` sujets non masqués, `y` total en base |
| 2 | `w nouveaux` | `w` sujets non lus et non masqués |

La ligne « Suivis : z » est supprimée. La ligne 2 reste masquée quand `w` vaut zéro.

Le compteur `w` change de nature sans changer de libellé : il inclut désormais les
sujets ayant reçu une réponse depuis leur dernière ouverture, et non plus seulement les
sujets jamais ouverts.

## Notification

### Nouvelles lignes de réponses

Pour chaque forum ayant reçu au moins une nouvelle réponse lors de la dernière
synchronisation, une ligne s'ajoute au texte de contenu :

```
Oneshots : +5 réponses
Campagnes : +1 réponse
```

- Le nom du forum, puis le total des nouvelles réponses détectées sur l'ensemble de
  ses sujets, précédé d'un signe `+` et accordé au pluriel.
- Un forum sans nouvelle réponse n'a pas de ligne de réponses.
- Le total inclut les réponses ayant démasqué un sujet.

### Ordre des lignes

Les lignes sont regroupées par forum, dans l'ordre des identifiants de forum. Pour
chaque forum : d'abord la ligne de nouveaux sujets, puis la ligne de réponses.

```
Dernière synchro
Oneshots (3) : Nouvelle aventure D&D
Oneshots : +5 réponses
Campagnes : +1 réponse
```

La limite de cinq lignes et la ligne « et N autres » du module 14 restent en vigueur.

### Son et bandeau

Seuls les **nouveaux sujets** déclenchent son, vibration et bandeau. Les réponses
mettent à jour le texte en silence.

| Situation | `setOnlyAlertOnce` |
|---|---|
| Au moins un nouveau sujet | `false`, alerte |
| Uniquement des réponses | `true`, silencieux |
| Aucune nouveauté | `true`, silencieux |
| Échec ou vérification requise | `false`, alerte, inchangé |

Le paramètre de `StatusNotifier.update()` reflète donc la présence de **nouveaux
sujets**, et non la présence de nouveautés en général. Il est renommé `hasNewTopics`
pour que ce sens soit sans ambiguïté.

### Retour à l'état de repos

La notification revient à « Oneshots (X) - Campagnes (Y) » à la première
synchronisation suivante n'ayant détecté **ni nouveau sujet ni nouvelle réponse**.

## Persistance des nouveautés

La structure `SyncHighlights` du module 14 évolue :

```json
{
  "newTopicsByForum": { "15": ["Nouvelle aventure D&D"] },
  "newReplyCountByForum": { "15": 5, "16": 1 }
}
```

Le champ `newRepliesByTopic` est remplacé par `newReplyCountByForum`. Grâce à
`ignoreUnknownKeys`, un contenu persisté par la version précédente est lu sans erreur,
l'ancien champ étant simplement ignoré.

`SystemNewContentNotifier.notifyNewReplies` additionne les réponses par forum au lieu
de les ventiler par sujet. Son contrat reste inchangé.

## Journal des notifications

Le journal continue d'enregistrer les nouvelles réponses, désormais pour tous les
sujets. Il reste le seul endroit où retrouver le détail par sujet.

## Purge

`deleteStale` supprime désormais tous les sujets dont `lastSeenAt` est antérieur au
seuil, sans condition sur `isWatched`.

## Écran de debug

- Le bouton « Basculer surveillé » est retiré.
- L'affichage des drapeaux d'un sujet ne mentionne plus le suivi.
- « Décrémenter le compteur de réponses » s'applique désormais à n'importe quel sujet
  sélectionné, masqué ou non, ce qui permet de tester le démasquage.

## Architecture

Aucune classe nouvelle. Modifications uniquement.

| Classe | Modification |
|---|---|
| `SyncForumUseCase` | Détection des réponses sur tous les sujets, démasquage, passage en non lu |
| `SystemNewContentNotifier` | Agrégation des réponses par forum |
| `SyncHighlights` | Nouveau champ `newReplyCountByForum` |
| `StatusNotifier` | Lignes de réponses, paramètre `hasNewTopics` |
| `SyncWorker` | Calcul de `hasNewTopics` à partir des seuls nouveaux sujets |
| `TopicCard` | Retrait de la cloche |
| `ForumDetailViewModel` | Tri, masquage, « Masquer les COMPLET » simplifiés |
| `HomeViewModel`, `ForumCard` | Retrait du compteur de suivis |
| `TopicDao` | `deleteStale` sans condition sur `isWatched` |
| `BackupRepositoryImpl` ou cas d'usage | `isWatched` écrit à `false` à l'export, ignoré à l'import |

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Réponse sur un sujet visible | Gras, remonte en tête |
| Réponse sur un sujet masqué | Redevient visible, gras, en tête |
| Réponse sur un sujet complet masqué par « Masquer les COMPLET » | Redevient visible |
| Sujet masqué dont le titre change | Reste masqué |
| Sujet masqué qui perd l'icône « Complet » | Reste masqué |
| Uniquement des réponses lors d'une synchronisation | Texte mis à jour, aucun son |
| Nouveaux sujets et réponses ensemble | Son et bandeau, toutes les lignes affichées |
| Amorçage | Aucun démasquage, aucune ligne de réponses |
| Contenu persisté par le module 14 | Lu sans erreur, ancien champ ignoré |
| Import d'une sauvegarde contenant des sujets suivis | Le suivi est ignoré |
| Réponse postée par l'utilisateur lui-même | Traitée comme toute autre, l'application étant anonyme |

## Fichiers autorisés

```
app/src/main/java/com/jdrvirtuel/watcher/domain/usecase/SyncForumUseCase.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/model/SyncHighlights.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/SystemNewContentNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/StatusNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/work/SyncWorker.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/dao/TopicDao.kt
app/src/main/java/com/jdrvirtuel/watcher/data/repository/TopicRepositoryImpl.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/repository/TopicRepository.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/forumdetail/TopicCard.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/forumdetail/ForumDetailScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/forumdetail/ForumDetailViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/forumdetail/ForumDetailContract.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeContract.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/ForumCard.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugContract.kt
app/src/main/java/com/jdrvirtuel/watcher/data/repository/BackupRepositoryImpl.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/usecase/ExportBackupUseCase.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/usecase/ImportBackupUseCase.kt
app/src/main/res/values/strings.xml
```

Aucune dépendance nouvelle. Aucune modification du schéma Room.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
gradlew testDebugUnitTest
```

### Scénario manuel

1. Ouvrir un forum.
   Attendu : aucune cloche sur les cartes, seul l'oeil subsiste. Les sujets sont triés
   par date du dernier message.
2. Ouvrir l'accueil.
   Attendu : deux lignes de compteurs par forum, plus de ligne « Suivis ».
3. Ouvrir un sujet dans le navigateur, revenir.
   Attendu : il n'est plus en gras.
4. Écran de debug : sélectionner ce sujet, décrémenter son compteur de réponses de
   deux, synchroniser.
   Attendu : il repasse en gras et remonte en tête. La notification affiche
   « Oneshots : +2 réponses », **sans son ni bandeau**.
5. Masquer un sujet, puis le sélectionner dans l'écran de debug, décrémenter son
   compteur de réponses, synchroniser.
   Attendu : il réapparaît dans la liste, en gras, en tête.
6. Masquer un sujet, puis modifier son état « Complet » depuis l'écran de debug si
   l'outil le permet, ou attendre un changement de titre, et synchroniser sans
   provoquer de réponse.
   Attendu : il reste masqué.
7. Supprimer un sujet au hasard et décrémenter les réponses d'un autre, synchroniser.
   Attendu : son et bandeau. La notification affiche la ligne du nouveau sujet puis la
   ligne de réponses, regroupées par forum.
8. Décrémenter une seule réponse sur un sujet.
   Attendu : ligne « +1 réponse », au singulier.
9. Resynchroniser sans provoquer de nouveauté.
   Attendu : retour à « Oneshots (X) - Campagnes (Y) », aucun son.
10. Utiliser « Masquer les COMPLET ».
    Attendu : tous les sujets complets disparaissent, sans exception.
11. Vérifier le journal des notifications.
    Attendu : les nouvelles réponses y figurent, sujet par sujet.
12. Exporter les données, puis inspecter le fichier.
    Attendu : tous les sujets portent `isWatched` à `false`.

## Travail attendu de Gemini

Points d'attention, par ordre d'importance :

1. La détection des réponses s'applique désormais à **tous** les sujets, et une
   réponse **démasque** le sujet. C'est le coeur du module.
2. Seuls les nouveaux sujets alertent. Le paramètre de `StatusNotifier.update()` est
   renommé `hasNewTopics` et calculé à partir des seuls nouveaux sujets, faute de quoi
   les réponses feraient sonner le téléphone à chaque synchronisation.
3. La colonne `isWatched` reste en base, mais plus aucun code ne doit s'en servir. Une
   recherche de toutes ses occurrences est attendue dans le compte rendu.
4. Aucune migration Room.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 14 est validé. Lis `specs/00_SPECIFICATIONS_GENERALES.md` puis
> `specs/MODULE_15_SUPPRESSION_SUIVI.md` et implémente uniquement le module 15. Points
> critiques : toute nouvelle réponse sur n'importe quel sujet le passe en non lu et le
> démasque ; seuls les nouveaux sujets déclenchent son et bandeau, les réponses mettent
> à jour la notification en silence ; la colonne `isWatched` reste en base mais n'est
> plus utilisée nulle part, sans migration. Liste dans ton compte rendu toutes les
> occurrences restantes de `isWatched`. Respecte la liste des fichiers autorisés et
> termine par le compte rendu demandé.
