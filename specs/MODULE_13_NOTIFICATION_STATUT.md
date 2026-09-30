# MODULE 13 : Notification de statut permanente

Statut : à faire
Prérequis : module 12 validé

## Objectif

Afficher en permanence, dans la barre de notification, l'heure de la dernière
synchronisation réussie et l'état de la surveillance, avec deux actions directes :
synchroniser maintenant, ou ouvrir l'application.

L'objectif est de savoir d'un coup d'oeil si la surveillance fait son travail, sans
avoir à ouvrir l'application.

Ce module ne fait pas : aucun service de premier plan, aucune modification de la
logique de synchronisation, aucun receveur de démarrage.

## Choix de conception

### Heure absolue plutôt que temps écoulé

Une notification, une fois postée, conserve son texte jusqu'à ce que l'application le
remplace. Comme la synchronisation tourne toutes les quinze minutes, un libellé du
type « il y a 3 minutes » resterait faux la plupart du temps.

L'affichage retenu est donc l'**heure absolue** de la dernière synchronisation
réussie, au format `14:30`. Elle ne bouge jamais et ne devient jamais fausse. C'est la
solution qu'emploient la plupart des applications météo, pour la même raison.

Le chronomètre natif d'Android, `setUsesChronometer`, a été envisagé puis écarté : il
donne un temps écoulé exact en permanence, mais l'affichage défile seconde par
seconde, ce qui est visuellement agité pour une information consultée
occasionnellement.

### Pas de service de premier plan

Une notification ordinaire avec `setOngoing(true)` suffit, sans permission
particulière et sans consommation en arrière-plan.

À partir d'Android 14, une notification `ongoing` reste balayable par l'utilisateur.
Ce n'est pas un problème : elle est reposée à la synchronisation suivante.

### Pas de receveur de démarrage

Après un redémarrage de l'appareil, la notification est absente jusqu'à la première
synchronisation, soit un quart d'heure au maximum, ou jusqu'à la prochaine ouverture
de l'application. Un `BroadcastReceiver` sur `ACTION_BOOT_COMPLETED` comblerait cette
fenêtre, au prix d'une permission supplémentaire et d'un composant de plus, pour un
cas qui se produit rarement. Il est délibérément écarté.

## Fonctionnalités

- Une notification permanente, silencieuse, affichant l'état de la surveillance.
- Deux actions : « Synchroniser » et « Ouvrir ».
- Un réglage permettant de la désactiver.

## Contenu de la notification

| Élément | Contenu |
|---|---|
| Icône | `ic_notification`, la même que les autres notifications |
| Titre | Variable selon l'état, voir ci-dessous |
| Texte | Variable selon l'état |
| Action 1 | « Synchroniser » |
| Action 2 | « Ouvrir » |
| Appui sur le corps | Équivalent à « Ouvrir » |

### Variantes selon l'état

| Situation | Titre | Texte |
|---|---|---|
| Au moins une synchronisation réussie | « Surveillance active » | « Dernière synchronisation à 14:30 » |
| Aucune synchronisation réussie | « Surveillance active » | « Aucune synchronisation effectuée » |
| Dernière tentative en échec sur les deux forums | « Dernière synchronisation en échec » | « Dernier succès à 14:30 », ou « Aucune synchronisation effectuée » |
| Vérification Cloudflare requise | « Vérification requise » | « Appuyez sur Ouvrir pour débloquer l'accès » |

L'heure affichée est celle de la **plus récente** des deux valeurs `lastSyncAt` des
forums surveillés. Afficher deux heures distinctes surchargerait la notification.

Le format est `HH:mm`, dans le fuseau de l'appareil. Si la dernière synchronisation
réussie date de plus de vingt-quatre heures, le format devient `JJ/MM à HH:mm`, sans
quoi l'heure seule serait trompeuse.

## Comportement des actions

### Synchroniser

Déclenche une synchronisation immédiate des deux forums **sans ouvrir
l'application**.

L'action passe par un `BroadcastReceiver` qui appelle directement
`WorkManager.getInstance(context).enqueueUniqueWork(...)` avec une requête
`OneTimeWorkRequest` de `SyncWorker` portant la donnée d'entrée `sync_source` à
`MANUAL`, afin que le journal reflète l'origine réelle.

