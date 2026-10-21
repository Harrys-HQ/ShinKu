# Extension Repositories Guide

ShinKu is built as a modular reader shell. Out of the box, ShinKu contains no bundled scrapers or manga feeds. Instead, it allows users to connect compatible external extension repository feeds provided by independent third-party developers.

---

## ⚖️ Important Disclaimer

::: info Neutral Platform Notice
* ShinKu does **not** develop, host, curate, or maintain any extension repositories or comic scraping feeds.
* ShinKu maintainers cannot provide URLs, endorsement, or customer support for specific third-party sources.
* You are solely responsible for ensuring that the repositories and content you connect to adhere to applicable copyright and licensing laws in your jurisdiction.
:::

---

## 🛠️ How to Add an Extension Repository

Third-party developers publish repository index feeds using either modern binary Protobuf (`.pb`) catalogs or standard JSON manifests. ShinKu natively supports both formats with automatic fallback.

### Steps to Add a Repository:

1. Obtain a repository index URL from an independent community developer or provider of your choice (example format: `https://example.com/index.min.json`).
2. Open **ShinKu** and tap the **More** tab in the navigation dock.
3. Tap **Settings** → **Browse** → **Extension repositories**.
4. Tap the **+ Add repository** floating button or menu action.
5. In the input dialog, paste your repository URL.
6. Tap **Add**.

Once added, navigate to the **Browse** tab and tap **Extensions**. ShinKu will automatically fetch the catalog, allowing you to install extensions individually.

---

## 🔄 Updating & Refreshing Repositories

Extension repositories receive updates independently from the main ShinKu application.

* **Automatic Checks:** ShinKu periodically queries your added repository URLs to display when newer extension versions are available.
* **Manual Refresh:** You can pull-to-refresh on the **Browse → Extensions** tab to immediately check for newer extension packages.
* **Batch Update:** If multiple extensions have updates, tap the **Update All** button at the top right of the extensions list.

---

## 🗑️ Removing a Repository

1. Navigate to **More → Settings → Browse → Extension repositories**.
2. Tap the three-dot menu next to the repository you want to remove.
3. Select **Delete**.
4. *(Optional)* Use the [Dead Source Scanner](/guides/source-migration#dead-source-scanner) to migrate any saved manga from removed extensions to other available sources.
