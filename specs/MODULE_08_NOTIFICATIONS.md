# MODULE 08 : Notifications

Statut : à faire
Prérequis : modules 04 et 07 validés

## Objectif

Prévenir l'utilisateur des nouveautés détectées par la synchronisation, en respectant
les règles de déclenchement définies au module 04.

C'est la raison d'être de l'application.

Ce module ne fait pas : la gestion du cas Cloudflare bloquant, qui appartient au
module 09, même si le canal de notification correspondant est créé ici.

## Fonctionnalités

- Demander la permission de notification sur Android 13 et supérieur.
- Créer trois canaux de notification distincts.
- Émettre une notification par nouveau sujet.
- Émettre une notification par nouvelle réponse sur un sujet surveillé.
- Regrouper les notifications par forum.
- Ouvrir le sujet dans le navigateur à l'appui.

## Règles de déclenchement

Rappel des règles, déjà appliquées par le module 04 :

| Événement | Notification |
|---|---|
| Nouveau sujet, forum amorcé | Oui, systématiquement |
| Nouveau sujet, forum non amorcé | Non, amorçage silencieux |
| Nouvelle réponse, sujet surveillé | Oui, que le sujet soit complet ou non |
| Nouvelle réponse, sujet non surveillé | Non |
| Nouvelle réponse, sujet masqué | Non, un sujet masqué n'est jamais surveillé |
| Sujet passant en « Complet » | Non |
| Sujet épinglé | Non, jamais stocké |

Ce module ne réimplémente aucune de ces règles : il reçoit deux listes déjà filtrées
et se contente de les afficher.

## Canaux de notification

| Identifiant | Nom affiché | Importance | Usage |
|---|---|---|---|
| `new_topics` | Nouveaux sujets | `IMPORTANCE_DEFAULT` | Nouvelles propositions de parties |
| `new_replies` | Nouvelles réponses | `IMPORTANCE_DEFAULT` | Réponses sur les sujets surveillés |
| `verification` | Vérification requise | `IMPORTANCE_HIGH` | Accès au forum bloqué |

Les canaux sont créés au démarrage de l'application, dans `NotificationChannels.
create(context)`, appelé depuis `JdrVirtuelWatcherApp.onCreate()`.

Trois canaux distincts permettent à l'utilisateur de couper les notifications de
réponses tout en gardant celles des nouveaux sujets, directement depuis les réglages
Android. Aucun réglage équivalent n'est donc à créer dans l'application.

## Contenu des notifications

### Nouveau sujet

- Titre : nom du forum, par exemple « Oneshots ».
- Texte : titre du sujet.
- Style : `BigTextStyle`, pour que les titres longs soient lisibles.
- Identifiant : identifiant du sujet, afin qu'une même nouveauté ne produise jamais
  deux notifications distinctes.
- Groupe : `group_forum_15` ou `group_forum_16`.

### Nouvelle réponse

- Titre : nom du forum.
- Texte : « Nouvelle réponse : {titre du sujet} ».
- Mêmes règles d'identifiant et de groupe. L'identifiant est décalé pour ne pas
  entrer en collision avec celui d'un nouveau sujet : `topicId + 1_000_000`.

### Notification de synthèse

Une notification de synthèse par forum est émise dès que deux notifications ou plus
sont présentes dans le même groupe :

- Titre : nom du forum.
- Texte : « X nouveautés ».
- `setGroupSummary(true)`, `InboxStyle` listant les titres.
- Identifiant : `forumId` directement, valeurs 15 et 16, sans risque de collision avec
  les identifiants de sujets qui sont bien supérieurs.

## Comportement à l'appui

Un appui sur une notification :

1. ouvre le sujet dans le navigateur, via Custom Tabs ;
2. marque le sujet comme lu ;
3. retire la notification.

L'implémentation passe par un `PendingIntent` visant une destination profonde de
`MainActivity`, portant l'identifiant du sujet en paramètre. `MainActivity` traite ce
paramètre au démarrage, marque le sujet lu, puis lance le navigateur.

L'application n'affiche donc pas d'écran intermédiaire : le comportement est le même
qu'un appui sur le titre dans l'écran de détail.

Un balayage de la notification la retire sans marquer le sujet comme lu.

## Permission

Sur Android 13 et supérieur, la permission `POST_NOTIFICATIONS` est requise.

- Elle est déclarée dans le manifeste.
- Elle est demandée à la première ouverture de l'écran d'accueil, via
  `rememberLauncherForActivityResult` et `ActivityResultContracts.RequestPermission`.
- Si elle est refusée, un bandeau discret et permanent apparaît en haut de l'écran
  d'accueil : « Notifications désactivées », avec un bouton « Activer » qui ouvre les
  réglages système de l'application.
- L'application reste pleinement fonctionnelle sans la permission, seules les
  notifications manquent.

La demande n'est faite qu'une fois par installation. Si l'utilisateur refuse, elle
n'est pas répétée à chaque lancement.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `NotificationChannels` | `notification` | Création des canaux |
| `AppNotifier` | `notification` | Émission des notifications |
| `SystemNewContentNotifier` | `notification` | Implémentation de `NewContentNotifier` |
| `NotificationIds` | `notification` | Calcul des identifiants |
| `NotificationModule` | `core.di` | Liaison Hilt |
| `PermissionBanner` | `core.ui.component` | Bandeau de permission refusée |

`SystemNewContentNotifier` remplace `NoOpNewContentNotifier` dans la liaison Hilt.
`NoOpNewContentNotifier` est conservé mais n'est plus lié : il reste utile pour un
éventuel test.

Aucune modification n'est nécessaire dans `SyncForumUseCase` ni dans `SyncWorker` :
le notifieur y est déjà injecté.

## Modèle de données

Aucune modification du schéma.

## Composants UI

`Surface` pour le bandeau, `TextButton`, `Icon`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Permission refusée | Aucune notification, bandeau affiché, application fonctionnelle |
| Premier lancement | Amorçage silencieux, aucune notification |
| Dix nouveaux sujets d'un coup | Dix notifications groupées plus une synthèse |
| Un seul nouveau sujet | Une notification, pas de synthèse |
| Même sujet détecté deux fois | Une seule notification, l'identifiant est stable |
| Nouveau sujet et nouvelle réponse sur le même sujet | Impossible, un sujet nouveau n'est pas encore surveillé |
| Application fermée | Les notifications arrivent, la tâche de fond fonctionne |
| Appui sur une notification, aucun navigateur installé | Message d'erreur au lancement de l'application |
| Titre de sujet très long | `BigTextStyle` le rend lisible |
| Canal désactivé par l'utilisateur | Aucune notification sur ce canal, aucun crash |

## Fichiers autorisés

```
app/src/main/AndroidManifest.xml                        permission et intent-filter
app/src/main/java/com/jdrvirtuel/watcher/notification/NotificationChannels.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/AppNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/SystemNewContentNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/NotificationIds.kt
app/src/main/java/com/jdrvirtuel/watcher/core/di/NotificationModule.kt
app/src/main/java/com/jdrvirtuel/watcher/core/di/SyncModule.kt        liaison uniquement
app/src/main/java/com/jdrvirtuel/watcher/core/ui/component/PermissionBanner.kt
app/src/main/java/com/jdrvirtuel/watcher/JdrVirtuelWatcherApp.kt      création des canaux
app/src/main/java/com/jdrvirtuel/watcher/MainActivity.kt              traitement du lien profond
app/src/main/java/com/jdrvirtuel/watcher/feature/home/HomeScreen.kt   demande de permission et bandeau
app/src/main/res/values/strings.xml
app/src/main/res/drawable/ic_notification.xml
```

Aucune dépendance nouvelle. `androidx.core:core-ktx` fournit
`NotificationCompat`.

L'icône de notification doit être une silhouette monochrome sur fond transparent,
faute de quoi Android affichera un carré blanc.

## Contrat exposé aux modules suivants

- `AppNotifier`, dont le module 09 utilisera la méthode dédiée à la vérification.
- Le canal `verification`, créé ici mais utilisé au module 09.

## Tests

Aucun test automatisé.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
```

