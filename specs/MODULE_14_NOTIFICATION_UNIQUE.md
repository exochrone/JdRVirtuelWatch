# MODULE 14 : Notification unique

Statut : à faire
Prérequis : module 13 validé

## Objectif

Remplacer l'ensemble des notifications de l'application par une **notification
unique**, permanente, dont le contenu change selon ce que la dernière synchronisation
a détecté.

L'application n'empile plus une notification par sujet : une seule ligne dans la barre
de notification suffit à savoir si la surveillance fonctionne et ce qu'elle a trouvé.

Ce module remplace le comportement défini au module 08 et étend celui du module 13.

## Ce qui change

| Avant | Après |
|---|---|
| Une notification par nouveau sujet | Une ligne dans la notification unique |
| Une notification par nouvelle réponse | Une ligne dans la notification unique |
| Une notification de synthèse par forum | Supprimée |
| Une notification de statut permanente | Conservée, enrichie |
| Trois canaux : `new_topics`, `new_replies`, `verification` | Supprimés au profit du seul canal `status` |

Les classes `AppNotifier` et `SystemNewContentNotifier` du module 08 ne produisent plus
de notifications. Le lien profond par sujet, `jdrvirtuel://topic/{id}`, devient sans
objet.

### Conséquence assumée

Avec une notification unique, l'appui ouvre l'écran d'accueil et non le sujet
concerné. Il n'est plus possible d'aller directement d'une notification au sujet dans
le navigateur. C'est le prix de la simplification, accepté explicitement.

## Sonorisation

Le canal `status` passe de `IMPORTANCE_MIN` à `IMPORTANCE_DEFAULT`.

Sans cela, une notification unique ne pourrait plus alerter de rien : ni son, ni
vibration, ni bandeau, et il faudrait ouvrir la barre de notification pour découvrir
les nouveautés.

La distinction se fait par `setOnlyAlertOnce` :

| Situation | Valeur | Effet |
|---|---|---|
| Mise à jour de routine, aucune nouveauté | `true` | Mise à jour silencieuse, aucun bandeau |
| La synchronisation a détecté au moins une nouveauté | `false` | Son, vibration et bandeau |
| Passage en état d'échec ou en vérification requise | `false` | Son et bandeau |

Une mise à jour toutes les quinze minutes reste donc parfaitement silencieuse, et
seules les vraies nouveautés se manifestent.

## Contenu de la notification

### Titre

Toujours : `Dernière synchro HH:mm`, où l'heure est celle de la plus récente des deux
valeurs `lastSyncAt` des forums surveillés.

Le titre « Surveillance active » du module 13 est remplacé par ce libellé.

Cas particuliers :

| Situation | Titre |
|---|---|
| Au moins une synchronisation réussie | « Dernière synchro 16:21 » |
| Aucune synchronisation réussie | « Aucune synchronisation effectuée » |
| Dernier succès de plus de 24 h | « Dernière synchro 09/08 à 16:21 » |
| Dernière tentative en échec sur les deux forums | « Échec de synchronisation » |
| Vérification Cloudflare requise | « Vérification requise » |

### Texte de contenu, état de repos

Lorsque la dernière synchronisation n'a détecté **aucune** nouveauté :

```
Oneshots (3) - Campagnes (2)
```

Les nombres entre parenthèses sont les **sujets non masqués** de chaque forum, c'est
à dire les mêmes valeurs que le compteur `x` de l'écran d'accueil.

Les deux forums sont toujours présents dans cet état, sur une seule ligne.

### Texte de contenu, état de nouveauté

Lorsque la dernière synchronisation a détecté des nouveautés, le texte devient une
liste de lignes, dans cet ordre :

**Lignes de nouveaux sujets**, une par forum concerné, conditionnelles :

```
Oneshots (3) : Nouvelle aventure D&D
Campagnes (2) : 2 nouveaux sujets
```

- Le nom du forum, suivi entre parenthèses de son nombre de sujets non masqués.
- Puis, s'il n'y a qu'un seul nouveau sujet, son titre. S'il y en a plusieurs,
  « Z nouveaux sujets ».
- Un forum sans nouveau sujet n'a pas de ligne.

**Lignes de nouvelles réponses**, une par sujet surveillé concerné :

```
3 réponses à Recrutement Vermines 2047
1 réponse à Friponnes RPG
```

- Le nombre de nouvelles réponses détectées sur ce sujet, accordé au pluriel.
- Puis le titre du sujet.

Exemple complet :

```
Dernière synchro 16:21
Oneshots (3) : Nouvelle aventure D&D
Campagnes (2) : 2 nouveaux sujets
3 réponses à Recrutement Vermines 2047
```

### Conséquence de la conditionnalité

Un forum sans nouveauté n'apparaît pas dans l'état de nouveauté. Son compteur de
sujets non masqués n'est donc pas visible à ce moment. C'est un choix assumé :
l'information de volume est secondaire lorsqu'il y a du neuf à signaler, et elle
revient à la synchronisation suivante.

