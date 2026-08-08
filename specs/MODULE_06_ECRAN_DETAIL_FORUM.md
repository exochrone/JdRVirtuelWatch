# MODULE 06 : Écran de détail d'un forum

Statut : à faire
Prérequis : modules 01 et 04 validés, module 05 recommandé

## Objectif

Afficher la liste des sujets d'un forum et permettre à l'utilisateur d'agir sur
chacun : l'ouvrir dans le navigateur, le masquer, le mettre sous surveillance.

C'est l'écran principal de l'application.

Ce module ne fait pas : aucune notification, aucune tâche de fond.

## Fonctionnalités

- Afficher les sujets d'un forum avec toutes leurs métadonnées.
- Ouvrir un sujet dans le navigateur.
- Masquer ou réafficher un sujet.
- Mettre un sujet sous surveillance ou l'en retirer.
- Afficher temporairement les sujets masqués.
- Rafraîchir la liste.

## Écrans

### ForumDetailScreen

- **Nom** : `ForumDetailScreen`
- **Objectif** : consulter et trier les propositions de parties d'un forum.
- **Contenu** :
  - `TopAppBar` portant le nom du forum, son sous-titre de comptage, une flèche de
    retour, un bouton « Afficher les sujets masqués » à bascule, et un menu de
    débordement à trois points verticaux ;
  - une `LazyColumn` de `TopicCard` ;
  - un `PullToRefreshBox` ;
  - un `SnackbarHost`.
- **Actions possibles** : détaillées ci dessous.
- **Navigation** : atteinte depuis l'accueil, retour par la flèche ou le bouton
  système.

### TopicCard

Chaque carte contient :

- le titre du sujet, en `titleMedium`, sur deux lignes maximum, en gras si le sujet
  n'est pas lu ;
- une pastille « Complet » à droite du titre si `isFull` est vrai ;
- une ligne de métadonnées en `bodySmall` : « par {auteur} le {date de création} » ;
- une seconde ligne : « {nombre} réponses, dernier message de {auteur} {date} » ;
- deux `IconButton` alignés à droite : surveillance puis masquage.

### Logique des icônes de la carte

Les deux icônes décrivent l'**état** du sujet, jamais l'action que l'appui
déclenchera. C'est la convention Material, et elle évite que deux icônes barrées
côte à côte ne signifient des choses opposées.

| État du sujet | Oeil | Cloche |
|---|---|---|
| Visible, non surveillé | `Icons.Outlined.Visibility` | `Icons.Outlined.NotificationsOff` |
| Visible, surveillé | `Icons.Outlined.Visibility` | `Icons.Filled.Notifications` |
| Masqué | `Icons.Outlined.VisibilityOff` | `Icons.Outlined.NotificationsOff`, désactivée et grisée |

Les deux icônes restent affichées sur tous les sujets sans exception. Masquer la
cloche sur les sujets non surveillés rendrait la mise sous surveillance inaccessible,
faute de point d'appui visible.

Les descriptions de contenu accompagnant les icônes décrivent également l'état, et non
l'action.

### Menu de débordement

Ouvert par une icône `Icons.Outlined.MoreVert` placée à droite de l'oeil, il contient
deux entrées, dans cet ordre :

| Entrée | Icône |
|---|---|
| « Rafraîchir la liste » | `Icons.Outlined.Refresh` |
| « Masquer les COMPLET » | `Icons.Outlined.PlaylistRemove` |

Le geste de tirage vers le bas reste actif et continue de déclencher un
rafraîchissement, indépendamment de l'entrée de menu.

## Comportement

### Appui sur le titre

Ouvre le sujet dans le navigateur, via Chrome Custom Tabs. Si aucun navigateur ne
prend en charge les Custom Tabs, repli sur un `Intent.ACTION_VIEW`. Si aucun
navigateur n'est disponible, message temporaire « Aucun navigateur disponible ».

Au moment de l'ouverture, le sujet passe à `isRead` vrai. Le titre cesse donc d'être
en gras au retour dans l'application.

### Appui sur l'icône de masquage

- Sujet visible : il devient masqué, sa surveillance est automatiquement retirée, et
  un message temporaire « Sujet masqué » apparaît avec une action « Annuler » qui
  restaure les deux états précédents.
- Sujet masqué, visible parce que l'option d'affichage est active : il redevient
  visible.

