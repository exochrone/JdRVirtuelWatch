# JdRVirtuelWatcher

Version : 1.0
Dernière mise à jour : 05/08/2026

## 1. Objectif de l'application

### 1.1 Description générale

JdRVirtuelWatcher est une application Android qui surveille deux sections du forum
phpBB `jdrvirtuel.com` consacrées aux propositions de parties de jeu de rôle, et qui
prévient l'utilisateur par notification lorsqu'une nouveauté apparaît.

L'application ne remplace pas le forum : elle sert de vigie. La lecture et la
participation se font toujours dans le navigateur.

### 1.2 Public visé

Un utilisateur unique, joueur de jeu de rôle, qui souhaite repérer rapidement les
nouvelles propositions de parties sans avoir à ouvrir le site et à parcourir les
listes de sujets plusieurs fois par jour.

### 1.3 Problème résolu

Les propositions de parties partent vite. Les repérer suppose aujourd'hui de
consulter manuellement deux pages web et de comparer mentalement avec ce qu'on avait
déjà vu. L'application automatise cette comparaison.

### 1.4 Forums surveillés

| Identifiant phpBB | Nom | URL |
|---|---|---|
| 15 | Oneshots | `https://www.jdrvirtuel.com/viewforum.php?f=15` |
| 16 | Campagnes | `https://www.jdrvirtuel.com/viewforum.php?f=16` |

Seule la première page de chaque forum est consultée. Le tri par défaut de phpBB
place les sujets les plus récemment actifs en tête, ce qui garantit qu'aucune
nouveauté n'est manquée.

### 1.5 Fonctionnalités majeures

1. Écran d'accueil listant les deux forums surveillés, avec compteur de sujets non
   lus, date de dernière synchronisation et indicateur d'erreur.
2. Écran de détail listant les sujets d'un forum : titre, auteur, date de création,
   nombre de réponses, auteur et date du dernier message.
3. Masquage individuel des sujets, réversible, avec option d'affichage temporaire des
   sujets masqués.
4. Mise sous surveillance individuelle des sujets, qui conditionne les notifications
   de nouvelles réponses.
5. Ouverture d'un sujet dans le navigateur.
6. Synchronisation automatique toutes les 15 minutes en arrière-plan.
7. Notification à la publication d'un nouveau sujet.
8. Notification à l'arrivée d'une réponse sur un sujet mis sous surveillance.
9. Gestion du dispositif anti-robot Cloudflare, y compris le cas où une validation
   humaine est requise.

### 1.6 Hors périmètre de cette version

L'application ne fait pas les choses suivantes, et Gemini ne doit en aucun cas les
implémenter, même partiellement :

- aucune authentification sur le forum, aucune publication de message ;
- aucune lecture du contenu des messages, seule la liste des sujets est traitée ;
- aucun ajout, retrait ou configuration de forums surveillés par l'utilisateur ;
- aucune pagination au delà de la première page d'un forum ;
- aucune synchronisation multi appareils, aucun compte utilisateur, aucun cloud ;
- aucune recherche, aucun filtre par mot clé ;
- aucune traduction, l'interface est exclusivement en français ;
- aucun widget d'écran d'accueil, aucune version Wear OS, aucune version tablette
  spécifique.

## 2. Consignes générales pour Gemini

Ces consignes s'appliquent à tous les modules, sans exception.

### 2.1 Pile technique imposée

- Kotlin uniquement, aucun fichier Java.
- Jetpack Compose uniquement, aucun layout XML, aucune Activity secondaire.
- Material 3.
- Architecture MVVM sur trois couches : `data`, `domain`, `feature`.
- Coroutines et `StateFlow`.
- Navigation Compose en mode type-safe, routes déclarées par des classes
  `@Serializable`.
- Room pour la persistance.
- DataStore Preferences pour les réglages.
- Hilt pour l'injection de dépendances.
- WorkManager pour la tâche périodique.
- Jsoup pour l'analyse du HTML.
- AndroidX Browser (Custom Tabs) pour l'ouverture des sujets.

### 2.2 Gestion des versions de dépendances

Le projet est créé par l'assistant « Empty Activity » d'Android Studio. Le fichier
`gradle/libs.versions.toml` généré fait autorité.

#### Toolchain réel du projet

