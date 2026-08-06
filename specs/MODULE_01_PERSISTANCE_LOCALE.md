# MODULE 01 : Persistance locale

Statut : à faire
Prérequis : module 00 validé

## Objectif

Mettre en place la totalité du stockage local : base Room avec ses deux entités, ses
DAO, l'amorçage des deux forums surveillés, les préférences DataStore, et l'écran de
debug qui permet de vérifier le contenu de la base.

Le schéma est défini une fois pour toutes dans ce module. Les modules suivants ne
créent aucune colonne et n'écrivent aucune migration. La base reste en version 1 pour
toute la version 1.0 de l'application.

Ce module ne fait pas : aucun accès réseau, aucune analyse de HTML, aucune
synchronisation.

## Fonctionnalités

- La base est créée au premier lancement et contient les deux forums surveillés.
- Les sujets peuvent être lus, insérés, mis à jour et supprimés.
- Les préférences sont lisibles et modifiables.
- Un écran de debug affiche le contenu réel des tables.

## Modèle de données

### ForumEntity

Table `forums`.

| Champ | Type Kotlin | Contrainte |
|---|---|---|
| `id` | `Int` | clé primaire, valeur imposée 15 ou 16 |
| `name` | `String` | non nul |
| `url` | `String` | non nul |
| `lastSyncAt` | `Long?` | nul tant qu'aucune synchronisation n'a réussi |
| `lastSyncSuccess` | `Boolean` | faux par défaut |
| `lastSyncError` | `String?` | nul par défaut |
| `isBootstrapped` | `Boolean` | faux par défaut |

### TopicEntity

Table `topics`, index sur `forumId`.

| Champ | Type Kotlin | Contrainte |
|---|---|---|
| `id` | `Int` | clé primaire, identifiant phpBB du sujet |
| `forumId` | `Int` | clé étrangère vers `forums.id`, `onDelete = CASCADE` |
| `title` | `String` | non nul |
| `url` | `String` | non nul |
| `author` | `String` | non nul |
| `createdAt` | `Long` | millisecondes epoch |
| `replyCount` | `Int` | zéro par défaut |
| `lastPostAuthor` | `String` | non nul |
| `lastPostAt` | `Long` | millisecondes epoch |
| `isFull` | `Boolean` | faux par défaut |
| `isHidden` | `Boolean` | faux par défaut |
| `isWatched` | `Boolean` | faux par défaut |
| `isRead` | `Boolean` | faux par défaut |
| `firstSeenAt` | `Long` | millisecondes epoch |
| `lastSeenAt` | `Long` | millisecondes epoch |

### Modèles de domaine

`domain.model.Forum` et `domain.model.Topic` reprennent les mêmes champs, sans
annotation Room. Les dates y sont exposées en `Long`, la conversion en texte
appartient à la couche présentation.

`data.mapper.ForumMapper` et `data.mapper.TopicMapper` assurent les conversions dans
les deux sens, sous forme de fonctions d'extension `toDomain()` et `toEntity()`.

## Architecture

### Classes à créer

| Classe | Package | Rôle |
|---|---|---|
| `ForumEntity` | `data.local.entity` | Entité Room |
| `TopicEntity` | `data.local.entity` | Entité Room |
| `ForumDao` | `data.local.dao` | Accès aux forums |
| `TopicDao` | `data.local.dao` | Accès aux sujets |
| `AppDatabase` | `data.local.db` | Base Room, version 1 |
| `DatabaseSeeder` | `data.local.db` | Amorçage des deux forums |
| `AppPreferences` | `data.local.prefs` | Accès DataStore |
| `Forum` | `domain.model` | Modèle métier |
| `Topic` | `domain.model` | Modèle métier |
| `ForumMapper` | `data.mapper` | Conversions |
| `TopicMapper` | `data.mapper` | Conversions |
| `ForumRepository` | `domain.repository` | Interface |
| `TopicRepository` | `domain.repository` | Interface |
| `ForumRepositoryImpl` | `data.repository` | Implémentation |
| `TopicRepositoryImpl` | `data.repository` | Implémentation |
| `DatabaseModule` | `core.di` | Fourniture Hilt de la base et des DAO |
| `RepositoryModule` | `core.di` | Liaisons `@Binds` des dépôts |
| `DebugScreen` | `feature.debug` | Écran de vérification |
| `DebugViewModel` | `feature.debug` | État de l'écran de debug |
| `DebugUiState` | `feature.debug` | État |
| `DebugEvent` | `feature.debug` | Actions |

### Fonctions attendues des DAO

`ForumDao` :

