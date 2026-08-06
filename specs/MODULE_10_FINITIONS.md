# MODULE 10 : Finitions

Statut : à faire
Prérequis : tous les modules précédents validés

## Objectif

Rendre l'application présentable et durable : états vides et messages d'erreur
soignés, écran de réglages, masquage de l'outillage de développement, et recette
complète.

Ce module ne crée aucune fonctionnalité métier nouvelle.

## Fonctionnalités

- Uniformiser les états de chargement, vide et erreur sur tous les écrans.
- Ajouter un écran de réglages informatif.
- Rendre l'écran de debug invisible en compilation `release`.
- Ajouter une icône d'application.
- Vérifier l'absence de textes en dur et de fuites.

## États uniformes

Trois composables partagés sont créés dans `core.ui.component` et utilisés partout :

| Composable | Usage |
|---|---|
| `LoadingState` | Indicateur centré, utilisé pendant le premier chargement |
| `EmptyState(icon, title, message, actionLabel, onAction)` | Contenu absent |
| `ErrorState(message, onRetry)` | Erreur bloquante avec bouton de reprise |

Applications imposées :

| Écran | Cas | Contenu |
|---|---|---|
| Accueil | Chargement initial | `LoadingState` |
| Détail | Aucun sujet en base | `EmptyState` avec « Aucun sujet », « Tirez vers le bas pour rafraîchir » |
| Détail | Tous les sujets masqués | `EmptyState` avec « Tous les sujets sont masqués » et une action « Afficher les masqués » |
| Détail | Erreur de première synchronisation | `ErrorState` avec « Réessayer » |

## Écran de réglages

- **Nom** : `SettingsScreen`
- **Objectif** : donner de la visibilité sur le fonctionnement de l'application.
- **Contenu**, en lecture seule sauf mention contraire :
  - section « Surveillance » : les deux forums, leur URL, leur date de dernière
    synchronisation, la période de rafraîchissement en cours (quinze minutes ou une
    heure en cas de blocage) ;
  - section « Notifications » : un bouton « Gérer les notifications » ouvrant les
    réglages système de l'application, où l'utilisateur pilote les trois canaux ;
  - section « Données » : le nombre de sujets stockés, un bouton « Effacer les données
    locales » avec confirmation, qui vide les sujets et remet les forums à l'état non
    amorcé ;
  - section « À propos » : nom de l'application, numéro de version issu de
    `BuildConfig.VERSION_NAME`, et une phrase rappelant que l'application se contente
    de consulter des pages publiques du forum.
- **Navigation** : accessible depuis un bouton de la barre supérieure de l'accueil,
  retour par la flèche.

L'accès à l'écran de debug est déplacé ici, dans une section « Développement » visible
uniquement si `BuildConfig.DEBUG` est vrai. La barre supérieure de l'accueil ne porte
donc plus le bouton de debug, ce qui allège l'écran principal.

## Masquage du debug

- La destination de debug reste déclarée dans le graphe, mais n'est plus atteignable
  en compilation `release` puisque le seul point d'entrée est conditionné par
  `BuildConfig.DEBUG`.
- Aucune suppression de code n'est demandée : l'outillage reste disponible pour les
  évolutions futures.

## Icône de l'application

Une icône adaptative est fournie, composée d'une forme simple évoquant un dé à vingt
faces ou une loupe, dans les tons de la couleur d'amorce `#8C3B3B`. Elle est générée
en vecteur, sans image bitmap.

Si Gemini ne peut pas produire un vecteur satisfaisant, il le signale et laisse
l'icône par défaut, ce qui ne bloque pas la recette.

## Vérifications de qualité

Gemini effectue et documente les contrôles suivants :

1. Aucune chaîne de caractères en dur dans les composables, tout est dans
   `strings.xml`.
2. Aucune valeur `dp` littérale hors `Dimens`, sauf tailles d'icônes.
3. Aucune couleur littérale hors du thème.
4. Aucun appel à `GlobalScope`, aucun `runBlocking` hors tests.
5. Toutes les collectes de flux dans Compose passent par
   `collectAsStateWithLifecycle()`.
6. Toutes les WebView créées sont détruites, y compris en cas d'annulation.
7. Aucun avertissement de compilation non justifié. Ceux qui subsistent sont listés
   dans le compte rendu avec leur raison.

## Modèle de données

