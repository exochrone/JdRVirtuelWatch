# MODULE 16 : Priorité, format et actions de la notification unique

Statut : à faire
Prérequis : module 15 validé

## Objectif

Faire évoluer la notification unique gérée par `StatusNotifier` sur quatre points :

1. Sa **priorité** varie selon la situation : discrète quand rien ne se passe, visible
   en cas de nouveauté, maximale en cas de problème.
2. Son **contenu** adopte un format fixe sur deux lignes, une par forum.
3. Elle porte **trois actions** : « Synchroniser », et un accès direct à chacun des
   deux forums.
4. Les compteurs de nouveautés **s'accumulent** jusqu'à l'ouverture de
   l'application, au lieu d'être remis à zéro à chaque synchronisation.

Ce module ne fait pas : aucune modification de la détection des nouveautés, aucun
service de premier plan, aucun receveur de démarrage, aucune modification du schéma
Room.

## Ce qui change

| Avant | Après |
|---|---|
| Un canal `status` en `IMPORTANCE_DEFAULT` | Trois canaux, un par niveau, le canal `status` est supprimé |
| Actions « Synchroniser » et « Ouvrir » | Actions « Synchroniser », « Oneshots », « Campagnes » |
| Texte variable selon les cas, jusqu'à cinq lignes | Toujours deux lignes, une par forum |
| Nom du premier nouveau sujet affiché s'il est seul | Plus de titre de sujet, uniquement des compteurs |
| Compteurs remis à zéro au début de chaque synchronisation | Compteurs cumulés jusqu'à l'ouverture de l'application |
| Retour au repos à la première synchro sans nouveauté (modules 14 et 15) | Retour au repos à l'ouverture de l'application |
| Limite de cinq lignes et ligne « et N autres » (module 14) | Supprimées, le corps compte toujours deux lignes |
| Échec signalé seulement si les deux forums échouent | Échec signalé dès qu'un forum échoue |
| Aucun signalement si la synchronisation s'arrête | Alerte si aucune synchronisation réussie depuis une heure |

L'appui sur la notification elle-même est inchangé : il ouvre l'écran d'accueil.

### Ce qui ne change pas

- La notification reste unique, permanente (`setOngoing(true)`), sous l'identifiant
  `NotificationIds.STATUS`.
- L'interrupteur « Notifications » des réglages, clé `status_notification_enabled`,
  supprime toujours toute notification lorsqu'il est désactivé.
- Le journal des notifications continue d'enregistrer les événements, et les mises à
  jour de la notification elle-même n'y sont toujours pas inscrites.
- `SyncActionReceiver` reste minimal, sans Hilt ni `goAsync()`.
- `StatusNotifier.update()` reste appelée depuis `JdrVirtuelWatcherApp`,
  `SyncWorker`, `HomeViewModel`, `ForumDetailViewModel` et `SettingsViewModel`.
- Aucun service de premier plan, aucun receveur de démarrage.

### Décision révisée

Le module 14 prévoyait que les nouveautés restent affichées jusqu'à la synchronisation
suivante, même après consultation de l'application. À l'usage, une nouveauté n'était
ainsi visible qu'un quart d'heure. La règle est inversée : les nouveautés restent
affichées tant que l'application n'a pas été ouverte, quel que soit le nombre de
synchronisations intermédiaires.

## Principe : un canal par niveau

L'importance d'une notification est portée par son canal, et une application ne peut
plus modifier l'importance d'un canal après sa création. Pour faire varier la
priorité, la notification est donc republiée, sous le même identifiant
`NotificationIds.STATUS`, sur un canal différent.

## Canaux de notification

| Identifiant | Nom affiché | Importance | Badge |
|---|---|---|---|
| `status_idle` | Surveillance : rien de nouveau | `IMPORTANCE_MIN` | Non |
| `status_new` | Surveillance : nouveautés | `IMPORTANCE_DEFAULT` | Oui |
| `status_alert` | Surveillance : problème | `IMPORTANCE_HIGH` | Oui |

Descriptions des canaux :

- `status_idle` : « Affiche la surveillance en cours quand aucune nouveauté n'est en
  attente. »
- `status_new` : « Signale les nouveaux sujets et les nouvelles réponses. »
- `status_alert` : « Signale un échec de synchronisation, une vérification requise ou
  une surveillance interrompue. »

`NotificationChannels.create()` crée ces trois canaux et supprime le canal `status`,
en plus des suppressions déjà présentes.