### Affichage replié et déplié

Une notification repliée n'affiche qu'une seule ligne de texte. Pour que les lignes
suivantes soient visibles une fois la notification dépliée, le style
`NotificationCompat.BigTextStyle` est appliqué, avec les lignes jointes par des
retours à la ligne.

La première ligne est donc celle qui compte le plus : c'est la seule visible sans
déplier.

### Limite du nombre de lignes

Au delà de **cinq lignes** au total, les lignes excédentaires sont remplacées par une
dernière ligne « et N autres ». Une notification interminable serait illisible et
tronquée par le système.

L'ordre de priorité pour cette troncature est celui de la liste : nouveaux sujets
d'abord, réponses ensuite.

## Retour à l'état de repos

La notification revient à l'affichage « Oneshots (3) - Campagnes (2) » à la
**première synchronisation suivante n'ayant détecté aucune nouveauté**.

Elle ne revient pas à l'ouverture de l'application, ni à la lecture des sujets
concernés, ni au bout d'un certain temps. Ce choix est délibéré : les nouveautés
restent visibles tant qu'une nouvelle synchronisation n'a pas eu lieu, même si
l'utilisateur a consulté l'application entre temps.

## Persistance du contenu

`StatusNotifier.update()` est appelée depuis plusieurs endroits, dont certains ne
connaissent pas le résultat de la dernière synchronisation, par exemple le démarrage
de l'application.

Le contenu de la dernière synchronisation est donc **persisté** dans
`AppPreferences`, sous la clé `last_sync_highlights`, sérialisé en JSON :

```json
{
  "newTopicsByForum": { "15": ["Nouvelle aventure D&D"], "16": ["Titre A", "Titre B"] },
  "newRepliesByTopic": [ { "title": "Recrutement Vermines 2047", "count": 3 } ]
}
```

Cette structure est écrite à chaque fin de synchronisation, y compris vide lorsqu'il
n'y a rien à signaler. `update()` la lit pour reconstruire le texte, ce qui garantit
que la notification affiche la même chose après un redémarrage de l'application.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `StatusNotifier` | `notification` | Construction de la notification unique, modifié |
| `SyncHighlights` | `domain.model` | Contenu de la dernière synchronisation, `@Serializable` |
| `NotificationChannels` | `notification` | Un seul canal conservé, modifié |
| `AppNotifier` | `notification` | Vidé de ses émissions, conservé si d'autres usages subsistent |
| `SystemNewContentNotifier` | `notification` | N'émet plus, enregistre les nouveautés dans `AppPreferences` |

`SystemNewContentNotifier` change de rôle : au lieu d'émettre des notifications, il
reçoit les listes `newTopics` et `newReplies` du module 04 et les écrit dans
`last_sync_highlights`. Le contrat `NewContentNotifier` reste inchangé, ce qui évite
de toucher à `SyncForumUseCase`.

`StatusNotifier.update()` accepte un paramètre optionnel indiquant si l'appel fait
suite à une synchronisation ayant détecté des nouveautés, afin de décider la valeur de
`setOnlyAlertOnce`.

## Journal des notifications

Le journal du module 10 continue d'enregistrer les **événements** détectés, nouveaux
sujets et nouvelles réponses, même s'ils ne donnent plus lieu à une notification
distincte. C'est désormais le seul endroit où retrouver l'historique détaillé.

Les mises à jour de la notification elle-même ne sont toujours pas journalisées.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Aucune nouveauté depuis l'installation | « Oneshots (0) - Campagnes (0) » |
| Nouveautés sur un seul forum | Une seule ligne de forum, l'autre n'apparaît pas |
| Un seul nouveau sujet | Son titre s'affiche en entier |
| Titre de sujet très long | Tronqué par le système, aucune mise en forme particulière |
| Plus de cinq lignes | Les cinq premières, puis « et N autres » |
| Réponses sur un sujet non surveillé | Rien, ces réponses ne sont pas détectées |
| Amorçage initial | Aucune nouveauté signalée, état de repos |
| Synchronisation en échec | Titre d'échec, texte conservant le dernier contenu connu |
| Permission de notification refusée | Aucune notification, aucun plantage |
| Interrupteur de statut désactivé | Aucune notification du tout, y compris pour les nouveautés |
| Redémarrage de l'application | La notification retrouve le contenu de la dernière synchronisation |

Le cas de l'interrupteur mérite attention : le désactiver supprime désormais **toute**
notification, y compris les alertes de nouveautés. Le libellé du réglage doit en
avertir l'utilisateur.

## Réglage

L'interrupteur « Notification permanente de statut » du module 13 est renommé
« Notifications », et sa phrase explicative devient : affiche une notification
permanente indiquant l'état de la surveillance et les nouveautés détectées. La
désactiver supprime toute alerte.

## Fichiers autorisés

