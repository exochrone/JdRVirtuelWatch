# MODULE 07 : Rafraîchissement automatique

Statut : à faire
Prérequis : module 04 validé

## Objectif

Exécuter la synchronisation des deux forums toutes les 15 minutes en arrière-plan,
même application fermée.

Ce module ne fait pas : aucune notification n'est émise, c'est l'objet du module 08.

## Fonctionnalités

- Programmer une tâche périodique dès le premier lancement de l'application.
- Synchroniser les deux forums à chaque exécution.
- Ne s'exécuter que si le réseau est disponible.
- Ne pas reprogrammer la tâche à chaque lancement si elle existe déjà.
- Permettre le déclenchement manuel de la tâche depuis l'écran de debug, pour la
  vérification.

## Contraintes techniques

### Périodicité

15 minutes est la valeur minimale acceptée par WorkManager pour une tâche périodique.
Toute valeur inférieure est silencieusement ramenée à 15 minutes. La périodicité de
production n'est donc pas paramétrable, et un champ de réglage serait trompeur.

Android n'en garantit pas non plus la ponctualité : sous Doze, l'exécution peut être
différée de plusieurs dizaines de minutes. C'est un comportement normal du système, à
ne pas contourner par une alarme exacte ou un service en avant-plan.

Pour valider le module sans attendre des quarts d'heure, un mode test est prévu dans
l'écran de debug. Il repose sur des tâches ponctuelles qui se replanifient, ce qui
échappe à la limite des 15 minutes tout en s'exécutant réellement en arrière-plan.

### Restrictions constructeur

Sur de nombreux appareils, en particulier Samsung, une gestion d'énergie propriétaire
peut empêcher WorkManager de s'exécuter lorsque l'application n'a pas été ouverte
depuis un certain temps.

Avant de conclure à un dysfonctionnement, vérifier sur l'appareil de test :

- `Paramètres` > `Batterie` > `Limites d'utilisation en arrière-plan` : l'application
  ne doit figurer ni dans les applications en veille, ni dans les applications en
  veille profonde ;
- informations de l'application > `Batterie` : régler sur « Sans restriction ».

Ces réglages relèvent de l'utilisateur et ne peuvent pas être imposés par le code. Le
module 10 en fait mention dans l'écran de réglages.

### Injection dans le Worker

Le Worker a besoin de `SyncAllForumsUseCase`, donc de l'injection Hilt. Cela impose
une configuration précise, qui est la principale source d'erreur de ce module :

1. Le Worker est annoté `@HiltWorker`, et son constructeur `@AssistedInject` avec
   `@Assisted context: Context` et `@Assisted params: WorkerParameters`.
2. La dépendance `androidx.hilt:hilt-work` est ajoutée, et
   `androidx.hilt:hilt-compiler` est déclarée en KSP.
3. `JdrVirtuelWatcherApp` implémente `Configuration.Provider`, injecte
   `HiltWorkerFactory` et expose une `workManagerConfiguration` qui la déclare.
4. L'initialiseur par défaut de WorkManager est désactivé dans le manifeste :

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

L'oubli de cette dernière étape produit une exception au premier déclenchement de la
tâche. Elle est obligatoire.

## Architecture

| Classe | Package | Rôle |
|---|---|---|
| `SyncWorker` | `work` | Worker de synchronisation |
| `SyncScheduler` | `work` | Programmation, déclenchement, mode test |
| `TestModeLog` | `work` | Journal des exécutions du mode test |
| `WorkModule` | `core.di` | Fourniture Hilt de `WorkManager` |

