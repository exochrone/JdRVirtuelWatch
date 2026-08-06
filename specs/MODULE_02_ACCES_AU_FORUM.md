# MODULE 02 : Accès au forum

Statut : à faire
Prérequis : module 00 validé

## Objectif

Récupérer le code HTML d'une page de forum malgré la protection Cloudflare, et
l'exposer sous une forme simple au reste de l'application.

Ce module ne fait pas : aucune analyse du HTML, aucune écriture en base, aucune
notification.

## Contrainte structurante

Le domaine `jdrvirtuel.com` renvoie une réponse `403` avec l'en tête
`Cf-Mitigated: challenge` à tout client HTTP classique, quels que soient les en têtes
envoyés. Le flux ATOM de phpBB est protégé de la même manière. Le test suivant a été
effectué et confirme le blocage :

```
curl.exe -sIL -A "Mozilla/5.0 (Linux; Android 14) ..." "https://www.jdrvirtuel.com/viewforum.php?f=15"
HTTP/1.1 403 Forbidden
Cf-Mitigated: challenge
```

En conséquence, la seule voie d'accès est une WebView, qui exécute le JavaScript du
challenge et obtient le cookie `cf_clearance`.

### Nature du challenge, constatée à la mise en oeuvre

Le challenge de ce forum est **interactif** : il exige qu'un humain coche une case de
vérification. Aucun réglage de WebView, aucun délai d'attente et aucune boucle
d'interrogation ne permet de le franchir automatiquement.

Deux conséquences structurantes :

- une WebView invisible ne peut réussir que si un cookie `cf_clearance` valide est
  déjà présent. Sans cookie, elle retourne nécessairement `ChallengeRequired` ;
- le cookie obtenu a une durée de vie nominale de plusieurs mois, mais Cloudflare
  l'invalide notamment lors d'un changement d'adresse IP, ce qui arrive plusieurs fois
  par jour sur un téléphone passant du wifi au réseau mobile.

La validation manuelle par WebView visible, spécifiée au module 09, n'est donc pas un
cas limite exceptionnel : c'est une fonction courante de l'application, à laquelle
l'utilisateur aura recours régulièrement.

Sur une installation neuve, le premier chargement échoue toujours en
`ChallengeRequired`. C'est le comportement attendu et non une régression.

Interdictions absolues pour ce module :

- ne pas utiliser OkHttp, Retrofit, `HttpURLConnection`, Ktor ni `Jsoup.connect()` ;
- ne pas tenter de reproduire le challenge en Kotlin ;
- ne pas ajouter de bibliothèque de contournement anti robot.

## Fonctionnalités

- Charger une URL du forum dans une WebView invisible.
- Attendre la résolution éventuelle du challenge Cloudflare.
- Extraire le HTML final du DOM.
- Distinguer trois issues : succès, vérification humaine requise, erreur technique.
- Conserver les cookies entre les appels et entre les lancements de l'application.
- Afficher le résultat dans l'écran de debug.

## Architecture

### Contrat exposé

```kotlin
interface ForumPageSource {
    suspend fun fetchHtml(url: String): FetchResult
}

sealed interface FetchResult {
    data class Success(val html: String) : FetchResult
    data object ChallengeRequired : FetchResult
    data class Error(val message: String) : FetchResult
}
```

L'interface est déclarée dans `domain.repository`, l'implémentation dans
`data.remote`. La liaison Hilt se fait par `@Binds` dans un module
`RemoteModule`.

### Classes à créer

| Classe | Package | Rôle |
|---|---|---|
| `ForumPageSource` | `domain.repository` | Interface |
| `FetchResult` | `domain.model` | Type de retour scellé |
| `WebViewForumPageSource` | `data.remote` | Implémentation WebView |
| `WebViewConstants` | `data.remote` | User agent, délais, seuils |
| `RemoteModule` | `core.di` | Liaison Hilt |

### Fonctionnement attendu de l'implémentation

1. Basculer sur `Dispatchers.Main`, car une WebView ne peut être instanciée que sur
   le thread principal. C'est la seule exception à la règle générale qui impose
   `Dispatchers.IO` pour les accès aux données.
2. Créer une WebView hors écran, sans l'attacher à une hiérarchie de vues.
3. Configurer :
   - `javaScriptEnabled = true` (indispensable au challenge) ;
   - `domStorageEnabled = true` ;
   - `userAgentString` fixé à une valeur Chrome mobile réaliste, constante et stable ;
   - `CookieManager.getInstance().setAcceptCookie(true)` et
     `setAcceptThirdPartyCookies(webView, true)`.
