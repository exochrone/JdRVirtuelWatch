# MODULE 04 : Synchronisation

Statut : à faire
Prérequis : modules 01, 02 et 03 validés

## Objectif

Assembler les trois briques précédentes : récupérer le HTML d'un forum, l'analyser,
comparer le résultat avec la base locale, en déduire les nouveautés, et mettre la base
à jour sans détruire les choix de l'utilisateur.

C'est le cœur métier de l'application.

Ce module ne fait pas : aucune notification n'est émise, aucune tâche périodique n'est
programmée. Il expose seulement le résultat.

## Fonctionnalités

- Synchroniser un forum donné, ou les deux.
- Détecter les nouveaux sujets.
- Détecter les nouvelles réponses sur les sujets sous surveillance.
- Préserver les états `isHidden`, `isWatched` et `isRead`.
- Amorcer silencieusement la base au premier passage.
- Purger les sujets trop anciens.
- Enregistrer l'issue de la synchronisation sur le forum concerné.

## Règles métier

### Détection d'un nouveau sujet

Un sujet est nouveau si son identifiant est absent de la base. La comparaison porte
sur la base complète, pas sur le dernier chargement : un sujet ancien qui remonte en
première page grâce à une réponse tardive n'est donc pas considéré comme nouveau.

Un nouveau sujet est signalé quel que soit son état, y compris s'il porte déjà
l'icône « Complet ». Il ne peut être ni masqué ni surveillé puisque l'utilisateur n'a
encore rien décidé le concernant.

Les sujets épinglés n'atteignent jamais ce module : ils ont été écartés par le
module 03.

### Détection d'une nouvelle réponse

Une nouvelle réponse est signalée si toutes les conditions suivantes sont réunies :

- le sujet existe déjà en base ;
- son `replyCount` analysé est strictement supérieur au `replyCount` stocké ;
- son `isWatched` est vrai.

L'état « Complet » n'entre pas en compte : un sujet complet sous surveillance
continue de produire des signalements.

Un sujet masqué a nécessairement `isWatched` à faux, il ne produit donc jamais de
signalement.

### Amorçage

Si le forum a `isBootstrapped` à faux, la synchronisation remplit la base mais ne
signale aucune nouveauté. À la fin de l'opération, `isBootstrapped` passe à vrai.

Ce mécanisme évite de recevoir une vingtaine de notifications à l'installation.

### Préservation des états utilisateur

Lors de la mise à jour d'un sujet existant, seuls les champs issus de la page web sont
écrasés : `title`, `url`, `author`, `createdAt`, `replyCount`, `lastPostAuthor`,
`lastPostAt`, `isFull`, `lastSeenAt`.

Les champs `isHidden`, `isWatched` et `firstSeenAt` sont conservés tels quels.

Le champ `isRead` est repassé à faux lorsqu'une nouvelle réponse est signalée sur un
sujet sous surveillance. Il est conservé dans tous les autres cas.

### Sujets disparus de la page

Un sujet présent en base mais absent de la page n'est pas supprimé immédiatement : son
`lastSeenAt` n'est simplement pas mis à jour. Il reste consultable dans l'écran de
détail.

### Purge

À la fin de chaque synchronisation réussie, les sujets dont `lastSeenAt` est antérieur
à 30 jours et dont `isWatched` est faux sont supprimés.

Un sujet sous surveillance n'est jamais purgé.

### Issue de la synchronisation

À la fin de l'opération, le forum est mis à jour :

- succès : `lastSyncAt` prend l'instant courant, `lastSyncSuccess` passe à vrai,
  `lastSyncError` est effacé ;
- échec ou vérification requise : `lastSyncSuccess` passe à faux, `lastSyncError`
  reçoit un libellé, et `lastSyncAt` n'est pas modifié afin de conserver la date du
  dernier succès réel.

En cas d'échec, aucune donnée de sujet n'est modifiée. Une panne réseau ne doit jamais
vider la liste.

## Architecture

### Classes à créer

| Classe | Package | Rôle |
|---|---|---|
| `SyncOutcome` | `domain.model` | Issue d'une synchronisation |
| `NewContentNotifier` | `domain.repository` | Interface de signalement |
| `NoOpNewContentNotifier` | `data.repository` | Implémentation neutre provisoire |
| `SyncForumUseCase` | `domain.usecase` | Synchronisation d'un forum |
| `SyncAllForumsUseCase` | `domain.usecase` | Synchronisation des deux forums |
| `SyncModule` | `core.di` | Fourniture Hilt |

### Modèles

