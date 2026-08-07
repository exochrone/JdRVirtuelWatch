# MODULE 09 : Vérification Cloudflare

Statut : à faire
Prérequis : modules 02 et 08 validés

## Objectif

Traiter le cas où Cloudflare exige une intervention humaine : prévenir l'utilisateur,
lui offrir un moyen de valider en deux secondes, et reprendre la surveillance
normalement.

Le principe directeur est qu'une application de veille ne doit jamais rester
silencieuse quand elle est aveugle. Sans ce module, un blocage prolongé serait
indiscernable d'un forum calme.

## Fonctionnalités

- Détecter qu'une synchronisation a retourné `CHALLENGE_REQUIRED`.
- Émettre une notification de vérification.
- Proposer un écran avec WebView visible pour valider le challenge.
- Reprendre la synchronisation après validation.
- Espacer les tentatives après trois échecs consécutifs.
- Afficher un bandeau d'alerte persistant sur l'écran d'accueil.

## Règles métier

### Comptage des échecs

Le compteur `consecutiveChallengeFailures` de `AppPreferences` est incrémenté à chaque
synchronisation retournant `CHALLENGE_REQUIRED`, et remis à zéro à chaque
synchronisation réussie.

Le comptage est global, pas par forum : le challenge concerne le domaine entier.

### Notification

Une notification de vérification est émise dès le premier `CHALLENGE_REQUIRED`.

Pour éviter le harcèlement, elle n'est pas réémise plus d'une fois par heure. La date
de la dernière émission est stockée dans `lastChallengePromptAt`.

Contenu :

- canal `verification`, importance haute ;
- titre : « Vérification requise » ;
- texte : « Le forum demande une vérification. Appuyez pour la faire. » ;
- appui : ouverture de l'écran de vérification dans l'application, et non du
  navigateur.

La notification est retirée dès qu'une synchronisation réussit.

### Espacement des tentatives

À partir de trois échecs consécutifs, la tâche périodique est reprogrammée avec une
période d'une heure au lieu de quinze minutes. Elle revient à quinze minutes dès la
première réussite.

Cette reprogrammation passe par `SyncScheduler`, avec la politique
`ExistingPeriodicWorkPolicy.UPDATE` afin de remplacer la tâche existante.

### Bandeau d'accueil

Dès que `consecutiveChallengeFailures` est supérieur ou égal à un, un bandeau
persistant apparaît en haut de l'écran d'accueil :

- fond `errorContainer`, texte `onErrorContainer` ;
- icône d'alerte ;
- texte : « Accès au forum bloqué, vérification requise » ;
- bouton « Vérifier » menant à l'écran de vérification.

Le bandeau disparaît dès qu'une synchronisation réussit.

## Écrans

### VerificationScreen

- **Nom** : `VerificationScreen`
- **Objectif** : permettre à l'utilisateur de résoudre manuellement le challenge.
- **Contenu** :
  - `TopAppBar` titrée « Vérification », avec une flèche de retour ;
  - un texte d'explication : « Le forum demande une vérification anti robot. Validez la
    case ci dessous, la page se fermera automatiquement. » ;
  - une WebView **visible**, occupant le reste de l'écran, chargeant l'URL du forum 15 ;
  - un indicateur de chargement pendant l'analyse.
- **Actions possibles** :
  - interagir avec la WebView ;
  - revenir en arrière manuellement.
- **Navigation** : atteinte depuis le bandeau d'accueil ou depuis la notification.

### Comportement de l'écran

1. La WebView ne modifie pas `settings.userAgentString`, exactement comme
   `WebViewForumPageSource`. Imposer un user agent fait basculer Cloudflare en
   challenge interactif et rend le cookie éphémère. Voir l'encadré du module 02.
2. Elle partage le `CookieManager` de l'application, ce qui est le comportement par
   défaut.
3. À chaque `onPageFinished`, le contenu est examiné : si le marqueur `topictitle`
   apparaît, la vérification a réussi.
4. En cas de réussite :
   - `CookieManager.flush()` est appelé ;
   - le compteur d'échecs est remis à zéro ;
   - la tâche périodique est reprogrammée à quinze minutes ;
   - une synchronisation immédiate est déclenchée ;
   - un message temporaire « Vérification réussie » s'affiche ;
   - l'écran se ferme automatiquement et revient à l'accueil.
5. Si l'utilisateur quitte l'écran sans que la vérification aboutisse, rien n'est
   modifié.

La WebView est détruite proprement à la sortie de l'écran, comme au module 02.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `VerificationScreen` | `feature.verification` | Composable avec WebView visible |
| `VerificationViewModel` | `feature.verification` | État et effets |
| `VerificationUiState` | `feature.verification` | État |
| `VerificationEvent` | `feature.verification` | Actions |
| `VerificationEffect` | `feature.verification` | Événements ponctuels |
| `ChallengeStateRepository` | `domain.repository` | Interface de gestion des échecs |
| `ChallengeStateRepositoryImpl` | `data.repository` | Implémentation sur `AppPreferences` |
| `ChallengeBanner` | `core.ui.component` | Bandeau d'alerte |

La WebView visible est intégrée par `AndroidView`, avec une `factory` créant la
WebView et un `onRelease` la détruisant.

### Route

```kotlin
@Serializable
data object VerificationRoute
```

Un lien profond est déclaré pour que la notification puisse y mener directement.

### Points d'accrochage

- `SyncForumUseCase` incrémente ou remet à zéro le compteur via
  `ChallengeStateRepository`. C'est la seule modification autorisée dans un fichier du
  module 04, et elle doit rester minimale.
