# MODULE 00 : Fondations

Statut : à faire
Prérequis : aucun

## Objectif

Mettre en place le squelette du projet : dépendances, injection de dépendances,
thème Material 3, navigation, et un écran d'accueil vide qui sert de point d'entrée.

Ce module ne produit aucune fonctionnalité visible pour l'utilisateur. Il garantit
que l'application se lance, affiche un écran au bon thème, et que l'ossature technique
est prête pour les modules suivants.

Ce module ne fait pas : aucune base de données, aucun accès réseau, aucun contenu
métier.

## Point de départ

L'utilisateur crée au préalable un projet Android Studio neuf :

- modèle « Empty Activity » avec Compose ;
- nom de l'application : `JdRVirtuelWatcher` ;
- nom du package : `com.jdrvirtuel.watcher` ;
- langage Kotlin, catalogue de versions Gradle activé ;
- minSdk 26, targetSdk et compileSdk laissés à la valeur proposée par l'assistant.

Le fichier `gradle/libs.versions.toml` généré fait autorité. Gemini ne modifie aucune
version existante.

## Fonctionnalités

- L'application se lance et affiche un écran d'accueil vide portant son titre.
- Le thème suit le réglage clair ou sombre du système.
- Les couleurs dynamiques sont utilisées à partir d'Android 12.
- La navigation est en place, prête à accueillir de nouvelles destinations.
- Hilt est fonctionnel de bout en bout.

## Dépendances à ajouter au catalogue

Gemini ajoute uniquement ces entrées dans `gradle/libs.versions.toml`, puis les
référence dans `app/build.gradle.kts`. Aucune autre bibliothèque n'est autorisée.

| Bibliothèque | Artefact | Version minimale |
|---|---|---|
| Hilt | `com.google.dagger:hilt-android` | 2.51.1 |
| Hilt compilateur | `com.google.dagger:hilt-android-compiler` | 2.51.1 |
| Hilt Navigation Compose | `androidx.hilt:hilt-navigation-compose` | 1.2.0 |
| Navigation Compose | `androidx.navigation:navigation-compose` | 2.8.0 |
| Sérialisation Kotlin | `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.7.0 |
| Lifecycle runtime Compose | `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.0 |
| Lifecycle ViewModel Compose | `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.0 |
| Material Icons Extended | `androidx.compose.material:material-icons-extended` | via BOM Compose |

Greffons à activer : `com.google.devtools.ksp`, `com.google.dagger.hilt.android`,
`org.jetbrains.kotlin.plugin.serialization`.

Si l'assistant Android Studio a généré une version différente d'un de ces artefacts,
Gemini conserve la version générée et le signale dans son compte rendu.

## Écrans

### Écran d'accueil, version provisoire

- **Nom** : `HomeScreen`
- **Objectif** : servir de destination de départ et prouver que le socle fonctionne.
- **Contenu** : un `Scaffold`, une `TopAppBar` affichant le nom de l'application, et
  un texte centré indiquant que l'application est en cours de construction.
- **Actions possibles** : aucune.
- **Navigation** : destination de départ du graphe.

Cet écran sera intégralement remplacé au module 05. Il ne doit contenir aucune
logique.

## Comportement

Au lancement, `MainActivity` installe le thème puis le graphe de navigation, dont la
destination de départ est `HomeRoute`. Le bouton retour du système ferme
l'application, puisqu'il n'existe qu'une seule destination.

La rotation de l'appareil ne provoque ni crash ni perte d'état.

## Règles métier

Aucune à ce stade.

## Architecture

### Classes à créer

| Classe | Package | Rôle |
|---|---|---|
| `JdrVirtuelWatcherApp` | racine | Classe `Application` annotée `@HiltAndroidApp` |
| `MainActivity` | racine | Activité unique, annotée `@AndroidEntryPoint` |
| `AppNavHost` | `navigation` | Graphe de navigation |
| `Routes` | `navigation` | Destinations `@Serializable` |
| `Color.kt` | `core.ui.theme` | Couleur d'amorce et schémas de secours |
| `Theme.kt` | `core.ui.theme` | `JdrVirtuelWatcherTheme` |
| `Type.kt` | `core.ui.theme` | Typographie Material 3 par défaut |
| `Dimens.kt` | `core.ui.theme` | Objet des espacements |
| `HomeScreen` | `feature.home` | Écran provisoire |

### Destinations de navigation

Seule `HomeRoute` est déclarée à ce stade :

```kotlin
@Serializable
data object HomeRoute
```

Les destinations suivantes seront ajoutées par leurs modules respectifs.

### Thème

`JdrVirtuelWatcherTheme` accepte un paramètre `darkTheme: Boolean =
isSystemInDarkTheme()` et un paramètre `dynamicColor: Boolean = true`. Sur Android 12
et supérieur, il utilise `dynamicLightColorScheme` ou `dynamicDarkColorScheme`. Sinon,
il applique les schémas de secours générés à partir de la couleur d'amorce `#8C3B3B`.