Constaté et validé au module 00. Ces valeurs sont figées et ne doivent plus être
modifiées :

| Élément | Version |
|---|---|
| Android Gradle Plugin | 9.2.1 |
| Gradle | 9.4.1 |
| compileSdk | 37 |
| minSdk | 26 |
| Hilt | 2.60.1 |
| Package et applicationId | `com.jdrvirtuel.watcher` |

Deux particularités liées à AGP 9 sont déjà en place et ne doivent pas être défaites :

- `gradle.properties` contient `android.disallowKotlinSourceSets=false`, nécessaire à
  la compatibilité entre KSP et AGP 9.2.1. L'avertissement « experimental » émis à la
  configuration est attendu et sans conséquence.
- Hilt est en 2.60.1 et non en 2.51.1 : les versions antérieures échouent avec
  l'erreur « Android BaseExtension not found » sur AGP 9.

Les versions plancher indiquées dans les modules suivants sont des minimums
indicatifs. Si le catalogue contient déjà une version supérieure, elle est conservée.
Si une bibliothèque à ajouter s'avère incompatible avec AGP 9.2.1, Gemini retient la
première version compatible et le signale dans son compte rendu.

Règles impératives :

- Gemini n'ajoute au catalogue que les bibliothèques listées dans le module 00.
- Gemini ne modifie jamais une version déjà présente dans le catalogue.
- Gemini ne modifie jamais les versions d'AGP, de Kotlin, de KSP ou de Gradle.
- Gemini n'ajoute aucune bibliothèque qui ne soit pas explicitement listée dans le
  module en cours.
- En cas de conflit de version à la compilation, Gemini signale le problème à
  l'utilisateur et propose une correction, sans l'appliquer d'autorité.

### 2.3 Contraintes de production

- Produire uniquement du code qui compile.
- Ne créer et ne modifier que les fichiers listés dans la section « Fichiers
  autorisés » du module en cours. Tout autre fichier est interdit d'accès.
- Ne pas refactoriser le code existant, même s'il paraît perfectible.
- Ne pas renommer de package, de classe ou de fichier créé par un module antérieur.
- Ne pas anticiper sur un module ultérieur, même si la structure semble l'appeler.
- Ne pas écrire de test qui n'est pas explicitement demandé.
- Privilégier la lisibilité, éviter les abstractions et les optimisations
  prématurées.
- Demander des précisions plutôt que d'inventer une règle métier absente.

### 2.4 Conventions de code

- Aucune chaîne de caractères en dur dans l'interface, tout passe par
  `res/values/strings.xml`.
- Aucune couleur en dur, tout passe par le thème Material 3.
- Aucune dimension en dur, les espacements viennent de l'objet `Dimens` défini au
  module 00.
- Collecte des flux dans Compose exclusivement via `collectAsStateWithLifecycle()`.
- Les événements ponctuels (navigation, message temporaire, ouverture de navigateur)
  passent par un `Channel` exposé en `Flow`, jamais par le `UiState`.
- Aucun usage de `GlobalScope`, aucun `runBlocking` en dehors des tests.
- Les composables sont sans état quand c'est possible, le `ViewModel` porte l'état.
- Nommage : `XxxScreen`, `XxxViewModel`, `XxxUiState`, `XxxEvent` pour les actions de
  l'utilisateur, `XxxEffect` pour les événements ponctuels.
- Les suspend functions d'accès aux données s'exécutent sur `Dispatchers.IO`, sauf le
  module 02 dont la contrainte est expliquée sur place.

### 2.5 Compte rendu attendu à la fin de chaque module

Gemini termine systématiquement par un résumé structuré :

- liste des fichiers créés, avec leur chemin complet ;
- liste des fichiers modifiés, avec la nature de la modification ;
- liste des points laissés en suspens ou des ambiguïtés rencontrées ;
- commande exacte à lancer pour vérifier le module.

## 3. Architecture globale

### 3.1 Découpage en couches

L'application suit une architecture en trois couches, avec une dépendance
unidirectionnelle : `feature` dépend de `domain`, `data` dépend de `domain`, et
`domain` ne dépend de rien.

- **domain** : modèles métier purs (aucune annotation Room, aucun type Android),
  interfaces de dépôt, cas d'usage.