Lorsque l'option d'affichage des masqués est inactive, masquer un sujet le fait
disparaître de la liste.

### Appui sur l'icône de surveillance

Bascule `isWatched`. Un message temporaire confirme : « Surveillance activée » ou
« Surveillance désactivée ».

Cette action est indisponible sur un sujet masqué : l'icône est alors désactivée et
grisée.

### Masquer les COMPLET

Entrée de menu qui masque en une fois tous les sujets du forum courant qui sont
marqués complets, non déjà masqués et **non sous surveillance**. Mettre un sujet sous
surveillance est un choix explicite de l'utilisateur, l'action en lot ne le défait
pas.

C'est un accélérateur, pas un filtre : l'action écrit `isHidden` exactement comme un
masquage individuel répété, sans créer d'état nouveau. Un sujet qui deviendra complet
plus tard ne sera donc pas masqué automatiquement.

Déroulement :

1. Si aucun sujet n'est concerné, afficher le message temporaire « Aucune partie
   complète à masquer » et ne rien faire. L'entrée de menu est par ailleurs désactivée
   et grisée dans ce cas.
2. Sinon, afficher un `AlertDialog` de confirmation indiquant le nombre de sujets
   concernés, accordé au pluriel par une ressource `plurals`.
3. Après confirmation, masquer les sujets et afficher un message temporaire indiquant
   le nombre traité, avec une action « Annuler ».
4. L'annulation rétablit **exactement** les sujets concernés par cette action, et eux
   seuls. La liste de leurs identifiants est donc mémorisée au moment de l'opération,
   faute de quoi l'annulation démasquerait aussi des sujets masqués auparavant.

### Bouton d'affichage des masqués

Bascule l'affichage des sujets masqués. Cet état n'est pas persistant : il revient à
« masqués cachés » à chaque ouverture de l'écran. Il est en revanche conservé lors
d'une rotation de l'appareil, puisqu'il est porté par le `ViewModel`.

L'icône de ce bouton suit une logique d'**action** et non d'état : oeil non barré par
défaut, c'est-à-dire lorsque les sujets masqués sont cachés, et oeil barré lorsqu'ils
sont affichés.

Ce choix est assumé et diffère de la logique retenue pour les icônes des cartes. Si le
rendu paraissait confus à l'usage, l'inversion tient en une ligne.

### Rafraîchissement

Le bouton et le geste de tirage déclenchent `SyncForumUseCase` sur le forum courant.
Le retour suit les mêmes messages que l'écran d'accueil.

## Règles métier

### Ordre d'affichage

1. Les sujets sous surveillance d'abord.
2. Puis les autres, par `lastPostAt` décroissant.
3. À l'intérieur du premier groupe, également par `lastPostAt` décroissant.

Les sujets complets et les sujets masqués ne sont pas déplacés : ils restent à leur
place dans cet ordre, seul leur rendu change.

### Visibilité

- Sujets masqués absents de la liste, sauf si l'option d'affichage est active.
- Aucun sujet épinglé n'est jamais présent, ils ne sont pas stockés.

### Grisage

Un sujet masqué n'est pas affiché, sauf lorsque l'option « Afficher les sujets
masqués » est active. Dans ce cas seulement, il réapparaît grisé et se retrouve côte à
côte avec les sujets complets, eux aussi grisés. Les trois combinaisons suivantes
doivent alors rester distinguables au premier coup d'oeil :

| Cas | Rendu |
|---|---|
| Sujet complet | Contenu grisé, pastille « Complet » visible |
| Sujet masqué, option d'affichage active | Contenu grisé, icône oeil barré en position « masqué » |
| Sujet complet et masqué, option active | Contenu grisé, pastille « Complet » et icône oeil barré |

Le grisage est produit par un composable `DimmedContent` défini dans
`core.ui.component`, qui applique un `alpha` de `0.5f` au conteneur.

### Lecture

- Un sujet nouveau est non lu.
- Un sujet ouvert dans le navigateur devient lu.
- Un sujet sous surveillance qui reçoit une réponse redevient non lu, règle appliquée
  par le module 04.
- Un sujet masqué n'entre jamais dans le compteur de non lus.

## Modèle de données