### SyncWorker

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncAllForums: SyncAllForumsUseCase
) : CoroutineWorker(context, params)
```

`doWork()` :

1. Appelle `syncAllForums()`.
2. Retourne `Result.success()` si au moins un forum a réussi.
3. Retourne `Result.retry()` si toutes les tentatives ont échoué pour cause réseau.
4. Retourne `Result.success()` si l'issue est `CHALLENGE_REQUIRED` : ce n'est pas une
   erreur technique, et une nouvelle tentative immédiate ne servirait à rien. Le
   module 09 traitera ce cas.
5. Ne lève jamais d'exception : tout est encapsulé dans un `try` avec repli sur
   `Result.retry()`.

### SyncScheduler

```kotlin
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun schedulePeriodicSync()
    fun triggerImmediateSync()
    fun cancelPeriodicSync()
}
```

- `schedulePeriodicSync` construit une `PeriodicWorkRequestBuilder<SyncWorker>(15,
  TimeUnit.MINUTES)` avec une contrainte `NetworkType.CONNECTED`, une politique de
  reprise `BackoffPolicy.EXPONENTIAL` de 30 secondes, et l'enregistre par
  `enqueueUniquePeriodicWork("periodic_sync", ExistingPeriodicWorkPolicy.KEEP, request)`.
  La politique `KEEP` évite de réinitialiser le compte à rebours à chaque lancement de
  l'application.
- `triggerImmediateSync` enregistre un `OneTimeWorkRequest` unique nommé
  `immediate_sync`, avec `ExistingWorkPolicy.KEEP`.
- `startTestMode(intervalMinutes: Int)` active le mode test, enregistre l'intervalle
  et planifie la première exécution de `test_sync`.
- `stopTestMode()` désactive le mode test et annule la tâche `test_sync` en cours.
- `scheduleNextTestRun()` est appelée par le Worker en fin d'exécution, et ne
  replanifie que si le mode test est toujours actif.

`schedulePeriodicSync` est appelée dans `JdrVirtuelWatcherApp.onCreate()`.

## Écrans

### Écran de debug, extension

Une section « Tâche de fond » est ajoutée :

- l'état de la tâche périodique, observé via
  `workManager.getWorkInfosForUniqueWorkFlow("periodic_sync")` :
  `ENQUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` ;
- la date de la dernière exécution, déduite des dates de synchronisation des forums ;
- un bouton « Déclencher maintenant » qui appelle `triggerImmediateSync` ;
- un bouton « Reprogrammer la tâche », qui annule puis reprogramme, utile en cas de
  doute.

### Journal permanent des synchronisations

Toute exécution de `SyncWorker` est journalisée, quelle que soit son origine et
quelle que soit son issue. Ce journal n'est pas un outil de développement : il est
conservé en version finale et présenté dans l'écran de réglages du module 10.

Chaque requête de travail porte une donnée d'entrée `sync_source` valant `MANUAL`,
`PERIODIC` ou `TEST`. Le Worker la lit et l'inscrit dans l'entrée de journal. Sans
cela, il est impossible de distinguer les sources, et le mode test contamine les
mesures de la tâche périodique.

Une entrée de journal contient :

| Champ | Contenu |
|---|---|
| `timestampMs` | Instant de l'exécution |
| `source` | `MANUAL`, `PERIODIC` ou `TEST` |
| `results` | Une ligne par forum : nom du forum, issue, nombre de nouveaux sujets |

Le nom du forum est celui affiché à l'utilisateur, « Oneshots » ou « Campagnes », et
non son identifiant numérique.

Le journal conserve les **50 entrées les plus récentes**, les plus anciennes étant
supprimées à chaque ajout. Il est stocké dans `AppPreferences` sous la clé `sync_log`,
sérialisé en JSON via `kotlinx.serialization`, déjà disponible depuis le module 00.
Un stockage en mémoire ne conviendrait pas, le Worker s'exécutant hors de toute
activité.

Format d'affichage attendu, défini ici et appliqué au module 10 :

```
05/08/26 - 14:52:03  ·  Automatique
   Oneshots : Succès · Nouveaux : 2
   Campagnes : Échec
