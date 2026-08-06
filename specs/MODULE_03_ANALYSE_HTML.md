# MODULE 03 : Analyse du HTML

Statut : à faire
Prérequis : module 00 validé

## Objectif

Transformer le code HTML d'une page `viewforum.php` en une liste d'objets exploitables,
et garantir par des tests unitaires que cette transformation est correcte.

C'est la partie la plus fragile de l'application : elle dépend de la structure du
thème du forum. Elle est donc isolée, entièrement testable hors ligne, et documentée
en détail.

Ce module ne fait pas : aucun accès réseau, aucune écriture en base, aucune
comparaison avec l'existant.

## Fichier de référence

Le fichier `app/src/test/resources/viewforum_f15.html` est fourni par l'utilisateur.
Il s'agit d'une sauvegarde réelle de la première page du forum 15.

Gemini ne doit ni le créer, ni le modifier, ni en inventer le contenu. S'il est
absent, Gemini interrompt le module et le signale.

## Structure HTML observée

Chaque sujet est une balise `<li class="row ...">` contenant un `<dl class="row-item ...">`.

### Sujet épinglé, à ignorer

```html
<li class="row bg2 sticky">
  <dl class="row-item sticky_read_locked">
    <dt title="Ce sujet est verrouillé...">
      <div class="list-inner">
        <a href=".../viewtopic.php?f=15&t=32440" class="topictitle">Veuillez utiliser l'icone "FULL"...</a>
```

Marqueur : la classe `sticky` sur le `<li>`.

### Sujet ordinaire

```html
<li class="row bg1">
  <dl class="row-item topic_read">
    <dt title="Aucun message non lu">
      <div class="list-inner">
        <a href=".../viewtopic.php?f=15&t=41234" class="topictitle">[Friponnes RPG][Discord][12/08][1/3 places]</a>
        <div class="topic-poster responsive-hide">
          par <a href="..." class="username">Etienneb</a> » ven. 24 juil. 2026 16:26
        </div>
    <dd class="posts">10 <dfn>Réponses</dfn></dd>
    <dd class="views">117 <dfn>Vues</dfn></dd>
    <dd class="lastpost">
      <span><dfn>Dernier message </dfn>par <a href="..." class="username">Weyland-Yutani Corp</a>
        <a href="..." title="Aller au dernier message">...</a>
        <br>mer. 5 août 2026 14:52
      </span>
    </dd>
```

### Sujet complet

Identique au précédent, à une différence près : le `<dt>` porte un attribut `style`
avec une image de fond.

```html
<dt style="background-image: url(./images/icons/misc/Complet.png); background-repeat: no-repeat;" title="Aucun message non lu">
```

Point important : un sujet complet reste `topic_read`, il n'est ni verrouillé ni
fermé au sens de phpBB. L'icône `Complet.png` est une icône de sujet choisie par
l'auteur. C'est le seul marqueur disponible.

## Sélecteurs imposés

| Donnée | Sélecteur ou méthode |
|---|---|
| Lignes de sujet | `ul.topiclist.topics li.row`, avec repli sur `li.row` si le premier ne retourne rien |
| Sujet épinglé | classe `sticky`, `announce` ou `global` sur le `<li>` |
| Titre | texte de `a.topictitle` |
| URL | attribut `href` de `a.topictitle` |
| Identifiant | expression régulière `[?&]t=(\d+)` appliquée à l'URL |
| Sujet complet | attribut `style` du `<dt>` contenant `complet` en insensible à la casse |
| Auteur | premier `a.username, a.username-coloured` dans `div.topic-poster` |
| Date de création | texte de `div.topic-poster` après le caractère `»` |
| Nombre de réponses | premier groupe de chiffres du texte de `dd.posts` |
| Auteur du dernier message | premier `a.username, a.username-coloured` dans `dd.lastpost` |
| Date du dernier message | dernière occurrence du motif de date dans le texte de `dd.lastpost` |

L'attribut `class` des noms d'utilisateur varie : `username` pour un membre ordinaire,
`username-coloured` pour un membre d'un groupe coloré. Les deux doivent être pris en
compte, faute de quoi les auteurs de certaines lignes seront vides.

## Analyse des dates

Format observé : `ven. 24 juil. 2026 16:26`, `sam. 1 août 2026 02:12`,
`mer. 5 août 2026 14:52`.

Règles :

1. Retirer le préfixe de jour de semaine, par exemple `ven. `, à l'aide d'une
   expression régulière `^\p{L}{3}\.\s*`.
2. Normaliser les espaces insécables : remplacer `\u00A0` et `\u202F` par un espace
   ordinaire, puis réduire les espaces multiples.
3. Analyser avec un `DateTimeFormatter` construit ainsi :
   ```kotlin
   DateTimeFormatterBuilder()
       .parseCaseInsensitive()
       .appendPattern("d MMM yyyy HH:mm")
       .toFormatter(Locale.FRENCH)
   ```
   Le jour n'est pas complété par un zéro, le motif `d` le gère.
4. Le forum affiche ses dates dans le fuseau `Europe/Paris`. La conversion en
   millisecondes epoch se fait via
   `LocalDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant().toEpochMilli()`.