Si l'utilisateur modifie l'importance d'un canal dans les réglages Android, son choix
prime. Aucun code ne tente de le contourner.

## Niveaux et raisons

### Calcul

Le calcul du niveau est isolé dans une classe pure, `StatusLevelResolver`, sans
dépendance Android, afin d'être testable unitairement.

```kotlin
enum class StatusLevel { IDLE, NEW, ALERT }

enum class AlertReason { VERIFICATION, SYNC_FAILED, STALE }

data class ForumSyncState(
    val lastSyncAt: Long?,
    val lastSyncSuccess: Boolean,
    val lastSyncError: String?
)

data class StatusDecision(
    val level: StatusLevel,
    val reason: AlertReason?
)

class StatusLevelResolver {
    fun resolve(
        forums: List<ForumSyncState>,
        challengeFailures: Int,
        pendingNewCount: Int,
        now: Long
    ): StatusDecision

    companion object {
        const val STALE_THRESHOLD_MS = 60 * 60 * 1000L
    }
}
```

`pendingNewCount` est la somme, tous forums confondus, des nouveaux sujets et des
nouvelles réponses en attente dans `SyncHighlights`.

### Règles, dans cet ordre, la première qui s'applique l'emporte

| # | Condition | Décision |
|---|---|---|
| 1 | `challengeFailures >= 1` | `ALERT` / `VERIFICATION` |
| 2 | Au moins un forum avec `lastSyncSuccess == false` et `lastSyncError != null` | `ALERT` / `SYNC_FAILED` |
| 3 | Dernière synchro réussie connue, et `now - dernière >= STALE_THRESHOLD_MS` | `ALERT` / `STALE` |
| 4 | `pendingNewCount > 0` | `NEW` |
| 5 | Sinon | `IDLE` |

La « dernière synchro réussie » est la plus récente des valeurs `lastSyncAt` des
forums, c'est-à-dire l'heure affichée dans la notification.

Avant toute synchronisation réussie, aucune des règles 1 à 4 ne s'applique
normalement : le niveau est `IDLE`.

## Contenu de la notification

### Titre

Le titre est rendu en gras par le système. L'heure de la dernière synchronisation
réussie est affichée par le système à côté du titre, ou à côté du nom de
l'application quand la notification est dépliée, grâce à `setWhen()` et
`setShowWhen(true)`, comme aujourd'hui. Le système affiche l'heure pour une date du
jour et la date pour une date plus ancienne : aucun formatage n'est à écrire.

| Situation | Titre |
|---|---|
| `IDLE` ou `NEW` | Dernière synchro |
| `ALERT` / `VERIFICATION` | Vérification requise |
| `ALERT` / `SYNC_FAILED` | Échec de synchro |
| `ALERT` / `STALE` | Pas de synchro depuis 1 h |
| Aucune synchro réussie à ce jour | Aucune synchronisation effectuée |

Si aucune synchro n'a jamais réussi, `setShowWhen(false)`.

### Corps

Toujours deux lignes, une par forum, dans l'ordre croissant des identifiants, quel
que soit le niveau :

```
• {nom du forum} {nombre de sujets visibles}[ / +N sujet(s)][ / +M réponse(s)]
```

- Le nombre de sujets visibles est celui des sujets non masqués, soit le compteur `x`
  de l'écran d'accueil. Il provient de `TopicRepository.observeVisibleCount()`.
- Le segment « sujets » n'apparaît que si N > 0, le segment « réponses » que si M > 0.
- N est le nombre de titres cumulés dans `SyncHighlights.newTopicsByForum` pour ce
  forum, M la valeur de `SyncHighlights.newReplyCountByForum`.
- Le nom du forum provient de la base, jamais d'une valeur codée en dur.

Exemples :

```
• Oneshots 4
• Campagnes 1
```

```
• Oneshots 4 / +1 sujet / +2 réponses
• Campagnes 1 / +2 sujets / +1 réponse
```

Mise en forme :

- `setContentText()` reçoit la première ligne, visible notification repliée.
- `NotificationCompat.BigTextStyle().bigText()` reçoit les deux lignes séparées par
  `\n`, visibles notification dépliée.

### Actions

Trois actions, dans cet ordre :

| Libellé | Icône | Effet |
|---|---|---|
| Synchroniser | `ic_sync` | Inchangé : `SyncActionReceiver` |
| Nom du forum 15 | `ic_open` | Ouvre l'écran de détail du forum 15 |
| Nom du forum 16 | `ic_open` | Ouvre l'écran de détail du forum 16 |

