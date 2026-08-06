# MODULE 05 : Écran d'accueil

Statut : à faire
Prérequis : modules 01 et 04 validés

## Objectif

Remplacer l'écran d'accueil provisoire du module 00 par l'écran réel : la liste des
deux forums surveillés, avec leur état de synchronisation et leur compteur de sujets
non lus.

Ce module ne fait pas : aucune liste de sujets, aucune notification, aucune tâche de
fond.

## Fonctionnalités

- Afficher les deux forums surveillés.
- Afficher pour chacun le nombre de sujets non lus et la date de dernière
  synchronisation réussie.
- Signaler visuellement un forum dont la dernière synchronisation a échoué.
- Lancer une synchronisation manuelle des deux forums.
- Naviguer vers l'écran de détail d'un forum.
- Accéder à l'écran de debug.

## Écrans

### HomeScreen

- **Nom** : `HomeScreen`
- **Objectif** : point d'entrée de l'application, vue d'ensemble des forums.
- **Contenu** :
  - `TopAppBar` portant le nom de l'application, un bouton de rafraîchissement et un
    bouton d'accès au debug ;
  - une `LazyColumn` de deux `Card`, une par forum, contenant :
    - le nom du forum en `titleLarge` ;
    - le nombre total de sujets stockés pour ce forum, en `bodyMedium`, sous la forme
      « 24 sujets » ou « 1 sujet » ;
    - une ligne d'état en `bodySmall` : « Mis à jour il y a X » ou « Jamais
      synchronisé » ;
    - un `Badge` avec le nombre de sujets non lus, masqué si ce nombre vaut zéro ;
    - une icône d'alerte si la dernière tentative a échoué, accompagnée du libellé
      « Dernière synchronisation en échec » ;
  - un `PullToRefreshBox` enveloppant la liste ;
  - un `SnackbarHost`.
- **Actions possibles** :
  - appui sur une carte : navigation vers le détail du forum ;
  - appui sur le bouton de rafraîchissement, ou geste de tirage vers le bas :
    synchronisation des deux forums ;
  - appui sur le bouton de debug : navigation vers l'écran de debug.
- **Navigation** : destination de départ. Le bouton retour ferme l'application.

## Comportement

- L'écran observe la base en continu. Une synchronisation déclenchée ailleurs, par
  exemple par la tâche de fond du module 07, met à jour l'affichage sans action de
  l'utilisateur.
- Pendant une synchronisation, l'indicateur du `PullToRefreshBox` est visible et le
  bouton de rafraîchissement est désactivé.
- À l'issue d'une synchronisation manuelle, un message temporaire résume le résultat :
  - succès sans nouveauté : « Aucune nouveauté » ;
  - succès avec nouveautés : « X nouveaux sujets » au singulier ou au pluriel ;
  - échec : « Échec de la synchronisation » ;
  - vérification requise : « Vérification requise pour accéder au forum ». Le
    module 09 rendra ce message actionnable.
- Aucune synchronisation n'est déclenchée automatiquement à l'ouverture de l'écran.
  L'utilisateur garde la main, et la tâche de fond s'en charge par ailleurs.

## Règles métier

- Le nombre total de sujets compte tous les sujets du forum présents en base, y
  compris les sujets masqués et les sujets complets. C'est un indicateur de volume,
  pas de visibilité.
- Le nombre de sujets non lus compte les sujets du forum dont `isRead` est faux et
  `isHidden` est faux. Les sujets masqués ne comptent jamais.
- La date affichée est celle du dernier succès, jamais celle de la dernière tentative.
- Le libellé de date est relatif en dessous de 24 heures (« il y a 12 minutes »,
  « il y a 3 heures ») et absolu au delà (« le 3 août à 14:52 »). Le formatage est
  centralisé dans `core.util.DateFormatter`.

## Modèle de données

Aucune modification du schéma. L'écran consomme `ForumRepository.observeForums()`,
`TopicRepository.observeUnreadCount(forumId)` et
`TopicRepository.observeTopicCount(forumId)`.

Si `observeTopicCount` n'existe pas dans le DAO issu du module 01, c'est la seule
requête que Gemini est autorisé à ajouter dans `TopicDao` et `TopicRepository` pour ce
module. Il doit le mentionner explicitement dans son compte rendu.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `HomeScreen` | `feature.home` | Composable |
| `HomeViewModel` | `feature.home` | État et actions |
| `HomeUiState` | `feature.home` | État |
| `HomeEvent` | `feature.home` | Actions de l'utilisateur |
| `HomeEffect` | `feature.home` | Événements ponctuels |
| `ForumCard` | `feature.home` | Composable de ligne |
| `DateFormatter` | `core.util` | Formatage relatif et absolu |

### État

```kotlin
data class HomeUiState(
    val forums: List<ForumUiModel> = emptyList(),
    val isSyncing: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class ForumUiModel(
    val id: Int,
    val name: String,
    val topicCount: Int,
    val unreadCount: Int,
    val lastSyncAt: Long?,
    val hasSyncError: Boolean
)
```