Aucune modification du schéma.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `ForumDetailScreen` | `feature.forumdetail` | Composable |
| `ForumDetailViewModel` | `feature.forumdetail` | État et actions |
| `ForumDetailUiState` | `feature.forumdetail` | État |
| `ForumDetailEvent` | `feature.forumdetail` | Actions |
| `ForumDetailEffect` | `feature.forumdetail` | Événements ponctuels |
| `TopicCard` | `feature.forumdetail` | Composable de ligne |
| `DimmedContent` | `core.ui.component` | Grisage |
| `BrowserLauncher` | `core.util` | Ouverture Custom Tabs et repli |

### État

```kotlin
data class ForumDetailUiState(
    val forumName: String = "",
    val topics: List<TopicUiModel> = emptyList(),
    val showHidden: Boolean = false,
    val isSyncing: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class TopicUiModel(
    val id: Int,
    val title: String,
    val url: String,
    val author: String,
    val createdAtLabel: String,
    val replyCount: Int,
    val lastPostAuthor: String,
    val lastPostAtLabel: String,
    val isFull: Boolean,
    val isHidden: Boolean,
    val isWatched: Boolean,
    val isRead: Boolean
)
```

Le formatage des dates est fait dans le `ViewModel` à l'aide de `DateFormatter`, pas
dans le composable.

### Événements

```kotlin
sealed interface ForumDetailEvent {
    data class OnTopicClick(val topic: TopicUiModel) : ForumDetailEvent
    data class OnToggleHidden(val topicId: Int) : ForumDetailEvent
    data class OnToggleWatched(val topicId: Int) : ForumDetailEvent
    data class OnUndoHide(val topicId: Int, val wasWatched: Boolean) : ForumDetailEvent
    data object OnToggleShowHidden : ForumDetailEvent
    data object OnHideAllFull : ForumDetailEvent
    data class OnUndoHideAllFull(val topicIds: List<Int>) : ForumDetailEvent
    data object OnRefresh : ForumDetailEvent
    data object OnBack : ForumDetailEvent
}

sealed interface ForumDetailEffect {
    data class OpenUrl(val url: String) : ForumDetailEffect
    data class ShowMessage(val message: String) : ForumDetailEffect
    data class ShowUndoHide(val topicId: Int, val wasWatched: Boolean) : ForumDetailEffect
    data class ShowUndoHideAllFull(val topicIds: List<Int>) : ForumDetailEffect
    data object NavigateBack : ForumDetailEffect
}
```

Le paramètre `forumId` est récupéré depuis `SavedStateHandle` via la route
`ForumDetailRoute`.

## Composants UI

`Scaffold`, `TopAppBar`, `IconButton`, `PullToRefreshBox`, `LazyColumn` avec `key`,
`Card`, `AssistChip`, `Icon`, `Text`, `SnackbarHost` avec action, `DropdownMenu` et
`DropdownMenuItem`, `AlertDialog`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Forum sans aucun sujet en base | Message « Aucun sujet, tirez pour rafraîchir » |
| Tous les sujets masqués | Message « Tous les sujets sont masqués », le bouton d'affichage reste actif |
| Liste très longue | `LazyColumn` avec `key` sur l'identifiant, défilement fluide |
| Titre très long | Deux lignes maximum, points de suspension |
| Titre avec emojis | Affiché intégralement |
| Rotation | Position de défilement et option d'affichage conservées |
| Retour Android | Retour à l'accueil |
| Masquage d'un sujet surveillé | La surveillance est retirée, l'annulation restaure les deux états |
| « Masquer les COMPLET » sans aucun sujet complet | Entrée de menu désactivée, message explicite si atteinte |
| « Masquer les COMPLET » avec un sujet complet surveillé | Ce sujet n'est pas masqué |
| Annulation après « Masquer les COMPLET » | Seuls les sujets de cette action sont rétablis |
| Aucun navigateur installé | Message d'erreur, aucun crash |
| Synchronisation en échec | Message temporaire, la liste reste affichée |
| Sujet supprimé par une purge pendant l'affichage | La liste se met à jour, aucun crash |

## Fichiers autorisés

```
gradle/libs.versions.toml                    ajout d'AndroidX Browser uniquement
app/build.gradle.kts                         ajout de la dépendance uniquement
app/src/main/java/com/jdrvirtuel/watcher/feature/forumdetail/ForumDetailScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/forumdetail/ForumDetailViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/forumdetail/ForumDetailContract.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/forumdetail/TopicCard.kt
app/src/main/java/com/jdrvirtuel/watcher/core/ui/component/DimmedContent.kt
app/src/main/java/com/jdrvirtuel/watcher/core/util/BrowserLauncher.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/AppNavHost.kt
app/src/main/res/values/strings.xml
```

