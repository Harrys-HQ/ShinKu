# Storage & Download FAQ

Learn how ShinKu handles chapter downloads, storage directories, and download errors.

---

## 📁 Why Can't I Select the `Android/data` Folder?

Starting with Android 11 (API level 30), Google enforced strict **Scoped Storage** security boundaries. Apps can no longer freely access `Android/data` or `Android/obb` folders on internal storage or external SD cards.

### Recommended Storage Folders:
* Create a dedicated folder in your internal storage such as `/Documents/ShinKu/` or `/Storage/Manga/`.
* If using an SD Card, create a folder at the root of the SD card (e.g., `/SDCard/ShinKu/`).

---

## ⚡ How Resumable Byte-Range Downloads Work

Legacy readers often discard partially downloaded chapters if network connectivity drops or the source server drops the connection midway, forcing you to redownload the whole chapter from image 1.

ShinKu features **Resumable Byte-Range Downloads**:
* If your connection drops at image 14 of 30, ShinKu saves the verified partial content.
* When your connection recovers, ShinKu sends an HTTP `Range: bytes=...` request to resume exactly where it left off, saving mobile data and avoiding corrupted chapter zips.

---

## ⚠️ "Storage Directory Inaccessible" Error

If ShinKu displays an error that your storage location cannot be written to:
1. Navigate to **More → Settings → Data and storage**.
2. Tap **Storage directory**.
3. Re-select your chosen directory and tap **Use this folder** to renew Android's persistent URI grant permission.
