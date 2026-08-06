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
Android n'en garantit pas la ponctualité : sous Doze, l'exécution peut être différée.
C'est un comportement normal du système, à ne pas contourner par une alarme exacte ou
un service en avant-plan.

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
| `SyncScheduler` | `work` | Programmation et déclenchement |
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

`schedulePeriodicSync` est appelée dans `JdrVirtuelWatcherApp.onCreate()`.

## Écrans

### Écran de debug, extension

Une section « Tâche de fond » est ajoutée :

- l'état de la tâche périodique, observé via
  `workManager.getWorkInfosForUniqueWorkLiveData("periodic_sync")` converti en `Flow` :
  `ENQUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` ;
- la date de la dernière exécution, déduite des dates de synchronisation des forums ;
- un bouton « Déclencher maintenant » qui appelle `triggerImmediateSync` ;
- un bouton « Reprogrammer la tâche », qui annule puis reprogramme, utile en cas de
  doute.

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
| Économiseur de batterie actif | L'exécution peut être suspendue, comportement système |

## Fichiers autorisés

```
gradle/libs.versions.toml                    ajout de WorkManager et hilt-work
app/build.gradle.kts                         ajout des dépendances uniquement
app/src/main/AndroidManifest.xml             désactivation de l'initialiseur par défaut
app/src/main/java/com/jdrvirtuel/watcher/work/SyncWorker.kt
app/src/main/java/com/jdrvirtuel/watcher/work/SyncScheduler.kt
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
3. Forcer l'exécution depuis un terminal, application fermée :
   ```
   adb shell cmd jobscheduler run -f com.jdrvirtuel.watcher 0
   ```
   Si l'identifiant de tâche 0 ne correspond pas, le retrouver avec :
   ```
   adb shell dumpsys jobscheduler | findstr com.jdrvirtuel.watcher
   ```
   Résultat attendu : au retour dans l'application, les dates de synchronisation des
   deux forums ont été mises à jour.
4. Activer le mode avion et déclencher la tâche.
   Résultat attendu : la tâche reste en attente de la contrainte réseau, elle ne
   s'exécute pas.
5. Désactiver le mode avion.
   Résultat attendu : la tâche s'exécute d'elle même peu après.
6. Redémarrer l'appareil, puis rouvrir l'application sans rien faire d'autre.
   Résultat attendu : la tâche périodique est toujours à l'état `ENQUEUED`.
7. Laisser l'application fermée une heure, puis la rouvrir.
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