```

La date suit le format `JJ/MM/AA - hh:mm:ss`. La source est traduite en clair :
« Manuelle », « Automatique », « Test ».

### Mode test accéléré

Toujours dans la section « Tâche de fond », un second bloc permet de valider le
fonctionnement en arrière-plan sans attendre 15 minutes :

- un champ numérique « Intervalle de test », en minutes, valeur par défaut 2 ;
- un bouton « Démarrer le mode test » et un bouton « Arrêter le mode test » ;
- l'état courant du mode test, actif ou inactif, et l'intervalle retenu ;
- un journal des exécutions, le plus récent en haut, chaque ligne portant l'heure au
  format HH:mm:ss, l'issue de chaque forum et le nombre de nouveautés détectées.

Le mode test repose sur un `OneTimeWorkRequest` nommé `test_sync`, planifié avec un
`setInitialDelay` correspondant à l'intervalle choisi. À la fin de son exécution, le
Worker se replanifie lui même tant que le mode test est actif. Cette mécanique échappe
à la limite des 15 minutes des tâches périodiques tout en s'exécutant dans les mêmes
conditions d'arrière-plan.

L'état d'activation et l'intervalle sont stockés dans `AppPreferences`, sous les clés
`test_mode_enabled` et `test_mode_interval_minutes`, afin de survivre à la fermeture
de l'application. Le journal est écrit dans `AppPreferences` sous forme de liste
sérialisée, faute de quoi il serait perdu à chaque exécution du Worker, celui-ci
s'exécutant hors de toute activité.

Le mode test est un outil de développement. Il est retiré au module 10, avec le reste
de l'écran de debug placé derrière `BuildConfig.DEBUG`.

## Comportement

- La tâche est programmée au premier lancement et survit au redémarrage de
  l'appareil, WorkManager s'en chargeant seul.
- Une exécution de la tâche met à jour la base, donc les écrans ouverts se
  rafraîchissent automatiquement grâce aux `Flow` de Room.
- Une synchronisation manuelle depuis l'accueil et la tâche périodique peuvent se
  chevaucher : le `Mutex` du module 04 les sérialise.

## Règles métier

- Les deux forums sont toujours synchronisés ensemble par la tâche périodique.
- Aucune synchronisation n'est lancée sans réseau.
- Aucune notification n'est produite par ce module.

## Modèle de données

Aucune modification du schéma.

## Composants UI

`Card`, `Text`, `Button`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Application fermée | La tâche s'exécute quand même |
| Appareil redémarré | La tâche est restaurée par WorkManager |
| Mode avion prolongé | La tâche attend la contrainte réseau |
| Appareil en Doze | L'exécution est différée, ce qui est normal |
| Échec réseau ponctuel | `Result.retry()` avec reprise exponentielle |
| Challenge Cloudflare | `Result.success()`, l'incident est enregistré sur le forum |
| Double programmation | `KEEP` empêche la duplication |
| Mode test actif au redémarrage de l'application | Il reste actif, l'état est persistant |
| Journal atteignant 50 entrées | Les plus anciennes sont supprimées, aucune croissance illimitée |
| Tâche périodique s'exécutant pendant le mode test | Journalisée comme `PERIODIC`, ne replanifie pas le mode test |
| Mode test et tâche périodique simultanés | Les deux tournent, le `Mutex` du module 04 les sérialise |
| Économiseur d'énergie constructeur actif | La tâche peut ne jamais s'exécuter, voir la section dédiée |
| Économiseur de batterie actif | L'exécution peut être suspendue, comportement système |

## Fichiers autorisés

```
gradle/libs.versions.toml                    ajout de WorkManager et hilt-work
app/build.gradle.kts                         ajout des dépendances uniquement
app/src/main/AndroidManifest.xml             désactivation de l'initialiseur par défaut
app/src/main/java/com/jdrvirtuel/watcher/work/SyncWorker.kt
app/src/main/java/com/jdrvirtuel/watcher/work/SyncScheduler.kt
app/src/main/java/com/jdrvirtuel/watcher/work/TestModeLog.kt
app/src/main/java/com/jdrvirtuel/watcher/work/SyncLog.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/model/SyncLogEntry.kt
app/src/main/java/com/jdrvirtuel/watcher/data/local/prefs/AppPreferences.kt  ajout des cles du mode test
app/src/main/java/com/jdrvirtuel/watcher/core/di/WorkModule.kt
app/src/main/java/com/jdrvirtuel/watcher/JdrVirtuelWatcherApp.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugContract.kt
app/src/main/res/values/strings.xml
```

Dépendances à ajouter : `androidx.work:work-runtime-ktx` version minimale 2.9.0,
`androidx.hilt:hilt-work` version minimale 1.2.0, et `androidx.hilt:hilt-compiler` en
KSP.

## Contrat exposé aux modules suivants

- `SyncLog`, dont le module 10 affiche le contenu dans l'écran de réglages.
- `SyncScheduler`, utilisé par le module 09 pour espacer les tentatives après des
  échecs répétés de vérification.
- `SyncWorker`, qui portera l'appel aux notifications réelles une fois le module 08
  en place, sans modification de son code puisque le notifieur est injecté.

## Tests

Aucun test automatisé.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
```