Aucune modification du schéma.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `LoadingState` | `core.ui.component` | État de chargement |
| `EmptyState` | `core.ui.component` | État vide |
| `ErrorState` | `core.ui.component` | État d'erreur |
| `SettingsScreen` | `feature.settings` | Écran de réglages |
| `SettingsViewModel` | `feature.settings` | État et actions |
| `SettingsUiState` | `feature.settings` | État |
| `SettingsEvent` | `feature.settings` | Actions |
| `SettingsEffect` | `feature.settings` | Événements ponctuels |

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Effacement des données locales | Sujets supprimés, forums conservés mais non amorcés, la synchronisation suivante est silencieuse |
| Compilation release | Aucun accès au debug, application pleinement fonctionnelle |
| Version affichée | Correspond à `versionName` du fichier de build |
| Réglages ouverts sans aucune synchronisation | Les dates indiquent « Jamais synchronisé » |

## Fichiers autorisés

```
app/src/main/java/com/jdrvirtuel/watcher/core/ui/component/LoadingState.kt
app/src/main/java/com/jdrvirtuel/watcher/core/ui/component/EmptyState.kt
app/src/main/java/com/jdrvirtuel/watcher/core/ui/component/ErrorState.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsContract.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/forumdetail/ForumDetailScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/Routes.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/AppNavHost.kt
app/src/main/res/values/strings.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/drawable/ic_launcher_foreground.xml
app/src/main/res/values/ic_launcher_background.xml
```

Aucune dépendance nouvelle.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
gradlew assembleRelease
gradlew testDebugUnitTest
gradlew lintDebug
```

Les quatre commandes réussissent. `lintDebug` ne remonte aucune erreur bloquante, les
avertissements restants sont justifiés dans le compte rendu.

### Recette complète

Cette recette vaut réception de l'application entière.

1. Désinstaller toute version précédente, installer la version `debug`, lancer.
2. Accepter la permission de notification.
3. Tirer vers le bas pour synchroniser.
   Attendu : les deux forums se remplissent, aucune notification, badges à jour.
4. Ouvrir « Oneshots », comparer avec la page web dans un navigateur.
   Attendu : mêmes sujets, mêmes métadonnées, sujets complets grisés avec pastille,
   aucun sujet épinglé.
5. Ouvrir un sujet, revenir.
   Attendu : le navigateur s'ouvre au bon endroit, le titre n'est plus en gras, le
   badge de l'accueil a diminué de un.
6. Masquer un sujet, annuler, masquer de nouveau, activer l'affichage des masqués,
   quitter et revenir.
   Attendu : comportements conformes au module 06, l'option d'affichage est
   réinitialisée.
7. Mettre deux sujets sous surveillance.
   Attendu : ils remontent en tête de liste.
8. Ouvrir le debug, supprimer un sujet, décrémenter le compteur de réponses d'un sujet
   surveillé, puis synchroniser.
   Attendu : une notification de nouveau sujet et une notification de nouvelle
   réponse.
9. Fermer l'application, refaire la manipulation précédente puis forcer la tâche de
   fond par `adb`.
   Attendu : les notifications arrivent application fermée.
10. Simuler trois échecs Cloudflare depuis le debug.
    Attendu : bandeau d'alerte, période portée à une heure.
11. Passer par l'écran de vérification.
    Attendu : bandeau disparu, période revenue à quinze minutes, synchronisation
    relancée.
12. Ouvrir les réglages, effacer les données locales, resynchroniser.
    Attendu : la base se remplit de nouveau, sans aucune notification puisque
    l'amorçage recommence.
13. Faire pivoter l'appareil sur chaque écran.
    Attendu : aucun crash, aucun état perdu.
14. Basculer le système en thème sombre.
    Attendu : tous les écrans restent lisibles, y compris les éléments grisés.
15. Installer la version `release`, la lancer.
    Attendu : aucun accès au debug depuis les réglages, tout le reste fonctionne.
16. Laisser l'application installée une demi journée sans l'ouvrir.
    Attendu : les notifications de nouveaux sujets arrivent d'elles mêmes.

## Travail attendu de Gemini

Créer les composants d'état partagés, l'écran de réglages, l'icône, et appliquer les
modifications d'intégration listées.

Effectuer les sept vérifications de qualité et en rendre compte point par point,
en signalant tout écart constaté plutôt qu'en le corrigeant silencieusement dans un
fichier non autorisé.

## Prompt de démarrage

> Le module 09 est validé. Lis `00_SPECIFICATIONS_GENERALES.md` puis
> `MODULE_10_FINITIONS.md` et implémente uniquement le module 10. Effectue ensuite les
> sept vérifications de qualité listées et rends compte de chacune. Respecte la liste
> des fichiers autorisés et termine par le compte rendu demandé.