### Scénario manuel

1. Désinstaller puis réinstaller l'application, la lancer.
   Résultat attendu : la demande de permission de notification apparaît sur Android 13
   et supérieur.
2. Refuser la permission.
   Résultat attendu : le bandeau « Notifications désactivées » apparaît sur l'accueil,
   l'application reste utilisable.
3. Accepter la permission depuis le bouton du bandeau.
   Résultat attendu : le bandeau disparaît.
4. Synchroniser une première fois.
   Résultat attendu : aucune notification, l'amorçage est silencieux.
5. Ouvrir l'écran de debug, appuyer sur « Supprimer un sujet au hasard », noter son
   titre, puis synchroniser.
   Résultat attendu : une notification apparaît, titrée du nom du forum, avec le titre
   du sujet en corps.
6. Appuyer sur cette notification.
   Résultat attendu : le navigateur s'ouvre sur le bon sujet, la notification
   disparaît, et de retour dans l'application le sujet n'est plus en gras.
7. Supprimer trois sujets d'un coup puis synchroniser.
   Résultat attendu : trois notifications regroupées et une notification de synthèse
   annonçant trois nouveautés.
8. Mettre un sujet sous surveillance, ouvrir le debug, décrémenter son compteur de
   réponses, puis synchroniser.
   Résultat attendu : une notification « Nouvelle réponse : {titre} », et le sujet
   repasse en gras dans l'écran de détail.
9. Retirer la surveillance de ce sujet, décrémenter de nouveau, synchroniser.
   Résultat attendu : aucune notification.
10. Masquer un sujet surveillé, décrémenter, synchroniser.
    Résultat attendu : aucune notification.
11. Fermer complètement l'application, supprimer un sujet au préalable, puis forcer la
    tâche de fond par `adb`.
    Résultat attendu : la notification arrive alors que l'application est fermée.
12. Ouvrir les réglages Android de l'application, désactiver le canal « Nouvelles
    réponses », puis reproduire l'étape 8.
    Résultat attendu : aucune notification de réponse, mais les notifications de
    nouveaux sujets continuent de fonctionner.

## Travail attendu de Gemini

Créer uniquement les classes de notification, le bandeau de permission, et les
modifications strictement nécessaires dans le manifeste, l'application et
`MainActivity`.

Ne pas modifier `SyncForumUseCase` ni `SyncWorker` : la liaison Hilt suffit à activer
les notifications.

Ne pas réimplémenter les règles de déclenchement, elles appartiennent au module 04.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 07 est validé. Lis `00_SPECIFICATIONS_GENERALES.md` puis
> `MODULE_08_NOTIFICATIONS.md` et implémente uniquement le module 08. Remplace la
> liaison Hilt de `NewContentNotifier` par l'implémentation système, sans modifier
> `SyncForumUseCase` ni `SyncWorker`. Respecte la liste des fichiers autorisés et
> termine par le compte rendu demandé.
