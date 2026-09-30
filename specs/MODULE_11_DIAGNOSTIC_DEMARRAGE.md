# MODULE 11 : Diagnostic au démarrage

Statut : à faire
Prérequis : tous les modules précédents validés

## Objectif

Détecter les réglages système qui empêchent la synchronisation en arrière-plan de
fonctionner, et guider l'utilisateur vers les écrans de réglage concernés.

Sans ce module, une application parfaitement fonctionnelle peut paraître cassée : la
tâche de fond ne s'exécute jamais, aucune notification n'arrive, et rien n'explique
pourquoi.

Ce module ne fait pas : il ne modifie aucun réglage système, il se contente de
détecter, d'expliquer et d'ouvrir le bon écran.

## Contexte

Trois réglages conditionnent le bon fonctionnement de la surveillance :

1. **La permission de notification**, sur Android 13 et supérieur. Détectable et
   demandable par le code. Déjà traitée au module 08, reprise ici dans un diagnostic
   unifié.
2. **L'optimisation de batterie**. Détectable par `PowerManager.isIgnoringBatteryOptimizations`,
   et l'exemption peut être demandée par une intention système.
3. **La mise en veille propriétaire du constructeur**, en particulier sur Samsung.
   **Non détectable par le code** et sans intention officielle. On ne peut
   qu'expliquer le chemin et ouvrir les paramètres de batterie généraux.

Cette asymétrie est structurante : les deux premiers points donnent un état vrai ou
faux, le troisième reste une recommandation que l'utilisateur confirme lui-même.

## Fonctionnalités

- Vérifier les trois points au lancement de l'application.
- Afficher un écran de diagnostic si au moins un point n'est pas conforme.
- Passer directement à l'accueil si tout est conforme.
- Permettre de rouvrir ce diagnostic à tout moment depuis l'écran de réglages.
- Permettre à l'utilisateur d'ignorer le diagnostic et de ne plus le revoir.

## Règles métier

### Déclenchement

Au lancement, avant l'affichage de l'accueil, l'application évalue les trois points.

L'écran de diagnostic s'affiche si les deux conditions suivantes sont réunies :

- au moins un point détectable est non conforme, ou le point constructeur n'a pas
  encore été confirmé par l'utilisateur ;
- l'utilisateur n'a pas coché « Ne plus afficher ».

Sinon, l'accueil s'affiche directement, sans transition visible.

### État de chaque point

| Point | Détection | État possible |
|---|---|---|
| Notifications | `NotificationManagerCompat.areNotificationsEnabled()` | Conforme, non conforme |
| Optimisation de batterie | `PowerManager.isIgnoringBatteryOptimizations(packageName)` | Conforme, non conforme |
| Veille constructeur | Aucune | Confirmé par l'utilisateur, non confirmé |

Le troisième point est stocké dans `AppPreferences` sous la clé
`manufacturer_sleep_acknowledged`, mise à vrai lorsque l'utilisateur appuie sur
« C'est fait ».

### Persistance du refus

La case « Ne plus afficher ce diagnostic » écrit `diagnostic_dismissed` à vrai dans
`AppPreferences`. L'écran ne se déclenche alors plus automatiquement, mais reste
accessible depuis l'écran de réglages.

Réactiver le diagnostic depuis les réglages remet cette clé à faux.

## Écrans

### DiagnosticScreen

- **Nom** : `DiagnosticScreen`
- **Objectif** : expliquer et corriger les réglages bloquants.
- **Contenu** :
  - un titre et un paragraphe d'introduction expliquant que la surveillance
    automatique nécessite quelques autorisations ;
  - trois cartes, une par point, chacune contenant :
    - une icône d'état, coche verte si conforme, triangle d'alerte sinon ;
    - un titre court ;
    - une explication en une ou deux phrases de ce que le réglage empêche ;
    - un bouton d'action, masqué si le point est déjà conforme ;
  - une case à cocher « Ne plus afficher ce diagnostic » ;
  - un bouton « Continuer » menant à l'accueil.
- **Navigation** : destination de départ conditionnelle, ou atteinte depuis l'écran de
  réglages.

### Contenu des trois cartes

**Notifications**
Titre : « Autoriser les notifications ».
Explication : sans cette autorisation, l'application surveillera les forums mais ne
pourra pas vous prévenir des nouveautés.
Action : « Autoriser », qui déclenche la demande de permission sur Android 13 et
supérieur, ou ouvre les réglages de notification de l'application si la permission a
déjà été refusée définitivement.

**Optimisation de batterie**
Titre : « Désactiver l'optimisation de batterie ».
Explication : Android peut suspendre la surveillance pour économiser la batterie. Une
exemption permet de vérifier les forums régulièrement.
Action : « Régler », qui lance un `Intent` d'action
`Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` avec l'URI
`package:$packageName`.

