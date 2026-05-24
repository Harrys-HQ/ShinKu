# Extension Compatibility & Architectural Safeguards (The "Freeze Zone")

## Overview
ShinKu maintains a critical "Bridge" with external APK extensions (originally developed for Tachiyomi/Mihon). To ensure these extensions continue to function, certain package names and class structures must remain static.

## The Freeze Zone (Do Not Rename/Move)

### 1. Source Interfaces
These classes form the primary communication layer between the app and the extensions.
- **Path:** `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/Source.kt`
- **Path:** `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/model/SManga.kt`
- **Path:** `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/model/SChapter.kt`
- **Path:** `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/model/Page.kt`

### 2. Networking
Extensions rely on the app's shared OkHttp client and helper classes.
- **Path:** `core/common/src/main/kotlin/eu/kanade/tachiyomi/network/NetworkHelper.kt`
- **Path:** `core/common/src/main/kotlin/eu/kanade/tachiyomi/network/JavaScriptEngine.kt`

### 3. Deep Links & Redirects
Intent filters in `AndroidManifest.xml` must support legacy schemes to handle browser redirects and repository additions.
- **Scheme:** `tachiyomi://`
- **Scheme:** `mihon://`
- **Scheme:** `shinku://`
- **Critical Activity:** `com.shinku.reader.ui.main.MainActivity`
- **Critical Activity:** `com.shinku.reader.ui.deeplink.DeepLinkActivity`

## Deep Link Reference
ShinKu supports several deep link schemes and hosts to ensure compatibility with external tools and legacy redirects.

| Feature | Scheme | Host | Path / Extra |
| --- | --- | --- | --- |
| **Add Repository** | `com.shinku.reader`, `tachiyomi`, `mihon` | `add-repo` | N/A |
| **AniList Auth** | `com.shinku.reader`, `tachiyomi`, `mihon` | `anilist-auth` | N/A |
| **Bangumi Auth** | `com.shinku.reader`, `tachiyomi`, `mihon` | `bangumi-auth` | N/A |
| **MyAnimeList Auth** | `com.shinku.reader`, `tachiyomi`, `mihon` | `myanimelist-auth` | N/A |
| **Shikimori Auth** | `com.shinku.reader`, `tachiyomi`, `mihon` | `shikimori-auth` | N/A |
| **MangaDex Auth** | `shinku` | `mangadex-auth` | N/A |
| **Dropbox Auth** | `db-6h2m965uod1itio` | N/A | N/A |
| **Global Search** | `com.shinku.reader.SEARCH` | N/A | (Implicit Search Action) |
| **E-Hentai / ExH** | `http`, `https` | `e-hentai.org`, `exhentai.org` | `/g/..*` |
| **MangaDex Redirect** | `https` | `mangadex.org` | `/manga/..*`, `/title/..*` |

## Why is this necessary?
APK extensions are compiled independently of the main app. They use "reflection" or hardcoded class paths to interact with the host application. If we move `eu.kanade.tachiyomi.source.Source` to `com.shinku.reader.source.Source`, every existing extension will crash with a `ClassNotFoundException`.

## How to Refactor Safely
If you need to modernize a "frozen" class:
1. **Delegate:** Create a new, modern class in the `com.shinku` package.
2. **Bridge:** Make the legacy class in the `eu.kanade` package act as a wrapper or bridge to the new class.
3. **Internal Use:** Update the app's internal logic to use the new class, but keep the legacy class available for extensions.