```kotlin
data class SyncOutcome(
    val forumId: Int,
    val status: SyncStatus,
    val newTopics: List<Topic> = emptyList(),
    val newReplies: List<Topic> = emptyList(),
    val parsedCount: Int = 0,
    val insertedCount: Int = 0,
    val updatedCount: Int = 0,
    val purgedCount: Int = 0,
    val errorMessage: String? = null
)

enum class SyncStatus { SUCCESS, CHALLENGE_REQUIRED, ERROR }
```

### Interface de signalement

```kotlin
interface NewContentNotifier {
    suspend fun notifyNewTopics(forum: Forum, topics: List<Topic>)
    suspend fun notifyNewReplies(forum: Forum, topics: List<Topic>)
}
```

Ce module fournit `NoOpNewContentNotifier`, qui ne fait rien. Le module 08 remplacera
la liaison Hilt par l'implémentation réelle. Cette indirection permet de développer et
de valider la synchronisation sans dépendre des notifications.

### Déroulement de SyncForumUseCase

```
suspend operator fun invoke(forumId: Int): SyncOutcome
```

1. Charger le forum. S'il est introuvable, retourner `ERROR`.
2. Appeler `forumPageSource.fetchHtml(forum.url)`.
   - `ChallengeRequired` : mettre à jour l'issue du forum, retourner
     `CHALLENGE_REQUIRED`, ne rien modifier d'autre.
   - `Error` : mettre à jour l'issue du forum, retourner `ERROR`, ne rien modifier
     d'autre.
3. Analyser le HTML avec `TopicListParser`. Si la liste est vide alors que le HTML
   n'est pas vide, retourner `ERROR` avec un message signalant une structure
   inattendue. Ce garde-fou évite qu'un changement de thème du forum ne vide la base.
4. Charger les sujets existants du forum et les indexer par identifiant.
5. Pour chaque sujet analysé :
   - absent de la base : construire un `Topic` avec `firstSeenAt` et `lastSeenAt` à
     l'instant courant, `isHidden` faux, `isWatched` faux, `isRead` faux, et
     l'ajouter à la liste des insertions. S'ajoute à `newTopics` si le forum est
     amorcé.
   - présent : fusionner selon les règles de préservation. Si `replyCount` a augmenté
     et que `isWatched` est vrai, ajouter à `newReplies` et remettre `isRead` à faux.
6. Écrire toutes les modifications en une seule fois via `upsertAll`.
7. Si le forum n'était pas amorcé, le marquer amorcé.
8. Purger les sujets périmés.
9. Mettre à jour l'issue du forum.
10. Appeler `newContentNotifier` avec les deux listes, si elles ne sont pas vides.
11. Retourner le `SyncOutcome`.

L'ensemble s'exécute sur `Dispatchers.IO`, à l'exception de l'appel à
`fetchHtml`, qui gère lui même son basculement sur le thread principal.

### SyncAllForumsUseCase

Synchronise les forums l'un après l'autre, jamais en parallèle : deux WebView
simultanées augmentent le risque de déclencher un challenge. Retourne la liste des
`SyncOutcome`.

## Écrans

### Écran de debug, extension

Une section « Synchronisation » est ajoutée :

- deux boutons, « Synchroniser le forum 15 » et « Synchroniser le forum 16 », plus un
  bouton « Tout synchroniser » ;
- un indicateur de chargement ;
- le compte rendu de la dernière synchronisation : issue, nombre de sujets analysés,
  insérés, mis à jour, purgés, nombre de nouveaux sujets détectés, nombre de nouvelles
  réponses détectées ;
- la liste des titres détectés comme nouveaux ;
- deux outils de test :
  - « Supprimer un sujet au hasard », qui retire un sujet de la base afin qu'il soit
    redétecté comme nouveau à la synchronisation suivante ;
  - « Décrémenter le compteur de réponses », qui choisit un sujet sous surveillance et
    diminue son `replyCount` de 1, afin de provoquer une nouvelle réponse artificielle ;
  - « Réinitialiser l'amorçage », qui remet `isBootstrapped` à faux sur les deux
    forums.

Ces trois outils sont indispensables pour valider les modules 04 et 08 sans attendre
qu'un meneur de jeu publie une annonce.

## Comportement

- Une synchronisation en cours désactive les boutons.
- Quitter l'écran pendant une synchronisation ne l'interrompt pas : elle s'exécute
  dans le `viewModelScope`, et sera portée par un Worker à partir du module 07.
- Deux synchronisations du même forum ne peuvent pas se chevaucher, un verrou par
  `Mutex` est posé dans le cas d'usage.

## Composants UI