### Événements

```kotlin
sealed interface HomeEvent {
    data class OnForumClick(val forumId: Int) : HomeEvent
    data object OnRefresh : HomeEvent
    data object OnDebugClick : HomeEvent
}

sealed interface HomeEffect {
    data class NavigateToForum(val forumId: Int) : HomeEffect
    data object NavigateToDebug : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
}
```

Le `StateFlow` est construit en combinant le flux des forums et les flux de compteurs
non lus, via `combine`, puis `stateIn(viewModelScope,
SharingStarted.WhileSubscribed(5_000), HomeUiState())`.

### Navigation

Ajout de la destination :

```kotlin
@Serializable
data class ForumDetailRoute(val forumId: Int)
```

L'écran de détail est créé au module 06. Pour ce module, la destination pointe vers un
composable temporaire affichant l'identifiant reçu, qui sera remplacé au module 06.

## Composants UI

`Scaffold`, `TopAppBar`, `IconButton`, `PullToRefreshBox`, `LazyColumn`, `Card`,
`Badge`, `Icon`, `SnackbarHost`, `CircularProgressIndicator`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Base vide | Ne peut pas se produire, les forums sont amorcés au module 01. Si la liste est vide malgré tout, afficher un message d'erreur explicite |
| Aucune synchronisation encore effectuée | « Jamais synchronisé », « 0 sujet », pas de badge |
| Compteur de sujets à zéro | Libellé « 0 sujet » au singulier |
| Un seul sujet | Libellé « 1 sujet » au singulier |
| Compteur non lus à zéro | Aucun badge affiché |
| Compteur supérieur à 99 | Badge affichant « 99+ » |
| Rotation pendant une synchronisation | La synchronisation continue, l'état est restauré |
| Retour Android | L'application se ferme |
| Nom de forum très long | Une seule ligne, tronquée par des points de suspension |

## Fichiers autorisés

```
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeContract.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/ForumCard.kt
app/src/main/java/com/jdrvirtuel/watcher/core/util/DateFormatter.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/Routes.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/AppNavHost.kt
app/src/main/res/values/strings.xml
```

Aucune dépendance nouvelle. Aucune modification des couches `data` et `domain`.

## Contrat exposé aux modules suivants

- `HomeScreen` et sa destination, point d'ancrage du bandeau d'alerte du module 09.
- `ForumDetailRoute`, destination que le module 06 remplira.
- `DateFormatter`, réutilisé par le module 06.

## Tests

Aucun test automatisé.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
```

### Scénario manuel

1. Désinstaller puis réinstaller l'application, la lancer.
   Résultat attendu : deux cartes, « Oneshots » et « Campagnes », toutes deux
   marquées « Jamais synchronisé », affichant « 0 sujet », sans badge. Aucun
   identifiant numérique de forum n'apparaît.
2. Tirer la liste vers le bas.
   Résultat attendu : l'indicateur de chargement apparaît, puis les deux cartes
   affichent « Mis à jour il y a quelques secondes », le nombre total de sujets
   récupérés, et un badge indiquant le nombre de sujets non lus. À ce stade les deux
   nombres sont identiques, puisque aucun sujet n'a encore été ouvert.
3. Attendre quelques minutes et rouvrir l'application.
   Résultat attendu : le libellé de date a évolué, par exemple « il y a 5 minutes ».
4. Appuyer sur la carte « Oneshots ».
   Résultat attendu : navigation vers l'écran provisoire affichant l'identifiant 15.
5. Revenir en arrière.
   Résultat attendu : retour à l'accueil, état conservé.
6. Activer le mode avion, appuyer sur le bouton de rafraîchissement.
   Résultat attendu : message « Échec de la synchronisation », icône d'alerte sur les
   deux cartes, dates de dernier succès conservées, badges inchangés.
7. Désactiver le mode avion, rafraîchir.
   Résultat attendu : les icônes d'alerte disparaissent.
8. Ouvrir l'écran de debug, marquer tous les sujets d'un forum comme lus, revenir à
   l'accueil.
   Résultat attendu : le badge de ce forum disparaît sans rafraîchissement manuel.
9. Faire pivoter l'appareil.
   Résultat attendu : aucun crash, aucune perte d'état.

## Travail attendu de Gemini

Remplacer intégralement l'écran d'accueil provisoire. Ne créer aucune classe dans les
couches `data` et `domain` : tout ce qui est nécessaire existe déjà. Si une requête
manque, le signaler dans le compte rendu plutôt que de la créer.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 04 est validé. Lis `00_SPECIFICATIONS_GENERALES.md` puis
> `MODULE_05_ECRAN_ACCUEIL.md` et implémente uniquement le module 05. L'écran
> d'accueil provisoire du module 00 est remplacé. N'ajoute rien aux couches data et
> domain. Respecte la liste des fichiers autorisés et termine par le compte rendu
> demandé.