```
app/src/main/AndroidManifest.xml
app/src/main/java/com/jdrvirtuel/watcher/notification/StatusNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/NotificationChannels.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/NotificationIds.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/AppNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/notification/SystemNewContentNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/model/SyncHighlights.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/prefs/AppPreferences.kt
app/src/main/java/com/jdrvirtuel/watcher/work/SyncWorker.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/settings/SettingsViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/MainActivity.kt
app/src/main/res/values/strings.xml
```

`MainActivity` est modifiée uniquement pour retirer le traitement du lien profond par
sujet, devenu sans objet.

`SyncForumUseCase` n'est pas modifié : le contrat `NewContentNotifier` reste le même.

Aucune dépendance nouvelle. Aucune modification du schéma Room.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
gradlew lintDebug
```

### Scénario manuel

1. Synchroniser sans qu'aucune nouveauté n'existe.
   Attendu : titre « Dernière synchro HH:mm », texte « Oneshots (X) - Campagnes (Y) »,
   les nombres correspondant aux sujets non masqués de l'écran d'accueil. Aucun son.
2. Masquer trois sujets d'un forum, resynchroniser.
   Attendu : le compteur de ce forum a diminué de trois.
3. Depuis l'écran de debug, supprimer un sujet d'un forum, noter son titre,
   resynchroniser.
   Attendu : son, vibration et bandeau. Le texte affiche « Oneshots (X) : » suivi du
   titre noté.
4. Supprimer trois sujets du même forum, resynchroniser.
   Attendu : le texte affiche « Oneshots (X) : 3 nouveaux sujets ».
5. Supprimer des sujets dans les deux forums, resynchroniser.
   Attendu : deux lignes, une par forum. Repliée, la notification n'affiche que la
   première. Dépliée, les deux.
6. Mettre un sujet sous surveillance, décrémenter son compteur de réponses de trois,
   resynchroniser.
   Attendu : une ligne « 3 réponses à » suivie du titre du sujet.
7. Décrémenter d'une seule réponse un autre sujet surveillé, resynchroniser.
   Attendu : une ligne « 1 réponse à », au singulier.
8. Provoquer plus de cinq nouveautés simultanées.
   Attendu : cinq lignes, puis « et N autres ».
9. Resynchroniser sans provoquer de nouveauté.
   Attendu : retour à « Oneshots (X) - Campagnes (Y) », et aucun son.
10. Provoquer une nouveauté, puis ouvrir l'application et consulter les sujets
    concernés sans resynchroniser.
    Attendu : la notification conserve son contenu de nouveauté.
11. Appuyer sur la notification.
    Attendu : l'écran d'accueil s'affiche, la notification reste en place.
12. Appuyer sur « Synchroniser ».
    Attendu : l'heure du titre est mise à jour quelques secondes plus tard.
13. Fermer complètement l'application, provoquer une nouveauté, attendre une
    synchronisation automatique.
    Attendu : la notification s'actualise et alerte, application fermée.
14. Redémarrer l'application après une synchronisation avec nouveautés.
    Attendu : la notification affiche toujours le même contenu.
15. Activer le mode avion et synchroniser.
    Attendu : titre « Échec de synchronisation », le texte reste celui du dernier
    contenu connu.
16. Désactiver l'interrupteur dans les réglages.
    Attendu : plus aucune notification, y compris lorsqu'une nouveauté survient.
17. Vérifier le journal des notifications dans les réglages.
    Attendu : les nouveautés y figurent toujours, malgré l'absence de notifications
    distinctes.

## Travail attendu de Gemini

Points d'attention, par ordre d'importance :

1. Le canal `status` passe en `IMPORTANCE_DEFAULT`, et `setOnlyAlertOnce` vaut `true`
   pour les mises à jour de routine et `false` uniquement lorsqu'il y a des
   nouveautés. Sans cette distinction, soit l'application sonne tous les quarts
   d'heure, soit elle n'alerte jamais.
2. Le contenu de la dernière synchronisation doit être persisté dans
   `AppPreferences`, faute de quoi la notification se viderait au redémarrage de
   l'application.
3. `SystemNewContentNotifier` change de rôle mais conserve son contrat, afin que
   `SyncForumUseCase` reste intact.
4. Les anciennes notifications par sujet et leurs canaux sont supprimés, mais le
   journal des notifications continue d'enregistrer les événements.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 13 est validé. Lis `specs/00_SPECIFICATIONS_GENERALES.md` puis
> `specs/MODULE_14_NOTIFICATION_UNIQUE.md` et implémente uniquement le module 14.
> L'application ne doit plus émettre qu'une seule notification, dont le contenu change
> selon ce que la synchronisation a détecté. Points critiques : le canal passe en
> `IMPORTANCE_DEFAULT` avec `setOnlyAlertOnce` à `true` en routine et `false` sur
> nouveauté ; le contenu de la dernière synchronisation est persisté dans
> `AppPreferences` ; `SyncForumUseCase` n'est pas modifié. Respecte la liste des
> fichiers autorisés et termine par le compte rendu demandé.