Les libellés des deux dernières actions sont les noms des forums tels qu'en base.

L'action « Ouvrir » est supprimée : l'appui sur la notification remplit déjà ce rôle.

## Accès direct à un forum

### Lien profond

La destination `ForumDetailRoute` reçoit un lien profond typé :

```kotlin
composable<ForumDetailRoute>(
    deepLinks = listOf(
        navDeepLink<ForumDetailRoute>(basePath = "jdrvirtuel://forum")
    )
) { ... }
```

Ce qui produit des adresses de la forme `jdrvirtuel://forum/15`.

### Intent de l'action

```kotlin
Intent(Intent.ACTION_VIEW, "jdrvirtuel://forum/${forum.id}".toUri(), context, MainActivity::class.java)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
```

Intent explicit : aucun `intent-filter` n'est nécessaire dans le manifeste.
`PendingIntent.getActivity` avec les codes de requête 2 et 3, et les drapeaux
`FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`.

Le composant de navigation reconstruit alors une pile avec l'écran d'accueil
dessous : le bouton retour depuis le détail du forum ramène à l'accueil.

### `MainActivity`

- Conserver une référence au `NavHostController` créé dans `setContent`.
- Dans `onNewIntent`, appeler `setIntent(intent)` puis
  `navController.handleDeepLink(intent)`.
- Supprimer la méthode vide `handleIntent` et ses appels.

## Cumul des compteurs

### Suppression de la remise à zéro automatique

- `SyncAllForumsUseCase` n'appelle plus `notifier.clearHighlights()`.
- `SyncForumUseCase` perd son paramètre `clearHighlights` et n'appelle plus
  `notifier.clearHighlights()`.
- `SyncAllForumsUseCase` appelle donc `syncForumUseCase(15)` et
  `syncForumUseCase(16)` sans argument supplémentaire.

`SystemNewContentNotifier` additionne déjà les nouveautés aux valeurs existantes :
aucune modification n'y est nécessaire.

### Remise à zéro à l'ouverture

L'application est considérée comme ouverte dès que `MainActivity` passe à l'état
`RESUMED`, quel que soit le point d'entrée : lanceur, appui sur la notification, ou
action d'accès direct à un forum.

Dans `MainActivity.onResume()` :

```kotlin
lifecycleScope.launch {
    newContentNotifier.clearHighlights()
    statusNotifier.update()
}
```

`NewContentNotifier` et `StatusNotifier` sont injectés dans `MainActivity`.

## Sonorisation

La décision de faire sonner la notification dépend du niveau et du niveau
précédemment publié.

| Niveau publié | Son, vibration et bandeau |
|---|---|
| `IDLE` | Jamais |
| `NEW` | Uniquement si `hasNewTopics` est vrai lors de cet appel |
| `ALERT` | Uniquement si le niveau précédent n'était pas `ALERT`, ou si la raison a changé |

La règle du module 15 est conservée : les nouvelles réponses font passer la
notification dans le canal `status_new`, donc dans la section des alertes, mais sans
son. Seuls les nouveaux sujets font sonner.

Mise en œuvre : `setSilent(true)` dès que la notification ne doit pas sonner. Le
paramètre `setOnlyAlertOnce` n'est plus utilisé, car il ne couvre pas le cas d'une
republication après annulation.

### Mémorisation du niveau publié

Le niveau publié est mémorisé en DataStore, pour survivre à l'arrêt du processus
entre deux synchronisations :

| Clé | Type | Valeur |
|---|---|---|
| `last_status_signature` | String | `IDLE`, `NEW`, `ALERT_VERIFICATION`, `ALERT_SYNC_FAILED` ou `ALERT_STALE` |

Ajout dans `AppPreferences` : un flux `lastStatusSignature` et une méthode
`setLastStatusSignature(value: String?)`.

### Changement de canal

Si le canal à utiliser diffère de celui qui correspond à la signature mémorisée, ou si
aucune signature n'est mémorisée :

1. `notificationManager.cancel(NotificationIds.STATUS)` ;
2. puis `notify()` sur le nouveau canal.

Sinon, `notify()` seul suffit.

Après chaque publication, la signature est enregistrée.

## Détection de l'absence de synchronisation

### Principe

Si la tâche de fond ne s'exécute plus, aucun code ne tourne pour signaler le
problème. L'alerte est donc **pré-programmée** : à chaque mise à jour de la
notification, une alarme est calée sur l'instant où la dernière synchro réussie
atteindra une heure d'ancienneté.