Cet appel est non bloquant et rend la main immédiatement. Le receveur n'a donc besoin
ni d'injection Hilt, ni de `goAsync()`, ni de coroutine. Il ne lit ni la base ni les
préférences.

Aucun retour visuel immédiat n'est prévu : un état « Synchronisation en cours… »
n'apparaîtrait que quelques secondes, sur un canal discret souvent replié par le
système. Le retour pour l'utilisateur est le changement d'heure quelques secondes plus
tard.

### Ouvrir

Ouvre l'écran d'accueil de l'application, via un `PendingIntent` vers
`MainActivity`. La notification n'est pas retirée, elle est permanente.

## Canal de notification

Un quatrième canal est créé, en plus des trois existants :

| Identifiant | Nom affiché | Importance |
|---|---|---|
| `status` | Statut de la surveillance | `IMPORTANCE_MIN` |

`IMPORTANCE_MIN` est indispensable : sans cela, chaque mise à jour produirait un son
et une vibration, toutes les quinze minutes. Avec cette importance, la notification
apparaît discrètement, sans bandeau ni son, et peut être reléguée en bas de la zone de
notification par le système.

L'utilisateur peut désactiver ce canal depuis les réglages Android, indépendamment du
réglage interne à l'application.

## Quand la notification est mise à jour

`StatusNotifier.update()` est appelée :

- au démarrage de l'application, depuis `JdrVirtuelWatcherApp.onCreate()` ;
- à la fin de chaque exécution de `SyncWorker`, quelle qu'en soit l'issue ;
- après une synchronisation manuelle déclenchée depuis l'interface, donc depuis
  `HomeViewModel` et `ForumDetailViewModel`.

Ce dernier point est le plus facile à oublier : les rafraîchissements déclenchés
depuis l'accueil ou l'écran de détail appellent les cas d'usage directement et ne
passent pas par `SyncWorker`. Sans cet appel, la notification resterait figée après un
tirage vers le bas.

## Réglage

Une entrée est ajoutée dans la section « Notifications » de l'écran de réglages :

- un interrupteur « Notification permanente de statut », activé par défaut ;
- une phrase explicative : affiche en continu l'heure de la dernière synchronisation,
  avec un accès rapide.

L'état est stocké dans `AppPreferences` sous la clé `status_notification_enabled`.

Désactiver l'interrupteur retire immédiatement la notification. La réactiver la repose
sans attendre la synchronisation suivante.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `StatusNotifier` | `notification` | Construction et mise à jour de la notification |
| `SyncActionReceiver` | `notification` | `BroadcastReceiver` de l'action Synchroniser |

`StatusNotifier` est un singleton Hilt exposant deux fonctions : `update()`, qui
reconstruit la notification à partir de l'état courant des forums et des préférences,
et `cancel()`.

`update()` ne fait rien, sans lever d'exception, si l'interrupteur est désactivé ou si
la permission de notification est refusée.

L'identifiant de la notification est une constante dédiée dans `NotificationIds`,
distincte de toutes les autres et hors de la plage des identifiants de sujets.

Le formatage de l'heure passe par `DateFormatter`, qui centralise déjà les formats de
date de l'application.

## Interaction avec les autres notifications

La notification de statut est indépendante des notifications de nouveaux sujets et de
nouvelles réponses. Elle ne les remplace pas et ne les regroupe pas.

Elle n'est **jamais** enregistrée dans le journal des notifications du module 10 : ce
journal recense des événements, pas les mises à jour d'un affichage permanent. L'y
inscrire toutes les quinze minutes le rendrait illisible.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Permission de notification refusée | Aucune notification, aucun plantage, l'interrupteur reste réglable |
| Canal `status` désactivé dans les réglages Android | Aucune notification, l'application n'insiste pas |
| Aucune synchronisation depuis l'installation | « Aucune synchronisation effectuée » |
| Dernier succès datant de plus de 24 h | Format `JJ/MM à HH:mm` |
| Redémarrage de l'appareil | La notification revient à la première synchronisation ou à l'ouverture de l'application |
| Notification balayée sur Android 14 et supérieur | Elle revient à la synchronisation suivante |
| Appui sur « Synchroniser » sans réseau | La synchronisation échoue, la notification passe en état d'échec |
| Appuis répétés sur « Synchroniser » | `enqueueUniqueWork` avec `KEEP` empêche les exécutions parallèles |
| Interrupteur désactivé | La notification disparaît immédiatement |
| Synchronisation manuelle depuis l'accueil | L'heure affichée est mise à jour |