- `observeAll(): Flow<List<ForumEntity>>`
- `observeById(id: Int): Flow<ForumEntity?>`
- `getById(id: Int): ForumEntity?`
- `upsert(forum: ForumEntity)`
- `count(): Int`

`TopicDao` :

- `observeByForum(forumId: Int): Flow<List<TopicEntity>>`
- `getByForum(forumId: Int): List<TopicEntity>`
- `getById(id: Int): TopicEntity?`
- `upsertAll(topics: List<TopicEntity>)`
- `updateHidden(id: Int, hidden: Boolean)`
- `updateWatched(id: Int, watched: Boolean)`
- `updateRead(id: Int, read: Boolean)`
- `deleteById(id: Int)`
- `deleteStale(threshold: Long)` supprime les sujets dont `lastSeenAt` est inférieur
  au seuil et dont `isWatched` est faux
- `countUnread(forumId: Int): Flow<Int>` compte les sujets non lus, non masqués

### Amorçage

`DatabaseSeeder` insère les deux forums si la table est vide :

| id | name | url |
|---|---|---|
| 15 | Oneshots | `https://www.jdrvirtuel.com/viewforum.php?f=15` |
| 16 | Campagnes | `https://www.jdrvirtuel.com/viewforum.php?f=16` |

L'amorçage est déclenché par un `RoomDatabase.Callback` sur `onCreate`, exécuté dans
une coroutine sur `Dispatchers.IO`. Il est aussi vérifié au démarrage de
l'application, afin de couvrir le cas d'une base existante mais vide.

Les noms des forums sont écrits dans `strings.xml` et non en dur dans le code.

### Préférences

`AppPreferences` expose :

- `consecutiveChallengeFailures: Flow<Int>` et `setConsecutiveChallengeFailures(v: Int)`
- `lastChallengePromptAt: Flow<Long>` et `setLastChallengePromptAt(v: Long)`

Le `DataStore<Preferences>` est fourni par Hilt en singleton, avec le nom de fichier
`jdrvirtuel_watcher_prefs`.

## Écrans

### Écran de debug, version 1

- **Nom** : `DebugScreen`
- **Objectif** : rendre le contenu de la base visible sans outil externe.
- **Contenu** :
  - une section « Forums » listant chaque forum avec son identifiant, son nom, son
    état d'amorçage et sa date de dernière synchronisation ;
  - une section « Sujets » listant, pour chaque forum, les sujets en base avec leur
    identifiant, leur titre, leur nombre de réponses et leurs trois drapeaux
    `isFull`, `isHidden`, `isWatched` ;
  - un compteur du nombre total de sujets.
- **Actions possibles** :
  - « Insérer un sujet de test » : crée un sujet fictif rattaché au forum 15, avec un
    identifiant aléatoire compris entre 900000 et 999999 ;
  - « Vider les sujets » : supprime tous les sujets, sans toucher aux forums.
- **Navigation** : accessible depuis un bouton de la barre supérieure de l'écran
  d'accueil, retour par le bouton système ou par la flèche de la barre supérieure.

L'écran d'accueil provisoire du module 00 est modifié uniquement pour ajouter ce
bouton d'accès. Aucune autre modification n'y est autorisée.

## Comportement

- L'écran de debug observe la base en continu : une insertion se reflète
  immédiatement dans la liste, sans rafraîchissement manuel.
- Le bouton d'insertion produit un sujet dont les dates de création et de dernier
  message valent l'instant courant.
- Le bouton de purge demande confirmation par un `AlertDialog`.

## Règles métier

- Les deux forums sont figés : ni ajout, ni suppression, ni modification par
  l'utilisateur.
- La suppression d'un forum entraîne la suppression de ses sujets, par cascade Room.
  Ce cas ne se produit pas dans l'application mais la contrainte est posée.
- `deleteStale` ne supprime jamais un sujet sous surveillance.
- Un sujet masqué a nécessairement `isWatched` à faux. Cette règle est appliquée dans
  `TopicRepositoryImpl.setHidden`, qui remet `isWatched` à faux lorsqu'il passe
  `isHidden` à vrai.

## Composants UI

`Scaffold`, `TopAppBar` avec bouton de retour, `LazyColumn`, `Card`, `Button`,
`AlertDialog`, `HorizontalDivider`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Premier lancement | La base est créée, les deux forums apparaissent |
| Aucun sujet en base | La section Sujets affiche un message explicite |
| Redémarrage de l'application | Les sujets insérés sont toujours présents |
| Insertion répétée | Aucun conflit, chaque sujet a un identifiant distinct |
| Rotation de l'écran | Aucune perte, l'état vient du `ViewModel` |
| Base corrompue | `fallbackToDestructiveMigration()` est activé, ce qui recrée la base |