### Espacements

```kotlin
object Dimens {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
```

## Composants UI

`Scaffold`, `TopAppBar`, `Text`, `Box`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Rotation de l'écran | Aucun crash, l'écran se redessine |
| Appareil sous Android 8 | Le thème de secours s'applique, pas de couleurs dynamiques |
| Thème sombre du système | L'application passe en sombre sans redémarrage |
| Retour Android | L'application se ferme |

## Fichiers autorisés

Gemini peut créer ou modifier uniquement :

```
gradle/libs.versions.toml
build.gradle.kts
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml
app/src/main/java/com/jdrvirtuel/watcher/JdrVirtuelWatcherApp.kt
app/src/main/java/com/jdrvirtuel/watcher/MainActivity.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/AppNavHost.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/Routes.kt
app/src/main/java/com/jdrvirtuel/watcher/core/ui/theme/Color.kt
app/src/main/java/com/jdrvirtuel/watcher/core/ui/theme/Theme.kt
app/src/main/java/com/jdrvirtuel/watcher/core/ui/theme/Type.kt
app/src/main/java/com/jdrvirtuel/watcher/core/ui/theme/Dimens.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeScreen.kt
```

Le manifeste est modifié pour déclarer la classe `Application` et la permission
`INTERNET`. Aucune autre permission n'est ajoutée à ce stade.

## Contrat exposé aux modules suivants

- `JdrVirtuelWatcherTheme` : thème à appliquer à tout composable.
- `Dimens` : espacements.
- `AppNavHost` et `Routes` : point d'ajout des nouvelles destinations.
- Hilt opérationnel, avec `@HiltAndroidApp` et `@AndroidEntryPoint` en place.

## Tests

Aucun test automatisé pour ce module.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
```

La commande se termine par `BUILD SUCCESSFUL`.

### Scénario manuel

1. Lancer l'application sur un appareil ou un émulateur.
   Résultat attendu : l'écran s'affiche avec le titre « JdRVirtuelWatcher » dans la
   barre supérieure et un texte au centre.
2. Faire pivoter l'appareil.
   Résultat attendu : aucun crash, l'écran se redessine à l'identique.
3. Basculer le système en thème sombre depuis les paramètres Android.
   Résultat attendu : l'application repasse en sombre, textes lisibles.
4. Appuyer sur le bouton retour.
   Résultat attendu : l'application se ferme sans erreur.

## Travail attendu de Gemini

Créer uniquement les classes listées. Ne pas créer de dépôt, d'entité, de ViewModel
ni de cas d'usage : ils appartiennent aux modules suivants.

Terminer par le compte rendu structuré décrit dans les spécifications générales.

## Prompt de démarrage

> Tu travailles sur le projet JdRVirtuelWatcher. Lis les spécifications générales
> (`00_SPECIFICATIONS_GENERALES.md`) puis le module 00 (`MODULE_00_FONDATIONS.md`).
> Implémente uniquement le module 00, en respectant strictement la liste des fichiers
> autorisés. Ne modifie aucune version existante du catalogue de dépendances. Termine
> par le compte rendu demandé.