### `StaleCheckScheduler`

Classe `@Singleton` du package `notification` :

- `schedule(triggerAt: Long)` : `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP,
  triggerAt, pendingIntent)`.
- `cancel()`.
- `PendingIntent.getBroadcast` vers `StaleCheckReceiver`, code de requête 10,
  `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`. Chaque appel remplace l'alarme précédente.

Aucune alarme exacte : elle exigerait une permission supplémentaire, et quelques
minutes de retard sont acceptables.

### Appel depuis `StatusNotifier.update()`

À la fin de chaque `update()` :

- si la notification est désactivée dans les réglages : `cancel()` ;
- sinon, si une dernière synchro réussie est connue et que le niveau n'est pas
  `ALERT` / `STALE` : `schedule(dernière + STALE_THRESHOLD_MS)` ;
- sinon : `cancel()`.

Aucun appel n'est à ajouter dans `SyncWorker` : il appelle déjà `update()` après
chaque synchronisation.

### `StaleCheckReceiver`

- `@AndroidEntryPoint`, déclaré dans le manifeste avec `exported="false"`.
- Utilise `goAsync()`, lance `statusNotifier.update()` dans le scope
  `@ApplicationScope` existant, puis appelle `finish()` dans un bloc `finally`.
- Aucun autre traitement : le résolveur décide lui-même si la synchro est périmée.

C'est une exception assumée à la règle de minimalisme de `SyncActionReceiver` : ce
receveur doit lire la base pour recalculer l'état.

### Redémarrage du téléphone

Les alarmes sont perdues au redémarrage. Aucun receveur de démarrage n'est ajouté :
au redémarrage, WorkManager relance la synchronisation périodique, ce qui recrée le
processus, appelle `update()` et réarme l'alarme.

Limite connue : si la tâche de fond ne repart jamais après un redémarrage, l'alerte
« Pas de synchro depuis 1 h » ne sera affichée qu'à la prochaine ouverture de
l'application.

## Chaînes de caractères

À ajouter :

```xml
<string name="notification_channel_status_idle">Surveillance : rien de nouveau</string>
<string name="notification_channel_status_idle_desc">Affiche la surveillance en cours quand aucune nouveauté n\'est en attente.</string>
<string name="notification_channel_status_new">Surveillance : nouveautés</string>
<string name="notification_channel_status_new_desc">Signale les nouveaux sujets et les nouvelles réponses.</string>
<string name="notification_channel_status_alert">Surveillance : problème</string>
<string name="notification_channel_status_alert_desc">Signale un échec de synchronisation, une vérification requise ou une surveillance interrompue.</string>
<string name="notification_status_stale_title">Pas de synchro depuis 1 h</string>
<string name="notification_status_line">• %1$s %2$d</string>
<string name="notification_status_separator">" / "</string>
<plurals name="notification_status_topics">
    <item quantity="one">+1 sujet</item>
    <item quantity="other">+%1$d sujets</item>
</plurals>
<plurals name="notification_status_replies">
    <item quantity="one">+1 réponse</item>
    <item quantity="other">+%1$d réponses</item>
</plurals>
```

Les guillemets de `notification_status_separator` sont indispensables : sans eux,
Android supprime les espaces en début et en fin de chaîne.

À supprimer, devenues inutiles : `notification_channel_status`,
`notification_channel_status_desc`, `notification_status_open`,
`notification_status_cloudflare_text`, `notification_status_repos`,
`notification_status_new_topics_single`, `notification_status_new_topics_multiple`,
`notification_status_forum_replies`, `notification_status_others`.

Avant chaque suppression, Gemini vérifie par recherche dans le projet que la chaîne
n'est plus utilisée ailleurs. S'il en trouve un usage, il la conserve et le signale.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `StatusLevelResolver` | `notification` | Calcul pur du niveau, nouvelle classe |
| `StatusNotifier` | `notification` | Construction et publication, modifiée |
| `StaleCheckScheduler` | `notification` | Programmation de l'alarme, nouvelle classe |
| `StaleCheckReceiver` | `notification` | Réception de l'alarme, nouvelle classe |
| `NotificationChannels` | `notification` | Trois canaux, modifiée |

`StatusNotifier.update(hasNewTopics: Boolean = false)` garde sa signature : aucun
appelant n'a à changer.

## Modèle de données