Dépendance à ajouter : `androidx.browser:browser`, version minimale 1.8.0.

## Contrat exposé aux modules suivants

- `ForumDetailRoute`, cible des notifications du module 08 si un retour dans
  l'application est souhaité.
- `BrowserLauncher`, réutilisé par le module 08 pour ouvrir un sujet depuis une
  notification.
- `DimmedContent`, réutilisable.

## Tests

Aucun test automatisé.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
```

### Scénario manuel

1. Depuis l'accueil, synchroniser puis ouvrir « Oneshots ».
   Résultat attendu : la liste correspond à la page web, aucun sujet épinglé, les
   titres sont en gras.
2. Comparer avec le forum ouvert dans un navigateur.
   Résultat attendu : mêmes titres, mêmes auteurs, mêmes nombres de réponses, mêmes
   dates. Les sujets portant l'icône « Complet » sur le web sont grisés et portent la
   pastille dans l'application.
3. Appuyer sur le titre d'un sujet.
   Résultat attendu : le navigateur s'ouvre sur le bon sujet.
4. Revenir dans l'application.
   Résultat attendu : le titre de ce sujet n'est plus en gras.
5. Masquer un sujet.
   Résultat attendu : il disparaît, un message avec « Annuler » apparaît.
6. Appuyer sur « Annuler ».
   Résultat attendu : le sujet revient à sa place.
7. Mettre un sujet sous surveillance.
   Résultat attendu : l'icône passe en cloche pleine, le sujet remonte en tête de
   liste.
8. Masquer ce même sujet.
   Résultat attendu : il disparaît et sa surveillance est retirée.
9. Activer « Afficher les sujets masqués ».
   Résultat attendu : le sujet réapparaît, grisé, avec l'icône œil barré, et son
   icône de surveillance est désactivée. Il est distinguable d'un sujet complet.
10. Quitter l'écran et y revenir.
    Résultat attendu : l'option d'affichage est revenue à l'état inactif, le sujet est
    de nouveau invisible.
11. Faire pivoter l'appareil avec l'option active.
    Résultat attendu : l'option reste active, la position de défilement est conservée.
12. Fermer complètement l'application puis la relancer et rouvrir l'écran.
    Résultat attendu : les masquages et les surveillances sont conservés.
13. Tirer la liste vers le bas.
    Résultat attendu : synchronisation, liste actualisée, masquages et surveillances
    intacts.
14. Vérifier la logique des icônes : un sujet ordinaire affiche un oeil **ouvert** et
    une cloche **barrée**. Un sujet surveillé affiche une cloche pleine.
15. Ouvrir le menu à trois points.
    Résultat attendu : deux entrées, « Rafraîchir la liste » et « Masquer les
    COMPLET ».
16. Mettre un sujet complet sous surveillance, puis appuyer sur « Masquer les
    COMPLET ».
    Résultat attendu : la confirmation annonce un nombre excluant ce sujet. Après
    confirmation, tous les sujets complets disparaissent sauf celui sous surveillance.
17. Appuyer sur « Annuler » dans le message temporaire.
    Résultat attendu : les sujets masqués par cette action réapparaissent. Un sujet
    masqué manuellement avant l'opération reste masqué.
18. Relancer « Masquer les COMPLET » deux fois de suite.
    Résultat attendu : à la seconde tentative, l'entrée de menu est désactivée ou le
    message « Aucune partie complète à masquer » s'affiche.

## Travail attendu de Gemini

Créer uniquement les classes listées. Ne rien ajouter aux couches `data` et `domain`.
Le tri et le filtrage se font dans le `ViewModel`, pas dans une requête SQL, afin de
garder le DAO stable.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 05 est validé. Lis `00_SPECIFICATIONS_GENERALES.md` puis
> `MODULE_06_ECRAN_DETAIL_FORUM.md` et implémente uniquement le module 06. Attention à
> bien distinguer visuellement un sujet complet d'un sujet masqué, tous deux étant
> grisés. Respecte la liste des fichiers autorisés et termine par le compte rendu
> demandé.