4. Envelopper le chargement dans un `suspendCancellableCoroutine`.
5. Dans `onPageFinished`, interroger le DOM de façon répétée plutôt qu'après une
   attente fixe. Toutes les 500 ms, extraire le DOM :
   ```
   evaluateJavascript("(function(){return document.documentElement.outerHTML;})();")
   ```
   La chaîne retournée est encodée en littéral JavaScript et doit être décodée avant
   usage.

   L'interrogation est planifiée par un `Handler(Looper.getMainLooper())` et non par
   `View.postDelayed`. Point critique : la WebView n'étant jamais attachée à une
   fenêtre, elle ne possède pas de `Handler` propre et `View.postDelayed` ne
   s'exécute jamais.

   Dès que le HTML contient `topictitle`, l'interrogation s'arrête et la continuation
   reprend avec `Success`. Passé `MAX_POLLING_MS`, l'interrogation s'arrête et le
   dernier HTML obtenu est classifié normalement.
6. Classer le résultat :
   - le HTML contient `topictitle` : `Success` ;
   - le HTML contient `cf-turnstile`, `challenge-platform`, `Just a moment`,
     `Un instant` ou `cf_chl` : le chargement est relancé une fois après
     `CHALLENGE_RETRY_DELAY_MS`, et si le second essai donne le même résultat,
     `ChallengeRequired`. Le site répond en français, le marqueur `Un instant` est
     donc indispensable ;
   - le HTML est vide ou ne correspond à aucun des deux cas : `Error`.
7. Encadrer l'ensemble par `withTimeoutOrNull(FETCH_TIMEOUT_MS)`. Cette valeur doit
   couvrir deux interrogations complètes plus le délai de reprise, faute de quoi un
   `ChallengeRequired` légitime se transforme en dépassement de délai trompeur.
8. Détruire la WebView dans un bloc `finally` : `stopLoading()`, `webChromeClient` et
   `webViewClient` remis à null, `removeAllViews()`, `destroy()`. Aucune fuite ne doit
   subsister.
9. Appeler `CookieManager.getInstance().flush()` après un succès, afin que le cookie
   `cf_clearance` survive au redémarrage de l'application.

### Constantes

| Nom | Valeur |
|---|---|
| `USER_AGENT` | `Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36` |
| `POLLING_INTERVAL_MS` | 500 |
| `MAX_POLLING_MS` | 20000 |
| `CHALLENGE_RETRY_DELAY_MS` | 5000 |
| `FETCH_TIMEOUT_MS` | 50000 |
| `SUCCESS_MARKER` | `topictitle` |

Réglages de WebView imposés, en plus de ceux de la section précédente :
`useWideViewPort = true` et `loadWithOverviewMode = true`. Un `viewport` mal
configuré donne au challenge des dimensions de fenêtre incohérentes.

Le user agent doit rester identique d'un appel à l'autre : Cloudflare lie le cookie
`cf_clearance` au user agent, un changement invalide la session.

## Écrans

### Écran de debug, extension

Une section « Réseau » est ajoutée à l'écran de debug existant :

- deux boutons, « Charger le forum 15 » et « Charger le forum 16 » ;
- un indicateur de chargement pendant l'appel ;
- l'issue affichée en clair : « Succès », « Vérification requise » ou « Erreur »
  suivie du message ;
- en cas de succès, la taille du HTML en octets et ses 2000 premiers caractères, dans
  une zone de texte défilante et sélectionnable ;
- un bouton « Copier le HTML » qui place le contenu complet dans le presse papier.

Le bouton de copie est important : il permet à l'utilisateur de récupérer le HTML réel
d'un appareil pour alimenter les tests du module 03 en cas d'évolution du forum.

### Outil de validation manuelle du challenge

Une seconde section « Cloudflare » est ajoutée à l'écran de debug, contenant :

- une phrase expliquant que ce bloc permet de renouveler manuellement le cookie
  d'accès au forum ;
- un bouton « Valider le challenge Cloudflare » ;
- une WebView **visible** de 700 dp de hauteur, intégrée par `AndroidView`, chargeant
  l'URL du forum 15 au clic, configurée à l'identique de
  `WebViewForumPageSource` : même user agent, `javaScriptEnabled`,
  `domStorageEnabled`, `useWideViewPort`, `loadWithOverviewMode`, cookies acceptés.

L'utilisateur coche la case de vérification, la page du forum s'affiche, et le cookie
obtenu devient immédiatement utilisable par la WebView invisible puisque le
`CookieManager` est partagé.

Cet outil n'est pas un reliquat de diagnostic : il est indispensable pendant le
développement des modules 03 à 08, période durant laquelle le module 09 n'existe pas
encore et où aucun autre moyen ne permet de renouveler un cookie expiré. Il reste en
place jusqu'au module 10, qui place l'écran de debug derrière `BuildConfig.DEBUG`.

## Comportement

- Un appel en cours empêche le déclenchement d'un second appel simultané. Les boutons
  sont désactivés pendant le chargement.