### Scénario manuel

1. Lancer l'application, ouvrir l'écran de debug, section Tâche de fond.
   Résultat attendu : la tâche périodique est à l'état `ENQUEUED`.
2. Vider les sujets, puis appuyer sur « Déclencher maintenant ».
   Résultat attendu : la tâche passe à `RUNNING` puis à `SUCCEEDED`, et les sujets des
   deux forums réapparaissent en base.
3. Vérifier au préalable les réglages d'économie d'énergie décrits plus haut, sans
   quoi les étapes suivantes échoueront pour une raison étrangère au code.
4. Régler l'intervalle de test sur 2 minutes et appuyer sur « Démarrer le mode test ».
   Fermer complètement l'application et poser le téléphone, écran éteint, pendant
   10 minutes.
   Résultat attendu : au retour dans l'écran de debug, le journal du mode test compte
   environ cinq exécutions, avec leurs horodatages et leurs issues. C'est la preuve
   que la synchronisation fonctionne application fermée.
5. Appuyer sur « Arrêter le mode test ».
   Résultat attendu : plus aucune ligne ne s'ajoute au journal.
6. Optionnel, si `adb` est disponible : forcer l'exécution de la tâche périodique
   depuis un terminal, application fermée :
   ```
   adb shell cmd jobscheduler run -f com.jdrvirtuel.watcher 0
   ```
   Si l'identifiant de tâche 0 ne correspond pas, le retrouver avec :
   ```
   adb shell dumpsys jobscheduler | findstr com.jdrvirtuel.watcher
   ```
   Cette étape n'est pas indispensable, le mode test couvre déjà la vérification.
7. Activer le mode avion et déclencher la tâche.
   Résultat attendu : la tâche reste en attente de la contrainte réseau, elle ne
   s'exécute pas.
8. Désactiver le mode avion.
   Résultat attendu : la tâche s'exécute d'elle même peu après.
9. Redémarrer l'appareil, puis rouvrir l'application sans rien faire d'autre.
   Résultat attendu : la tâche périodique est toujours à l'état `ENQUEUED`.
10. Laisser l'application fermée une heure, puis la rouvrir.
   Résultat attendu : les dates de dernière synchronisation montrent qu'au moins une
   exécution automatique a eu lieu entre temps. Un décalage par rapport aux 15 minutes
   théoriques est normal.

## Travail attendu de Gemini

Créer uniquement le Worker, le planificateur et l'extension de l'écran de debug. Ne
pas écrire de notification. Ne pas modifier les cas d'usage du module 04.

La configuration Hilt et WorkManager doit être complète : `@HiltWorker`,
`Configuration.Provider`, `HiltWorkerFactory`, et suppression de l'initialiseur par
défaut dans le manifeste. Vérifier ce dernier point explicitement et le mentionner
dans le compte rendu.

## Prompt de démarrage

> Le module 06 est validé. Lis `00_SPECIFICATIONS_GENERALES.md` puis
> `MODULE_07_RAFRAICHISSEMENT_AUTOMATIQUE.md` et implémente uniquement le module 07.
> N'oublie pas de désactiver l'initialiseur par défaut de WorkManager dans le
> manifeste, faute de quoi l'injection Hilt échouera à l'exécution. Aucune
> notification dans ce module. Respecte la liste des fichiers autorisés et termine par
> le compte rendu demandé.