5. Une date non analysable ne fait pas échouer la ligne entière : la valeur `0L` est
   retournée et l'incident est comptabilisé.

Les abréviations françaises attendues sont `janv.`, `févr.`, `mars`, `avr.`, `mai`,
`juin`, `juil.`, `août`, `sept.`, `oct.`, `nov.`, `déc.`, ce qui correspond à la
locale `Locale.FRENCH` de la machine virtuelle.

## Architecture

### Classes à créer

| Classe | Package | Rôle |
|---|---|---|
| `ParsedTopic` | `domain.model` | Sujet issu de l'analyse, sans état utilisateur |
| `ParseResult` | `domain.model` | Résultat global de l'analyse |
| `TopicListParser` | `data.parser` | Analyse d'une page complète |
| `FrenchDateParser` | `data.parser` | Analyse des dates du forum |
| `ParserModule` | `core.di` | Fourniture Hilt |

### Modèles

```kotlin
data class ParsedTopic(
    val id: Int,
    val title: String,
    val url: String,
    val author: String,
    val createdAt: Long,
    val replyCount: Int,
    val lastPostAuthor: String,
    val lastPostAt: Long,
    val isFull: Boolean
)

data class ParseResult(
    val topics: List<ParsedTopic>,
    val skippedSticky: Int,
    val skippedInvalid: Int
)
```

`ParsedTopic` ne contient volontairement ni `isHidden`, ni `isWatched`, ni `isRead` :
ces états appartiennent à l'utilisateur et non à la page web. C'est le module 04 qui
les préserve lors de la fusion.

### Signature du parseur

```kotlin
class TopicListParser @Inject constructor(
    private val dateParser: FrenchDateParser
) {
    fun parse(html: String): ParseResult
}
```

L'analyse se fait exclusivement avec `Jsoup.parse(html)`, jamais avec
`Jsoup.connect()`.

## Règles métier

- Les sujets épinglés sont exclus et comptés dans `skippedSticky`.
- Une ligne dont l'identifiant ne peut être extrait est exclue et comptée dans
  `skippedInvalid`.
- Un titre vide n'est pas une raison d'exclusion, le titre est conservé tel quel.
- Les URL relatives sont converties en URL absolues à partir de
  `https://www.jdrvirtuel.com/`.
- Les entités HTML sont décodées, le titre doit ressortir en texte lisible, emojis
  compris.
- L'ordre des sujets retourné est celui de la page.

## Écrans

### Écran de debug, extension

Une section « Analyse » est ajoutée :

- un bouton « Analyser le fichier de test », qui lit
  `viewforum_f15.html` depuis les ressources de test ne fonctionnant pas en
  production : à la place, une copie du même fichier est placée dans
  `app/src/main/assets/viewforum_f15.html` par l'utilisateur, et c'est ce fichier que
  l'écran de debug lit ;
- un bouton « Analyser le dernier HTML chargé », actif si le module 02 a déjà
  rapporté un HTML pendant la session ;
- le résultat affiché : nombre de sujets analysés, nombre d'épinglés ignorés, nombre
  de lignes invalides ;
- la liste des sujets analysés, chacun montrant identifiant, titre, auteur, date de
  création lisible, nombre de réponses, auteur et date du dernier message, et un
  marqueur « Complet » le cas échéant.

Note pour l'utilisateur : copier `viewforum_f15.html` à la fois dans
`app/src/test/resources/` pour les tests unitaires et dans `app/src/main/assets/`
pour l'écran de debug.

## Composants UI

`Button`, `LazyColumn`, `Card`, `Text`, `AssistChip` pour le marqueur « Complet ».

## Cas limites

| Cas | Comportement attendu |
|---|---|
| HTML vide | `ParseResult` avec une liste vide, aucun crash |
| HTML de page de challenge | Liste vide, aucun crash |
| Aucune ligne `li.row` | Liste vide |
| Ligne sans `a.topictitle` | Ligne ignorée, `skippedInvalid` incrémenté |
| Date illisible | Valeur `0L`, la ligne est conservée |
| Auteur absent | Chaîne vide, la ligne est conservée |
| Nombre de réponses absent | Valeur 0 |
| Titre contenant des emojis | Restitué intégralement |
| Titre contenant des entités HTML | Décodé |

## Tests unitaires attendus

Fichier `app/src/test/java/com/jdrvirtuel/watcher/data/parser/TopicListParserTest.kt`.

Les tests s'appuient sur `viewforum_f15.html`, chargé depuis les ressources de test.