- **data** : implémentations des dépôts, base Room, accès réseau, analyse HTML,
  conversion entre entités et modèles.
- **feature** : écrans Compose et ViewModels, un sous package par écran.

### 3.2 Arborescence des packages

```
com.jdrvirtuel.watcher
├── JdrVirtuelWatcherApp.kt
├── MainActivity.kt
├── core
│   ├── di            objets Hilt transverses
│   ├── ui
│   │   ├── theme     Color, Theme, Type, Dimens
│   │   └── component composables réutilisables
│   └── util          extensions, formatage de dates
├── data
│   ├── local
│   │   ├── db        AppDatabase
│   │   ├── dao       ForumDao, TopicDao
│   │   ├── entity    ForumEntity, TopicEntity
│   │   └── prefs     AppPreferences
│   ├── remote        ForumPageSource et son implémentation WebView
│   ├── parser        TopicListParser
│   ├── mapper        conversions entity vers domain
│   └── repository    implémentations
├── domain
│   ├── model         Forum, Topic, SyncResult
│   ├── repository    interfaces
│   └── usecase       cas d'usage
├── feature
│   ├── home
│   ├── forumdetail
│   ├── verification
│   ├── settings
│   └── debug
├── notification
├── work
└── navigation
```

### 3.3 Organisation des ViewModels

Chaque écran possède un `ViewModel` annoté `@HiltViewModel` qui expose :

- un `StateFlow<XxxUiState>` construit par `stateIn` avec
  `SharingStarted.WhileSubscribed(5_000)` ;
- une fonction unique `onEvent(event: XxxEvent)` qui traite toutes les actions de
  l'utilisateur ;
- un `Flow<XxxEffect>` issu d'un `Channel(Channel.BUFFERED)` pour les événements
  ponctuels.

Le `UiState` est une `data class` qui contient toujours au minimum `isLoading:
Boolean` et `errorMessage: String?`.

### 3.4 Organisation des dépôts

Les dépôts sont déclarés par une interface dans `domain.repository` et implémentés
dans `data.repository`. Le lien est établi par un module Hilt `@Binds`.

Les fonctions de lecture retournent un `Flow`, les fonctions d'écriture sont
`suspend`.

### 3.5 Gestion des erreurs

Aucune exception ne remonte jusqu'à l'interface. Les opérations susceptibles
d'échouer retournent un type scellé explicite, défini au module concerné, par exemple
`FetchResult` pour l'accès réseau et `SyncResult` pour la synchronisation.

Le `ViewModel` traduit ces résultats en message affichable, issu de `strings.xml`.

### 3.6 Gestion de l'état des écrans

Un écran connaît quatre états : chargement, contenu, contenu vide, erreur. Chacun
possède un rendu distinct et explicite. Un écran ne reste jamais vide sans message.

## 4. Modèle de données

Le schéma est défini intégralement au module 01 et ne change plus ensuite. La base
reste en version 1 pour toute la version 1.0 de l'application, aucune migration Room
n'est à écrire.

### 4.1 Entité Forum

| Champ | Type | Rôle |
|---|---|---|
| `id` | Int, clé primaire | Identifiant phpBB du forum, 15 ou 16 |
| `name` | String | Nom affiché, « Oneshots » ou « Campagnes » |
| `url` | String | URL complète de la première page |
| `lastSyncAt` | Long? | Horodatage de la dernière synchronisation réussie |
| `lastSyncSuccess` | Boolean | Résultat de la dernière tentative |
| `lastSyncError` | String? | Libellé technique de la dernière erreur |
| `isBootstrapped` | Boolean | Vrai dès qu'une première synchronisation a réussi |

Le champ `isBootstrapped` sert à supprimer les notifications lors de l'amorçage
initial de la base.

### 4.2 Entité Topic