Aucune modification du schéma Room. Une clé DataStore ajoutée :
`last_status_signature`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Synchro sans nouveauté, rien en attente | `IDLE`, section silencieuse, aucune icône dans la barre d'état |
| Nouveaux sujets | `NEW`, son et bandeau |
| Nouvelles réponses uniquement | `NEW`, section des alertes, sans son |
| Synchro suivante sans nouveauté, application non ouverte | Reste `NEW`, compteurs inchangés, sans son |
| Deux synchros successives avec nouveautés | Compteurs additionnés |
| Ouverture de l'application | Compteurs remis à zéro, retour en `IDLE` |
| Action « Oneshots » ou « Campagnes » | Écran de détail ouvert, compteurs remis à zéro |
| Retour depuis le détail ouvert par l'action | Écran d'accueil |
| Échec de synchro d'un seul forum | `ALERT` / `SYNC_FAILED`, son et bandeau une fois |
| Échec répété sur plusieurs synchros | Reste `ALERT`, sans nouveau son |
| Passage d'un échec à une vérification Cloudflare | Nouveau son, la raison a changé |
| Aucune synchro réussie depuis une heure | `ALERT` / `STALE` par l'alarme, même application fermée |
| Nouveautés en attente pendant une alerte | Titre d'alerte, compteurs toujours visibles dans les lignes |
| Synchro réussie après une alerte | Retour en `NEW` ou `IDLE`, sans son sauf nouveaux sujets |
| Interrupteur « Notifications » désactivé | Aucune notification, alarme annulée |
| Interrupteur réactivé | Notification reposée immédiatement, alarme réarmée |
| Permission de notification refusée | Aucune notification, aucun crash |
| Synchro pendant que l'application est au premier plan | Compteurs cumulés, remis à zéro à la prochaine reprise de l'application |

## Fichiers autorisés

```
app/src/main/AndroidManifest.xml                                                    déclaration de StaleCheckReceiver uniquement
app/src/main/java/com/jdrvirtuel/watcher/notification/StatusNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/StatusLevelResolver.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/StaleCheckScheduler.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/StaleCheckReceiver.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/NotificationChannels.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/prefs/AppPreferences.kt         ajout de la clé uniquement
app/src/main/java/com/jdrvirtuel/watcher/domain/usecase/SyncAllForumsUseCase.kt     retrait de la remise à zéro
app/src/main/java/com/jdrvirtuel/watcher/domain/usecase/SyncForumUseCase.kt         retrait du paramètre et de la remise à zéro
app/src/main/java/com/jdrvirtuel/watcher/navigation/AppNavHost.kt                   lien profond du détail uniquement
app/src/main/java/com/jdrvirtuel/watcher/MainActivity.kt                            onResume et onNewIntent
app/src/main/res/values/strings.xml
app/src/test/java/com/jdrvirtuel/watcher/notification/StatusLevelResolverTest.kt
```

Aucune dépendance nouvelle.

## Contrat exposé aux modules suivants

- `StatusLevelResolver` et ses types, réutilisables pour un indicateur d'état dans
  l'interface.
- Le lien profond `jdrvirtuel://forum/{forumId}`.

## Tests

Tests unitaires de `StatusLevelResolver`, avec `now` fixé :

| Cas | Entrée | Attendu |
|---|---|---|
| Aucune synchro | forums sans `lastSyncAt`, sans erreur, 0 en attente | `IDLE` |
| Synchro récente, rien en attente | succès il y a 10 min | `IDLE` |
| Synchro récente, nouveautés | succès il y a 10 min, 3 en attente | `NEW` |
| Échec d'un forum | un forum `lastSyncSuccess = false` avec erreur | `ALERT` / `SYNC_FAILED` |
| Vérification | `challengeFailures = 1` et un forum en échec | `ALERT` / `VERIFICATION` |
| Péremption | succès il y a 61 min | `ALERT` / `STALE` |
| Limite exacte | succès il y a 60 min pile | `ALERT` / `STALE` |
| Juste avant | succès il y a 59 min 59 s | `IDLE` |
| Péremption et nouveautés | succès il y a 61 min, 2 en attente | `ALERT` / `STALE` |
| Dernière synchro retenue | forum 15 il y a 70 min, forum 16 il y a 5 min | `IDLE` |

## Critères d'acceptation

### Vérification automatique

```
gradlew testDebugUnitTest
gradlew assembleDebug
gradlew lintDebug
```

### Scénario manuel