| Test | Vérification |
|---|---|
| `parse_ignoreLesSujetsEpingles` | Aucun sujet retourné ne porte un identifiant de sujet épinglé, `skippedSticky` est supérieur ou égal à 1 |
| `parse_extraitLIdentifiantDepuisLUrl` | Le sujet 41234 est présent |
| `parse_extraitLeTitre` | Le titre du sujet 41234 est `[Friponnes RPG][Discord][12/08][1/3 places]` |
| `parse_extraitLAuteurEtLaDateDeCreation` | Auteur `Etienneb`, date correspondant au 24 juillet 2026 à 16:26 heure de Paris |
| `parse_extraitLeNombreDeReponses` | 10 pour le sujet 41234 |
| `parse_extraitLeDernierMessage` | Auteur `Weyland-Yutani Corp`, date du 5 août 2026 à 14:52 |
| `parse_detecteLIconeComplet` | `isFull` vrai pour le sujet 41248, faux pour le sujet 41234 |
| `parse_gereLesNomsColores` | L'auteur d'une ligne utilisant `username-coloured` n'est pas vide |
| `parse_construitDesUrlAbsolues` | Toutes les URL commencent par `https://` |
| `parse_htmlVide` | Liste vide, aucune exception |

Fichier `app/src/test/java/com/jdrvirtuel/watcher/data/parser/FrenchDateParserTest.kt`.

| Test | Vérification |
|---|---|
| `parse_dateStandard` | `ven. 24 juil. 2026 16:26` produit l'instant attendu |
| `parse_jourSansZero` | `sam. 1 août 2026 02:12` est analysé |
| `parse_moisAvecAccent` | `août` et `déc.` sont reconnus |
| `parse_espaceInsecable` | Une date contenant `\u00A0` est analysée |
| `parse_dateInvalide` | Retourne `0L` sans exception |

## Fichiers autorisés

```
gradle/libs.versions.toml                     ajout de Jsoup uniquement
app/build.gradle.kts                          ajout de la dépendance uniquement
app/src/main/java/com/jdrvirtuel/watcher/domain/model/ParsedTopic.kt
app/src/main/java/com/jdrvirtuel/watcher/domain/model/ParseResult.kt
app/src/main/java/com/jdrvirtuel/watcher/data/parser/TopicListParser.kt
app/src/main/java/com/jdrvirtuel/watcher/data/parser/FrenchDateParser.kt
app/src/main/java/com/jdrvirtuel/watcher/core/di/ParserModule.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugScreen.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugViewModel.kt
app/src/main/java/com/jdrvirtuel/watcher/feature/debug/DebugContract.kt
app/src/test/java/com/jdrvirtuel/watcher/data/parser/TopicListParserTest.kt
app/src/test/java/com/jdrvirtuel/watcher/data/parser/FrenchDateParserTest.kt
app/src/main/res/values/strings.xml
```

Dépendance à ajouter : `org.jsoup:jsoup`, version minimale 1.17.2.
Dépendances de test : `junit`, déjà présente dans le projet généré.

Fichiers fournis par l'utilisateur, à ne pas créer :
`app/src/test/resources/viewforum_f15.html` et `app/src/main/assets/viewforum_f15.html`.

## Contrat exposé aux modules suivants

`TopicListParser.parse(html: String): ParseResult` et le modèle `ParsedTopic`. Le
module 04 est le seul consommateur.

## Critères d'acceptation

### Vérification automatique

```
gradlew testDebugUnitTest
```

Tous les tests passent. C'est le critère principal de ce module.

```
gradlew assembleDebug
```

### Scénario manuel

1. Ouvrir l'écran de debug, section Analyse, appuyer sur « Analyser le fichier de
   test ».
   Résultat attendu : le nombre de sujets analysés correspond au nombre de sujets non
   épinglés visibles sur la page réelle, et au moins un épinglé est signalé comme
   ignoré.
2. Chercher le sujet `[Friponnes RPG][Discord][12/08][1/3 places]`.
   Résultat attendu : auteur `Etienneb`, création le 24 juillet 2026, 10 réponses,
   dernier message de `Weyland-Yutani Corp` le 5 août 2026, pas de marqueur
   « Complet ».
3. Chercher le sujet `# ☣️ RECRUTEMENT – VERMINES 2047 ☣️`.
   Résultat attendu : marqueur « Complet » présent, emojis correctement affichés,
   auteur `Noctus`, 3 réponses.
4. Vérifier qu'aucun sujet épinglé n'apparaît dans la liste.
5. Revenir à la section Réseau, charger le forum 15, puis revenir à la section Analyse
   et appuyer sur « Analyser le dernier HTML chargé ».
   Résultat attendu : une liste de sujets réels et à jour, cohérente avec la page web
   ouverte en parallèle dans un navigateur.

## Travail attendu de Gemini

Créer uniquement le parseur, l'analyseur de dates, leurs tests et l'extension de
l'écran de debug. Ne pas écrire en base. Ne pas appeler le réseau. Ne pas modifier le
module 02.

Si un sélecteur ne fonctionne pas sur le fichier de référence, le signaler dans le
compte rendu plutôt que d'inventer une solution de repli non spécifiée.

Terminer par le compte rendu structuré.

## Prompt de démarrage

> Le module 02 est validé. Lis `00_SPECIFICATIONS_GENERALES.md` puis
> `MODULE_03_ANALYSE_HTML.md` et implémente uniquement le module 03. Le fichier
> `app/src/test/resources/viewforum_f15.html` existe déjà, ne le crée pas et ne le
> modifie pas. Utilise uniquement `Jsoup.parse`, jamais `Jsoup.connect`. Respecte la
> liste des fichiers autorisés et termine par le compte rendu demandé.