| Champ | Type | Rôle |
|---|---|---|
| `id` | Int, clé primaire | Identifiant phpBB du sujet, paramètre `t` de l'URL |
| `forumId` | Int, clé étrangère | Forum d'appartenance, suppression en cascade |
| `title` | String | Titre du sujet |
| `url` | String | URL complète du sujet |
| `author` | String | Auteur du sujet |
| `createdAt` | Long | Date de création, en millisecondes epoch |
| `replyCount` | Int | Nombre de réponses |
| `lastPostAuthor` | String | Auteur du dernier message |
| `lastPostAt` | Long | Date du dernier message, en millisecondes epoch |
| `isFull` | Boolean | Sujet portant l'icône « Complet » |
| `isHidden` | Boolean | Masqué par l'utilisateur, faux par défaut |
| `isWatched` | Boolean | Sous surveillance, faux par défaut |
| `isRead` | Boolean | Faux tant que l'utilisateur n'a pas ouvert le sujet |
| `firstSeenAt` | Long | Première apparition dans la base |
| `lastSeenAt` | Long | Dernière apparition sur la page du forum |

Un index est posé sur `forumId`.

### 4.3 Invariants

- `isHidden` à vrai force `isWatched` à faux.
- `isHidden`, `isWatched` et `isRead` ne sont jamais écrasés par une
  synchronisation.
- Un sujet dont `lastSeenAt` remonte à plus de 30 jours est supprimé, sauf s'il est
  sous surveillance.
- Les sujets épinglés du forum ne sont jamais insérés en base.

### 4.4 Préférences

Stockées en DataStore Preferences, dans `AppPreferences` :

| Clé | Type | Rôle |
|---|---|---|
| `consecutive_challenge_failures` | Int | Compteur d'échecs Cloudflare consécutifs |
| `last_challenge_prompt_at` | Long | Dernière alerte de vérification envoyée |

## 5. Charte UI

### 5.1 Palette

Material 3 avec couleurs dynamiques activées à partir d'Android 12. En dessous, ou si
les couleurs dynamiques sont indisponibles, un thème est généré à partir de la
couleur d'amorce `#8C3B3B`. Les thèmes clair et sombre sont tous deux pris en charge
et suivent le réglage du système.

### 5.2 Typographie

Typographie Material 3 par défaut, sans police additionnelle.

Usages imposés :

- titre de sujet : `titleMedium`, en `FontWeight.Bold` si le sujet n'est pas lu ;
- métadonnées d'un sujet : `bodySmall`, en `onSurfaceVariant` ;
- nom d'un forum : `titleLarge`.

### 5.3 Espacements

Un objet `Dimens` centralise les valeurs, définies au module 00 :

```
xs = 4.dp, sm = 8.dp, md = 16.dp, lg = 24.dp, xl = 32.dp
```

Aucune valeur `dp` littérale n'est écrite ailleurs, à l'exception des tailles
d'icônes et des épaisseurs de trait.

### 5.4 Composants

- `Scaffold` avec `TopAppBar` sur chaque écran.
- `LazyColumn` pour les listes, avec `key` sur l'identifiant de l'élément.
- `ListItem` Material 3 pour les lignes de forum.
- `Card` pour les lignes de sujet.
- `SnackbarHost` pour les messages temporaires.
- `IconButton` pour les actions par ligne.
- `PullToRefreshBox` pour le rafraîchissement manuel.

### 5.5 Grisage

Un élément grisé est rendu en enveloppant son contenu dans un
`CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant)`
avec un `alpha` de `0.5f` appliqué au conteneur. Ce comportement est factorisé dans
un composable `DimmedContent` défini au module 06.

### 5.6 Icônes

Bibliothèque `androidx.compose.material.icons`, jeu `Outlined` et `Filled`.

| Usage | Icône |
|---|---|
| Sujet visible | `Icons.Outlined.Visibility` |
| Sujet masqué | `Icons.Outlined.VisibilityOff` |
| Sujet non surveillé | `Icons.Outlined.NotificationsOff` |
| Sujet surveillé | `Icons.Filled.Notifications` |
| Masquer les sujets complets | `Icons.Outlined.PlaylistRemove` |
| Menu de débordement | `Icons.Outlined.MoreVert` |
| Rafraîchir | `Icons.Outlined.Refresh` |
| Réglages | `Icons.Outlined.Settings` |
| Alerte | `Icons.Outlined.Warning` |

Sur une carte de sujet, les icônes décrivent l'**état** et jamais l'action. Deux
icônes barrées côte à côte ne doivent pas signifier des choses opposées. Seul le
bouton de bascule « Afficher les sujets masqués » de la barre supérieure fait
exception et suit une logique d'action.

### 5.7 Animations

Aucune animation personnalisée. Les transitions par défaut de Navigation Compose et
les animations implicites de Material 3 suffisent.