1. Installer, ouvrir l'application, la fermer, attendre une synchronisation sans
   nouveauté.
   Résultat attendu : notification discrète dans la section silencieuse, pas d'icône
   dans la barre d'état, titre « Dernière synchro » avec l'heure, deux lignes
   « • Oneshots n » et « • Campagnes n ».
2. Déplier la notification.
   Résultat attendu : trois actions, « Synchroniser », « Oneshots », « Campagnes ».
3. Dans l'écran de debug, supprimer un sujet au hasard, fermer l'application, appuyer
   sur « Synchroniser » dans la notification.
   Résultat attendu : son et bandeau, la notification remonte dans la section des
   alertes, ligne « / +1 sujet » sur le bon forum.
4. Sans ouvrir l'application, appuyer de nouveau sur « Synchroniser ».
   Résultat attendu : aucun son, la mention « +1 sujet » est toujours présente.
5. Dans l'écran de debug, sélectionner un sujet, décrémenter son compteur de réponses de
   deux, fermer
   l'application, synchroniser depuis la notification.
   Résultat attendu : aucun son, mention « / +2 réponses » ajoutée, « +1 sujet »
   toujours présent.
6. Appuyer sur l'action « Campagnes ».
   Résultat attendu : l'écran de détail des Campagnes s'ouvre ; la notification
   repasse dans la section silencieuse sans aucun compteur.
7. Appuyer sur retour.
   Résultat attendu : l'écran d'accueil s'affiche.
8. Activer le mode avion, fermer l'application, synchroniser depuis la notification.
   Résultat attendu : son et bandeau, titre « Échec de synchro ».
9. Synchroniser de nouveau, toujours en mode avion.
   Résultat attendu : aucun nouveau son, titre inchangé.
10. Désactiver le mode avion et synchroniser.
    Résultat attendu : retour à « Dernière synchro » dans la section silencieuse.
11. Désactiver la mise à l'heure automatique du téléphone et avancer l'horloge de
    61 minutes.
    Résultat attendu : dans les minutes qui suivent, son et bandeau, titre « Pas de
    synchro depuis 1 h ». Remettre ensuite l'heure automatique.
12. Vérifier l'alarme programmée :
    `adb shell dumpsys alarm | findstr jdrvirtuel`
    Résultat attendu : une alarme de l'application est présente.
13. Ouvrir les réglages Android de l'application, section notifications.
    Résultat attendu : trois catégories « Surveillance : … », l'ancienne catégorie
    « Statut de la surveillance » a disparu.
14. Dans les réglages de l'application, désactiver l'interrupteur « Notifications ».
    Résultat attendu : la notification disparaît, et
    `adb shell dumpsys alarm | findstr jdrvirtuel` ne montre plus d'alarme.
15. Réactiver l'interrupteur.
    Résultat attendu : la notification revient immédiatement.
16. Vérifier le journal des notifications dans les réglages.
    Résultat attendu : les nouveaux sujets et réponses des étapes 3 et 5 y figurent,
    aucune entrée ne concerne les changements de niveau de la notification.

## Travail attendu de Gemini

1. Créer `StatusLevelResolver` et ses tests en premier, et les faire passer avant
   toute autre modification.
2. Réécrire la construction de la notification dans `StatusNotifier` selon ce
   document, sans changer la signature de `update()`.
3. Ne pas modifier `SystemNewContentNotifier`, `SyncWorker` ni
   `SyncActionReceiver`.
4. Ne pas créer de receveur de démarrage ni de service de premier plan.
5. Ne pas coder en dur les identifiants 15 et 16 dans `StatusNotifier` : parcourir
   la liste des forums triée par identifiant. Le code actuel le fait à plusieurs
   endroits, ces occurrences doivent disparaître.
6. Ne rien inscrire au journal des notifications depuis `StatusNotifier`.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 15 est validé. Lis `specs/00_SPECIFICATIONS_GENERALES.md` puis
> `specs/MODULE_16_NOTIFICATION_PRIORITE_ET_FORMAT.md` et implémente uniquement le
> module 16. Commence par `StatusLevelResolver` et ses tests. Points critiques : trois
> canaux dont l'importance ne change jamais, republication avec `cancel()` préalable
> à chaque changement de canal, `setSilent(true)` hors des cas d'alerte définis, et
> guillemets obligatoires autour de la chaîne `notification_status_separator`. Ne
> modifie ni `SystemNewContentNotifier`, ni `SyncWorker`, ni `SyncActionReceiver`.
> Respecte la liste des fichiers autorisés et termine par le compte rendu demandé.