**Veille constructeur**
Titre : « Retirer l'application des applications en veille ».
Explication : certains fabricants, notamment Samsung, mettent en veille les
applications peu utilisées, ce qui bloque totalement la surveillance. Le chemin exact
est indiqué à l'utilisateur : `Paramètres` > `Batterie` > `Limites d'utilisation en
arrière-plan`, puis retirer l'application des listes « Applications en veille » et
« Applications en veille profonde ».
Deux boutons : « Ouvrir les paramètres », qui lance
`Settings.ACTION_APPLICATION_DETAILS_SETTINGS` sur l'application, et « C'est fait »,
qui marque le point comme confirmé.

## Comportement

- Revenir dans l'application après une visite dans les réglages système réévalue les
  trois points, via un `LifecycleEventObserver` sur `ON_RESUME`. Les icônes d'état se
  mettent à jour sans action supplémentaire.
- Le bouton « Continuer » est toujours actif, même si des points restent non
  conformes. L'utilisateur n'est jamais bloqué.
- Sur les appareils antérieurs à Android 13, la carte des notifications s'affiche
  comme conforme sans action possible, la permission n'existant pas.

## Modèle de données

Aucune modification du schéma Room. Deux clés ajoutées à `AppPreferences` :
`manufacturer_sleep_acknowledged` et `diagnostic_dismissed`.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `DiagnosticScreen` | `feature.diagnostic` | Composable |
| `DiagnosticViewModel` | `feature.diagnostic` | État et actions |
| `DiagnosticUiState` | `feature.diagnostic` | État |
| `DiagnosticEvent` | `feature.diagnostic` | Actions |
| `DiagnosticEffect` | `feature.diagnostic` | Événements ponctuels |
| `SystemSettingsChecker` | `core.util` | Détection des trois points |
| `DiagnosticCard` | `feature.diagnostic` | Composable de carte |

### Route

```kotlin
@Serializable
data object DiagnosticRoute
```

La destination de départ du graphe est déterminée au lancement : `DiagnosticRoute` si
le diagnostic doit s'afficher, `HomeRoute` sinon. Cette décision est prise dans
`MainActivity`, sur la base d'une lecture unique des préférences, avant la composition
du graphe.

L'écran de démarrage système reste affiché tant que cette lecture n'est pas terminée,
afin d'éviter un clignotement entre les deux destinations possibles.

## Composants UI

`Scaffold`, `Card`, `Icon`, `Button`, `Checkbox`, `Text`, `LazyColumn`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Tous les points conformes | L'accueil s'affiche directement, aucun écran intermédiaire |
| Android antérieur à 13 | La carte notifications est conforme d'office |
| Permission refusée deux fois | Le bouton ouvre les réglages système au lieu de redemander |
| Utilisateur ignore tout et continue | L'application fonctionne, la surveillance peut être défaillante |
| « Ne plus afficher » coché | Plus de déclenchement automatique, accès conservé depuis les réglages |
| Retour depuis les réglages système | Les états se réévaluent automatiquement |
| Rotation | Aucune perte d'état |

## Fichiers autorisés

```
app/src/main/AndroidManifest.xml                    permission REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
app/src/main/java/com/jdrvirtuel/watcher/feature/diagnostic/DiagnosticScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/diagnostic/DiagnosticViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/diagnostic/DiagnosticContract.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/diagnostic/DiagnosticCard.kt
app/src/main/java/com/jdrvirtuel/watcher/core/util/SystemSettingsChecker.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/prefs/AppPreferences.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/Routes.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/AppNavHost.kt
app/src/main/java/com/jdrvirtuel/watcher/MainActivity.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsContract.kt
app/src/main/res/values/strings.xml
```

Aucune dépendance nouvelle.

Note sur la permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` : elle est nécessaire
pour lancer l'intention de demande d'exemption. Elle est interdite sur le Play Store
sauf justification, ce qui n'a pas d'incidence pour une application distribuée hors
magasin. Si une publication était envisagée un jour, il faudrait retirer cette carte
et se contenter d'ouvrir les réglages généraux.

## Tests

Aucun test automatisé.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
gradlew lintDebug
```

### Scénario manuel

1. Désinstaller l'application, la réinstaller, la lancer.
   Résultat attendu : l'écran de diagnostic s'affiche, les trois cartes sont en
   alerte.
2. Appuyer sur « Autoriser » de la carte notifications et accepter.
   Résultat attendu : la carte passe en coche verte, le bouton disparaît.
3. Appuyer sur « Régler » de la carte batterie et accepter l'exemption.
   Résultat attendu : au retour dans l'application, la carte passe en coche verte sans
   action supplémentaire.
4. Appuyer sur « Ouvrir les paramètres » de la carte constructeur, revenir sans rien
   changer, puis appuyer sur « C'est fait ».
   Résultat attendu : la carte passe en coche verte.
5. Appuyer sur « Continuer ».
   Résultat attendu : l'accueil s'affiche.
6. Fermer complètement l'application et la relancer.
   Résultat attendu : l'accueil s'affiche directement, sans écran de diagnostic.
7. Retirer l'exemption de batterie depuis les réglages Android, puis relancer
   l'application.
   Résultat attendu : le diagnostic réapparaît, avec la seule carte batterie en
   alerte.
8. Cocher « Ne plus afficher ce diagnostic » puis « Continuer ». Relancer
   l'application.
   Résultat attendu : l'accueil s'affiche directement malgré le point non conforme.
9. Ouvrir l'écran de réglages, rouvrir le diagnostic depuis le lien prévu.
   Résultat attendu : l'écran s'affiche, et l'option « Ne plus afficher » peut être
   décochée.

## Travail attendu de Gemini

Créer l'écran de diagnostic, le détecteur, et les modifications minimales dans
`MainActivity`, le graphe de navigation et l'écran de réglages.

Ne pas dupliquer la demande de permission de notification du module 08 : réutiliser le
mécanisme existant si possible, et le signaler dans le compte rendu si ce n'est pas
faisable proprement.

## Prompt de démarrage

> Le module 10 est validé. Lis `specs/00_SPECIFICATIONS_GENERALES.md` puis
> `specs/MODULE_11_DIAGNOSTIC_DEMARRAGE.md` et implémente uniquement le module 11.
> Attention : la mise en veille propriétaire des constructeurs n'est pas détectable par
> le code, cette carte repose sur une confirmation de l'utilisateur. Respecte la liste
> des fichiers autorisés et termine par le compte rendu demandé.