`Button`, `CircularProgressIndicator`, `Card`, `LazyColumn`, `Text`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Première synchronisation | Base remplie, `newTopics` vide, forum marqué amorcé |
| Deuxième synchronisation sans changement | Aucune nouveauté, uniquement des mises à jour |
| Sujet supprimé puis resynchronisé | Détecté comme nouveau |
| Sujet masqué avec une réponse de plus | Aucune nouveauté signalée |
| Sujet surveillé avec une réponse de plus | Nouvelle réponse signalée, `isRead` repassé à faux |
| Sujet surveillé et complet avec une réponse de plus | Nouvelle réponse signalée |
| Panne réseau | `ERROR`, base inchangée, date du dernier succès conservée |
| Challenge Cloudflare | `CHALLENGE_REQUIRED`, base inchangée |
| Page analysée sans aucun sujet | `ERROR`, base inchangée |
| Sujet renommé sur le forum | Titre mis à jour, aucune nouveauté signalée |
| Sujet passé en « Complet » | `isFull` mis à jour, aucune nouveauté signalée |

## Fichiers autorisés

```
app/src/main/java/com/jdrvirtuel/watcher/domain/model/SyncOutcome.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/repository/NewContentNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/usecase/SyncForumUseCase.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/usecase/SyncAllForumsUseCase.kt
app/src/main/java/com/jdrvirtuel/watcher/data/repository/NoOpNewContentNotifier.kt
app/src/main/java/com/jdrvirtuel/watcher/core/di/SyncModule.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugContract.kt
app/src/main/res/values/strings.xml
```

Ajouts autorisés dans `TopicDao` et `TopicRepository` uniquement si une requête
manque : dans ce cas, Gemini le signale explicitement dans son compte rendu.

Aucune dépendance nouvelle.

## Contrat exposé aux modules suivants

- `SyncForumUseCase` et `SyncAllForumsUseCase`, utilisés par les modules 05, 06 et 07.
- `SyncOutcome` et `SyncStatus`.
- `NewContentNotifier`, dont le module 08 fournira l'implémentation réelle.

## Tests

Aucun test automatisé exigé. La logique de fusion est vérifiée par l'écran de debug,
qui expose tous les compteurs nécessaires.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
gradlew testDebugUnitTest
```

Les tests du module 03 doivent toujours passer.

### Scénario manuel

1. Ouvrir l'écran de debug, appuyer sur « Vider les sujets », puis sur
   « Réinitialiser l'amorçage ».
2. Appuyer sur « Synchroniser le forum 15 ».
   Résultat attendu : issue « Succès », un nombre de sujets insérés cohérent avec la
   page réelle, zéro nouveau sujet signalé puisque le forum s'amorce.
3. Vérifier la section Sujets.
   Résultat attendu : la liste correspond à la page web, aucun sujet épinglé, les
   sujets complets portent bien `isFull`.
4. Appuyer de nouveau sur « Synchroniser le forum 15 ».
   Résultat attendu : issue « Succès », zéro insertion, des mises à jour, zéro
   nouveauté.
5. Appuyer sur « Supprimer un sujet au hasard », noter son titre, puis resynchroniser.
   Résultat attendu : un nouveau sujet signalé, portant ce titre.
6. Marquer un sujet comme surveillé depuis la base, à l'aide du bouton prévu dans
   l'écran de debug, puis appuyer sur « Décrémenter le compteur de réponses » et
   resynchroniser.
   Résultat attendu : une nouvelle réponse signalée, et le sujet repasse en non lu.
7. Masquer un sujet, décrémenter son compteur de réponses, resynchroniser.
   Résultat attendu : aucune nouveauté signalée.
8. Activer le mode avion et synchroniser.
   Résultat attendu : issue « Erreur », la liste des sujets reste intacte, la date du
   dernier succès est conservée.
9. Appuyer sur « Tout synchroniser ».
   Résultat attendu : les deux forums sont traités l'un après l'autre, tous deux en
   succès.

## Travail attendu de Gemini

Créer uniquement les cas d'usage, les modèles et l'extension de l'écran de debug. Ne
pas écrire de notification, ne pas créer de Worker, ne pas toucher aux modules 02 et
03.

Une attention particulière est attendue sur la préservation des états utilisateur,
qui est la règle la plus facile à casser.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Les modules 01, 02 et 03 sont validés. Lis `00_SPECIFICATIONS_GENERALES.md` puis
> `MODULE_04_SYNCHRONISATION.md` et implémente uniquement le module 04. Les états
> `isHidden`, `isWatched` et `firstSeenAt` ne doivent jamais être écrasés par une
> synchronisation. Aucune notification n'est émise dans ce module. Respecte la liste
> des fichiers autorisés et termine par le compte rendu demandé.
