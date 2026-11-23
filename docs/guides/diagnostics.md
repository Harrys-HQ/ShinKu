# App Diagnostics & Source Health

ShinKu is **local-first and zero-telemetry**. Built-in diagnostics and Source Health run on your device to help explain why a repository, extension, or source is failing without sending background usage data or telemetry anywhere.

---

## Where to Open Diagnostics

- **More → App Diagnostics**
- **More → Settings → Advanced → App Diagnostics**
- **More → Configure ShinKu → Maintenance & Tools → App Diagnostics** (or **Source Health**)

---

## Separate Health Dimensions

ShinKu avoids collapsing every failure into a single "dead" label. Instead, it separates three layers:

1. **Repository availability (`EXTENSION_REPOS`)**
   Checks your configured repository indexes (`index.pb`, `index.min.json`, or `index.json`) using bounded read-only HTTP GET requests when you tap **Check repositories** or refresh extensions. Failures distinguish network/HTTP errors from unparseable index files.
   Dedicated diagnostic checks cap downloaded and decompressed index data at 16 MiB per file. Source Health uses the current extension catalog. To refresh it, open **Browse → Extensions** and refresh there.
2. **Extension installation & loading (`EXTENSION_LOADING`)**
   Inspects locally installed extensions and distinguishes **Loaded**, **Update ready**, **Not in repos (Obsolete)**, **Untrusted**, and **Load failed** (missing version, incompatible extension API version, unsigned package, disabled 18+/NSFW filter, or classloader/instantiation errors).
3. **Source reachability & operations (`SOURCE_HEALTH`)**
   Records structured error categories (DNS resolution failure, connect timeout, read timeout, TLS/certificate error, HTTP `401`/`403`/`404`/`429`/`5xx`/redirects, Cloudflare challenges, parser failures) and tracks consecutive failures.
   - A base URL check is an unauthenticated request using ShinKu's client. It measures page reachability. Signed-in access and extension operations have their own results.
   - **Device offline** (`OFFLINE`) does **not** count against a source or increment its failure streak.
   - A **single failed probe** marks a source as **Unreachable** or **Degraded**, never **Dead**. Only repeated consecutive `DNS_FAILURE` or `HTTP_NOT_FOUND` failures (3 or more) surface as **Likely dead (3+)**, and rate-limited (`429`) or Cloudflare-protected (`403`) sources remain **Degraded** with guidance to open WebView or retry later.
   - Checks older than **7 days** are flagged as **Stale check**.

---

## What Is Collected Locally vs. Never Collected

### Collected locally for on-screen checks:
- App version, build flavor/commit, Android OS version, and device model.
- Android System WebView availability and version.
- Current network transport status (online, Wi-Fi/VPN flags, DNS-over-HTTPS provider setting).
- WorkManager worker state counts and local scan correlation ID / phase.
- Repository hostnames (never full repository URLs or query strings) and extension load statuses.
- Sanitized source health summaries, HTTP status codes, and latency.
- Optional **Recent diagnostic events** with structured categories, timestamps, and status codes (**off by default** when exporting).

### Never included:
- Manga or chapter titles, page images, library entries, or reading history.
- API keys, bearer/basic tokens, cookies, authorization headers, query parameters, or URL userinfo.
- Full extension repository URLs.

---

## Reviewing, Exporting, and Deleting Diagnostics

1. **Run local check**: Inspects environment, WebView, network state, workers, extensions, and cached health immediately without network calls or AI.
2. **Preview & export**: Lets you toggle individual sections on or off (recent diagnostic events are unchecked by default) and inspect the exact redacted text before copying or sharing via Android's share sheet.
3. **Optional AI summary**: If you have configured a cloud AI provider, **AI summary** shows a confirmation dialog with the exact destination and exact redacted report text first. Canceling makes zero requests. Any AI summary is read-only and advisory.
4. **Clear / Delete**: Open the overflow menu (`⋮`) in **App Diagnostics** and tap **Clear local diagnostics** to wipe in-memory diagnostic events, or **Reset diagnostics & source health** to also clear stored source health counters from the local database. Cancel an active Source Health scan first if you want to prevent it from recording new results.