- Quitter l'écran pendant un chargement annule la coroutine et détruit la WebView.
- Aucune donnée n'est écrite en base par ce module.

## Règles métier

- Un `ChallengeRequired` n'est jamais traité comme une erreur définitive : il signale
  simplement qu'une intervention humaine est nécessaire, ce que gérera le module 09.
- Un échec ne doit jamais provoquer d'exception remontant à l'appelant.

## Composants UI

`Button`, `CircularProgressIndicator`, `Text` avec `verticalScroll`, `Card`,
`SelectionContainer`.

## Cas limites

| Cas | Comportement attendu |
|---|---|
| Aucune connexion réseau | `Error` avec un message explicite, pas de crash |
| Aucun cookie valide | `ChallengeRequired`, cas normal et fréquent |
| Cookie invalidé par un changement d'adresse IP | `ChallengeRequired` |
| Interrogation sans évolution du DOM | `ChallengeRequired` après `MAX_POLLING_MS` |
| Page de forum vide ou renommée | `Error`, le marqueur `topictitle` est absent |
| Rotation pendant un chargement | La coroutine est annulée proprement |
| Appels successifs rapprochés | Le second est ignoré tant que le premier n'est pas fini |
| Application relancée | Le cookie persiste, le chargement réussit sans validation |

## Fichiers autorisés

```
app/src/main/java/com/jdrvirtuel/watcher/domain/repository/ForumPageSource.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/model/FetchResult.kt
app/src/main/java/com/jdrvirtuel/watcher/data/remote/WebViewForumPageSource.kt
app/src/main/java/com/jdrvirtuel/watcher/data/remote/WebViewConstants.kt
app/src/main/java/com/jdrvirtuel/watcher/core/di/RemoteModule.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugContract.kt
app/src/main/res/values/strings.xml
```

Aucune dépendance nouvelle. La WebView fait partie du framework Android.

## Contrat exposé aux modules suivants

`ForumPageSource.fetchHtml(url)` retournant un `FetchResult`. C'est le seul point
d'entrée réseau de l'application. Le module 04 l'utilise pour la synchronisation, le
module 09 pour la vérification manuelle.

## Tests

Aucun test automatisé. Ce module dépend d'un service externe et d'un composant
Android, il n'est pas testable hors ligne de façon utile.

## Critères d'acceptation

### Vérification automatique

```
gradlew assembleDebug
```

### Scénario manuel

1. Vérifier que l'appareil dispose d'une connexion réseau.
2. Ouvrir l'écran de debug, section Réseau, appuyer sur « Charger le forum 15 ».
   Résultat attendu sur une installation neuve : « Vérification requise ». C'est le
   comportement normal, aucun cookie n'est encore présent.
3. Aller dans la section Cloudflare, appuyer sur « Valider le challenge Cloudflare ».
   Résultat attendu : la WebView affiche « Vérification de sécurité en cours » puis une
   case à cocher.
4. Cocher la case.
   Résultat attendu : la page du forum s'affiche dans la WebView, avec la liste des
   sujets.
5. Revenir à la section Réseau et appuyer sur « Charger le forum 15 ».
   Résultat attendu : « Succès », avec un HTML de plusieurs centaines de milliers
   d'octets. Une taille voisine de 27 000 octets signalerait une page de challenge,
   donc un échec.
6. Lire l'extrait affiché.
   Résultat attendu : du HTML de forum phpBB, contenant `topictitle`.
7. Appuyer sur « Charger le forum 16 ».
   Résultat attendu : succès également, le cookie couvre tout le domaine.
8. Activer le mode avion et relancer un chargement.
   Résultat attendu : « Erreur » avec un message compréhensible, aucun crash.
9. Désactiver le mode avion, fermer complètement l'application, la relancer et
   recharger le forum 15.
   Résultat attendu : succès sans nouvelle validation, le cookie a survécu au
   redémarrage.
10. Appuyer sur « Copier le HTML » puis coller dans une note.
    Résultat attendu : le HTML complet est dans le presse papier.

## Travail attendu de Gemini

Créer uniquement les classes listées et l'extension de l'écran de debug. Ne pas
écrire d'analyse du HTML, même partielle : elle appartient au module 03. Ne pas
écrire en base.

Une attention particulière est attendue sur la destruction de la WebView et sur
l'annulation de la coroutine, qui sont les deux sources classiques de fuite mémoire.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 01 est validé. Lis `00_SPECIFICATIONS_GENERALES.md` puis
> `MODULE_02_ACCES_AU_FORUM.md` et implémente uniquement le module 02. Le site est
> protégé par Cloudflare : n'utilise ni OkHttp ni aucun client HTTP, uniquement une
> WebView, comme spécifié. Respecte la liste des fichiers autorisés et termine par le
> compte rendu demandé.
