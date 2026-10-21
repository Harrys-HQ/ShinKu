# Source Migration Guide

When a source extension stops working, undergoes domain changes, or gets discontinued, ShinKu's **Migration Suite** allows you to seamlessly transfer manga, reading progress, and custom category assignments to alternative sources without starting over.

---

## 🧲 1. Standard Source Migration

To migrate an entire source or selected manga to another source:

1. Open **ShinKu** and navigate to the **Browse** tab.
2. Select the **Migration** tab at the top.
3. Tap the source you want to migrate from.
4. Select the manga title(s) you wish to transfer.
5. Search for the title on your destination source and select the matching entry.
6. Choose what data you want to copy across:
   * **Reading Progress & Chapters:** Keep chapter read states intact.
   * **Categories:** Retain your custom library tags and folders.
   * **Tracking:** Automatically bind and sync your existing AniList/MAL trackers.
7. Confirm the migration.

---

## 💀 2. ShinKu Exclusive: Dead Source Scanner

If an extension was uninstalled, deleted, or removed, your library might retain "orphaned" manga titles that cannot be updated.

ShinKu includes a built-in **Dead Source Scanner**:

1. Go to **Browse → Migration**.
2. Tap the overflow menu (three dots in top right) and select **Scan for Dead Sources**.
3. ShinKu will instantly analyze your database and isolate every library title that belongs to an uninstalled or missing extension.
4. From the scan results, you can batch-migrate them to active sources or cleanly purge defunct entries.

---

## ⚠️ 3. Failed Updates Migration

Sometimes sources change their internal chapter URLs or title slugs, causing normal library updates to report errors.

1. Go to **Browse → Migration → Failed Updates**.
2. ShinKu lists all manga whose last automatic refresh returned a 404, 502, or parse failure.
3. Tap any title to perform an in-place source re-bind, instantly restoring update capability without losing your reading history.