- `SystemNewContentNotifier` reste inchangé. L'émission de la notification de
  vérification est faite par `AppNotifier`, appelé depuis `SyncWorker`.
- `SyncWorker` déclenche la notification de vérification et l'espacement des
  tentatives. C'est la seule modification autorisée dans un fichier du module 07.

## Modèle de données

Aucune modification du schéma Room. Les deux clés de préférences existent déjà depuis
le module 01.

## Composants UI

`AndroidView`, `Scaffold`, `TopAppBar`, `Surface`, `Icon`, `TextButton`,
`CircularProgressIndicator`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Challenge résolu automatiquement | L'écran de vérification n'apparaît jamais |
| Challenge exigeant une case | Notification, bandeau, écran de vérification |
| Utilisateur quittant sans valider | Le compteur reste inchangé, le bandeau persiste |
| Trois échecs consécutifs | Période portée à une heure |
| Réussite après espacement | Retour à quinze minutes |
| Notification répétée | Au plus une par heure |
| Aucun réseau sur l'écran de vérification | Message d'erreur, aucun crash |
| Rotation pendant la vérification | La WebView est recréée, l'utilisateur recommence |
| Permission de notification refusée | Le bandeau reste le seul canal d'alerte, ce qui est acceptable |

## Fichiers autorisés

```
app/src/main/java/com/jdrvirtuel/watcher/feature/verification/VerificationScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/verification/VerificationViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/verification/VerificationContract.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/repository/ChallengeStateRepository.kt
app/src/main/java/com/jdrvirtuel/watcher/data/repository/ChallengeStateRepositoryImpl.kt
app/src/main/java/com/jdrvirtuel/watcher/core/ui/component/ChallengeBanner.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/AppNotifier.kt      ajout d'une méthode
app/src/main/java/com/jdrvirtuel/watcher/work/SyncWorker.kt               ajout du traitement
app/src/main/java/com/jdrvirtuel/watcher/work/SyncScheduler.kt            ajout de la période longue
app/src/main/java/com/jdrvirtuel/watcher/domain/usecase/SyncForumUseCase.kt  compteur uniquement
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeScreen.kt       ajout du bandeau
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeViewModel.kt    exposition de l'état
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeContract.kt
app/src/main/java/com/jdrvirtuel/watcher/core/di/RepositoryModule.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/Routes.kt
app/src/main/java/com/jdrvirtuel/watcher/navigation/AppNavHost.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugContract.kt
app/src/main/res/values/strings.xml
```

Aucune dépendance nouvelle.

## Extension de l'écran de debug

Une section « Cloudflare » est ajoutée :

- l'état courant du compteur d'échecs consécutifs ;
- la date de la dernière notification de vérification ;
- un bouton « Effacer les cookies », qui vide le `CookieManager` et appelle `flush()`,
  ce qui invalide la session ;
- un bouton « Simuler trois échecs », qui force le compteur à trois ;
- un bouton « Réinitialiser le compteur ».

Ces outils sont le seul moyen de valider ce module sans attendre que Cloudflare
décide d'exiger une vérification.

## Tests

Aucun test automatisé.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
```

### Scénario manuel

1. Synchroniser normalement, vérifier que le compteur d'échecs vaut zéro et qu'aucun
   bandeau n'apparaît.
2. Ouvrir le debug, appuyer sur « Simuler trois échecs », revenir à l'accueil.
   Résultat attendu : le bandeau rouge « Accès au forum bloqué » apparaît.
3. Appuyer sur « Vérifier ».
   Résultat attendu : l'écran de vérification s'ouvre, la WebView charge le forum.
4. Attendre que la page du forum s'affiche.
   Résultat attendu : la vérification est détectée comme réussie, un message
   « Vérification réussie » apparaît, l'écran se ferme, le bandeau a disparu, et une
   synchronisation s'est déclenchée.
5. Ouvrir le debug, appuyer sur « Effacer les cookies », puis lancer une
   synchronisation.
   Résultat attendu : selon l'humeur de Cloudflare, soit la synchronisation réussit
   après résolution automatique, soit elle retourne une vérification requise, auquel
   cas une notification et le bandeau apparaissent.
6. Si une notification de vérification apparaît, appuyer dessus.
   Résultat attendu : l'écran de vérification s'ouvre directement, sans passer par
   l'accueil.
7. Simuler trois échecs, puis ouvrir la section Tâche de fond du debug.
   Résultat attendu : la tâche périodique a été reprogrammée avec une période d'une
   heure.
8. Réussir une vérification.
   Résultat attendu : la période revient à quinze minutes.
9. Simuler des échecs de façon répétée en moins d'une heure.
   Résultat attendu : une seule notification de vérification est émise sur la période.

## Travail attendu de Gemini

Créer l'écran de vérification, le dépôt d'état, le bandeau, et les modifications
minimales listées dans les fichiers existants.

Les modifications de `SyncForumUseCase` et de `SyncWorker` doivent se limiter au
strict nécessaire : incrémentation ou remise à zéro du compteur, émission de la
notification, reprogrammation de la tâche. Aucune réécriture de leur logique.

Attention : la WebView visible ne doit imposer aucun user agent, exactement comme
celle du module 02.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 08 est validé. Lis `00_SPECIFICATIONS_GENERALES.md` puis
> `MODULE_09_VERIFICATION_CLOUDFLARE.md` et implémente uniquement le module 09. La
> WebView visible ne doit imposer aucun user agent, comme
> `WebViewForumPageSource`. Limite les modifications de `SyncForumUseCase` et
> `SyncWorker` au strict nécessaire. Respecte la liste des fichiers autorisés et
> termine par le compte rendu demandé.