## Fichiers autorisés

```
gradle/libs.versions.toml                      ajout de Room et DataStore uniquement
app/build.gradle.kts                           ajout des dépendances uniquement
app/src/main/res/values/strings.xml
app/src/main/java/com/jdrvirtuel/watcher/data/local/entity/ForumEntity.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/entity/TopicEntity.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/dao/ForumDao.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/dao/TopicDao.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/db/AppDatabase.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/db/DatabaseSeeder.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/prefs/AppPreferences.kt
app/src/main/java/com/jdrvirtuel/watcher/data/mapper/ForumMapper.kt
app/src/main/java/com/jdrvirtuel/watcher/data/mapper/TopicMapper.kt
app/src/main/java/com/jdrvirtuel/watcher/data/repository/ForumRepositoryImpl.kt
app/src/main/java/com/jdrvirtuel/watcher/data/repository/TopicRepositoryImpl.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/model/Forum.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/model/Topic.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/repository/ForumRepository.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/repository/TopicRepository.kt
app/src/main/java/com/jdrvirtuel/watcher/core/di/DatabaseModule.kt
app/src/main/java/com/jdrvirtuel/watcher/core/di/RepositoryModule.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugContract.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/Routes.kt        ajout de DebugRoute
app/src/main/java/com/jdrvirtuel/watcher/navigation/AppNavHost.kt    ajout de la destination
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeScreen.kt  ajout du bouton uniquement
app/src/main/java/com/jdrvirtuel/watcher/JdrVirtuelWatcherApp.kt     appel du seeder uniquement
```

Dépendances à ajouter : `androidx.room:room-runtime`, `androidx.room:room-ktx`,
`androidx.room:room-compiler` en KSP, `androidx.datastore:datastore-preferences`.
Version minimale de Room : 2.6.1.

## Contrat exposé aux modules suivants

- `ForumRepository` : `observeForums(): Flow<List<Forum>>`,
  `observeForum(id: Int): Flow<Forum?>`, `getForum(id: Int): Forum?`,
  `updateSyncState(id, success, at, error)`, `markBootstrapped(id: Int)`.
- `TopicRepository` : `observeTopics(forumId: Int): Flow<List<Topic>>`,
  `getTopics(forumId: Int): List<Topic>`, `upsertAll(topics: List<Topic>)`,
  `setHidden(id, hidden)`, `setWatched(id, watched)`, `setRead(id, read)`,
  `deleteById(id)`, `deleteStale(threshold)`,
  `observeUnreadCount(forumId: Int): Flow<Int>`.
- `AppPreferences` : accès aux deux compteurs liés à Cloudflare.
- `DebugScreen` : écran extensible par les modules 02, 03 et 04.

## Tests

Aucun test automatisé pour ce module. La vérification passe par l'écran de debug.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
```

### Scénario manuel

1. Désinstaller l'application, puis la réinstaller et la lancer.
2. Ouvrir l'écran de debug depuis le bouton de la barre supérieure.
   Résultat attendu : la section Forums affiche exactement deux lignes, « 15 Oneshots »
   et « 16 Campagnes », toutes deux non amorcées et sans date de synchronisation.
3. Vérifier la section Sujets.
   Résultat attendu : un message indiquant qu'aucun sujet n'est présent, compteur à 0.
4. Appuyer trois fois sur « Insérer un sujet de test ».
   Résultat attendu : trois sujets apparaissent immédiatement, compteur à 3.
5. Fermer complètement l'application puis la relancer et rouvrir l'écran de debug.
   Résultat attendu : les trois sujets sont toujours là.
6. Faire pivoter l'appareil.
   Résultat attendu : la liste reste affichée, aucun crash.
7. Appuyer sur « Vider les sujets » et confirmer.
   Résultat attendu : la liste se vide, compteur à 0, les deux forums restent
   présents.

## Travail attendu de Gemini

Créer le schéma complet en une seule fois, y compris les colonnes qui ne seront
utilisées qu'aux modules 04, 06 et 08. Ne pas écrire de migration. Ne pas créer de cas
d'usage. Ne pas toucher au thème ni à la navigation au delà de l'ajout de la
destination de debug.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 00 est validé. Lis `00_SPECIFICATIONS_GENERALES.md` puis
> `MODULE_01_PERSISTANCE_LOCALE.md` et implémente uniquement le module 01. Respecte
> strictement la liste des fichiers autorisés. Le schéma Room doit être complet dès
> maintenant, aucune migration ne sera écrite plus tard. Termine par le compte rendu
> demandé.
