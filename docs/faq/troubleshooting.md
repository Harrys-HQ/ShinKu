# Common Troubleshooting & Error Resolution

This guide addresses the most common issues encountered when reading or browsing in ShinKu.

---

## 🛡️ Cloudflare & Bot Verification Challenges

Many websites employ Cloudflare Turnstile, DDoS protection, or bot mitigation to prevent automated scraping.

### Symptoms:
* Chapters fail to load, reporting `HTTP 403`, `HTTP 1020`, or `Cloudflare error`.
* The manga cover or chapter list appears empty.

### How to Resolve:
1. **Open in WebView:**
   * Go to the affected manga detail page or browse source.
   * Tap the **Globe / WebView** icon in the top toolbar.
2. **Complete the Verification:**
   * Wait for the Cloudflare captcha/Turnstile checkbox to appear.
   * Complete the challenge manually in the WebView window until the website content loads normally.
3. **Return to ShinKu:**
   * Press back to return to ShinKu. The session cookies are automatically passed to ShinKu's background HTTP client.
4. **Update Android System WebView:**
   * Ensure your device's **Android System WebView** app is updated to the latest version via the Google Play Store.

::: tip ShinKu's Anti-Bot Spoofing
ShinKu incorporates dynamic `Sec-CH-UA` client hint metadata and modern browser header emulation, reducing the frequency of Cloudflare challenges compared to legacy readers.
:::

---

## 🚫 HTTP 429: Too Many Requests (Rate Limiting)

### Why does this happen?
Sources enforce rate limits when too many requests are sent in a short burst (e.g., refreshing hundreds of manga simultaneously or mass-downloading chapters).

### How to Fix:
1. Pause ongoing downloads for 5 to 10 minutes.
2. In **More → Settings → Advanced**, check your network timeout settings.
3. Spread out your library updates into smaller custom categories rather than running a global refresh all at once.

---

## 📜 Viewing Error Logs & Running Diagnostics

If an unexpected error occurs, ShinKu provides built-in local diagnostics and redacted error export:

1. Open **More → App Diagnostics** (or **More → Settings → Advanced → App Diagnostics**).
2. Review check statuses (`OK`, `WARNING`, `FAILED`, `NOT CHECKED`), check **Source Health**, or tap **Preview & export** to inspect and share a redacted report.
3. See the [App Diagnostics & Health Guide](/guides/diagnostics) for details on each check, redaction rules, and clearing stored diagnostic history.

For a full crash-log share outside diagnostics, go to **More → Settings → Advanced → Dump crash logs**.