## 6. Découpage en modules

### 6.1 Principe

Chaque module est autonome et se valide indépendamment. Un module ne démarre jamais
avant que ses prérequis ne soient validés par l'utilisateur.

L'utilisateur signale explicitement à Gemini le passage d'un module au suivant.

### 6.2 Liste et ordre imposé

| # | Module | Prérequis |
|---|---|---|
| 00 | Fondations | aucun |
| 01 | Persistance locale | 00 |
| 02 | Accès au forum | 00 |
| 03 | Analyse du HTML | 00 |
| 04 | Synchronisation | 01, 02, 03 |
| 05 | Écran d'accueil | 01, 04 |
| 06 | Écran de détail d'un forum | 01, 04 |
| 07 | Rafraîchissement automatique | 04 |
| 08 | Notifications | 04, 07 |
| 09 | Vérification Cloudflare | 02, 08 |
| 10 | Finitions | tous |
| 11 | Diagnostic au démarrage | 10 |

### 6.3 Écran de debug

Les modules 01 à 04 ne produisent aucune interface destinée à l'utilisateur final.
Pour rester vérifiables, ils alimentent un écran de debug construit de façon
cumulative, accessible depuis un bouton de la barre supérieure de l'écran d'accueil.

Cet écran est un outil de développement. Le module 10 le place derrière
`BuildConfig.DEBUG` afin qu'il disparaisse d'une version de production.

## 7. Fichier de test fourni

Le fichier `app/src/test/resources/viewforum_f15.html` est fourni par l'utilisateur.
Il s'agit d'une sauvegarde réelle de la première page du forum 15.

Gemini ne doit ni le créer, ni le modifier, ni le remplacer par un contenu inventé.
S'il est absent, Gemini interrompt le module 03 et le signale.

## 8. Contrainte réseau structurante

Le domaine `jdrvirtuel.com` est intégralement protégé par un challenge Cloudflare.
Un client HTTP classique reçoit une réponse `403` avec l'en tête
`Cf-Mitigated: challenge`, quels que soient les en têtes envoyés. Le flux ATOM de
phpBB est protégé de la même manière.

En conséquence, toute récupération de contenu passe obligatoirement par une WebView,
qui exécute le JavaScript du challenge et obtient le cookie `cf_clearance`. Aucune
tentative avec OkHttp, Retrofit, `HttpURLConnection` ou `Jsoup.connect()` ne doit
être écrite.

Jsoup est utilisé uniquement en mode hors ligne, via `Jsoup.parse(html)`.

## 9. Glossaire

| Terme | Signification |
|---|---|
| Forum | Une des deux sections surveillées, identifiée par son paramètre `f` |
| Sujet | Une proposition de partie, identifiée par son paramètre `t` |
| Sujet épinglé | Sujet annoncé en tête de liste par le forum, jamais stocké |
| Sujet complet | Sujet portant l'icône `Complet.png`, recrutement terminé |
| Sujet masqué | Sujet que l'utilisateur a choisi de ne plus voir |
| Sujet surveillé | Sujet dont les nouvelles réponses déclenchent une notification |
| Amorçage | Première synchronisation réussie d'un forum, silencieuse |
| Challenge | Page de vérification anti robot renvoyée par Cloudflare |

## 10. Definition of Done globale

L'application est terminée lorsque l'ensemble des conditions suivantes est réuni :

1. `gradlew assembleDebug` réussit sans erreur.
2. `gradlew lintDebug` ne remonte aucune erreur bloquante.
3. `gradlew testDebugUnitTest` réussit.
4. L'application se lance sur un appareil réel sous Android 8.0 ou supérieur.
5. Une synchronisation manuelle depuis l'écran d'accueil remplit les deux forums.
6. Le masquage, la surveillance et l'ouverture d'un sujet fonctionnent et survivent à
   un redémarrage de l'application.
7. Une notification de nouveau sujet est reçue après suppression manuelle d'un sujet
   depuis l'écran de debug, suivie d'une synchronisation.
8. Le bandeau de vérification Cloudflare apparaît après invalidation du cookie depuis
   l'écran de debug, et la validation manuelle rétablit la synchronisation.
9. Le bouton d'accès à l'écran de debug est absent d'une compilation `release`.