## Fichiers autorisés

```
app/src/main/AndroidManifest.xml
app/src/main/java/com/jdrvirtuel/watcher/notification/StatusNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/SyncActionReceiver.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/NotificationChannels.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/NotificationIds.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/prefs/AppPreferences.kt
app/src/main/java/com/jdrvirtuel/watcher/JdrVirtuelWatcherApp.kt
app/src/main/java/com/jdrvirtuel/watcher/work/SyncWorker.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/forumdetail/ForumDetailViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsContract.kt
app/src/main/java/com/jdrvirtuel/watcher/core/util/DateFormatter.kt
app/src/main/res/values/strings.xml
```

Le manifeste est modifié uniquement pour déclarer `SyncActionReceiver`. Aucune
permission nouvelle n'est requise.

Aucune dépendance nouvelle. Aucune modification du schéma Room.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
gradlew lintDebug
```

### Scénario manuel

1. Lancer l'application après installation, avant toute synchronisation.
   Attendu : la notification apparaît, titre « Surveillance active », texte « Aucune
   synchronisation effectuée », deux actions visibles.
2. Synchroniser depuis l'accueil en tirant la liste vers le bas.
   Attendu : la notification affiche l'heure courante. C'est le point le plus facile à
   rater, ce chemin ne passant pas par le Worker.
3. Ouvrir un forum et rafraîchir depuis cet écran.
   Attendu : l'heure est de nouveau mise à jour.
4. Appuyer sur « Synchroniser » depuis la notification.
   Attendu : l'application ne s'ouvre pas, et l'heure change quelques secondes plus
   tard.
5. Vérifier le journal des synchronisations.
   Attendu : l'entrée correspondante porte la source « Manuelle ».
6. Appuyer sur « Ouvrir », puis sur le corps de la notification.
   Attendu : dans les deux cas l'écran d'accueil s'affiche, la notification reste en
   place.
7. Vérifier qu'aucune mise à jour n'a produit de son ni de vibration.
8. Activer le mode avion, appuyer sur « Synchroniser ».
   Attendu : le titre passe à « Dernière synchronisation en échec », le texte conserve
   l'heure du dernier succès.
9. Désactiver le mode avion, synchroniser.
   Attendu : retour à « Surveillance active ».
10. Ouvrir les réglages, désactiver l'interrupteur.
    Attendu : la notification disparaît immédiatement.
11. Réactiver l'interrupteur.
    Attendu : la notification revient sans attendre la synchronisation suivante.
12. Désactiver le canal « Statut de la surveillance » dans les réglages Android.
    Attendu : plus aucune notification de statut, les notifications de nouveaux sujets
    continuent de fonctionner.
13. Vérifier le journal des notifications dans les réglages.
    Attendu : aucune entrée liée à la notification de statut.

## Travail attendu de Gemini

Quatre points méritent une attention particulière :

1. L'affichage est une **heure absolue**, pas un temps écoulé ni un chronomètre.
2. Le canal `status` est en `IMPORTANCE_MIN`, sans quoi chaque mise à jour produirait
   un son toutes les quinze minutes.
3. `update()` doit être appelée depuis `HomeViewModel` et `ForumDetailViewModel`, et
   pas seulement depuis `SyncWorker`.
4. `SyncActionReceiver` reste minimal : un appel non bloquant à `enqueueUniqueWork`,
   sans injection Hilt, sans `goAsync()`, sans coroutine, sans accès à la base.

Ne pas créer de service de premier plan. Ne pas créer de receveur de démarrage. Ne pas
modifier la logique de synchronisation.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 12 est validé. Lis `specs/00_SPECIFICATIONS_GENERALES.md` puis
> `specs/MODULE_13_NOTIFICATION_STATUT.md` et implémente uniquement le module 13.
> Quatre points critiques : l'affichage est une heure absolue et non un chronomètre ;
> le canal `status` est en `IMPORTANCE_MIN` ; `StatusNotifier.update()` doit aussi
> être appelée depuis `HomeViewModel` et `ForumDetailViewModel` ; et
> `SyncActionReceiver` reste minimal, sans Hilt ni `goAsync()`. Aucun service de
> premier plan, aucun receveur de démarrage. Respecte la liste des fichiers autorisés
> et termine par le compte rendu demandé.
