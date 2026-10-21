# Getting Started with ShinKu

Welcome to **ShinKu**! This guide walks you through downloading, installing, and configuring ShinKu on your Android device for the best possible experience.

---

## 📥 1. Download & Installation

ShinKu is distributed as a direct, standalone APK asset.

1. Go to the [Official GitHub Releases](https://github.com/Harrys-HQ/ShinKu/releases/latest).
2. Download the latest `devRelease` APK file (e.g., `ShinKu-dev-release-vX.X.X.apk`).
3. Open your device's **Downloads** folder and tap the downloaded APK.
4. If prompted with *"Install unknown apps"*, allow your browser or file manager permission to install the APK.
5. Tap **Install** and open **ShinKu**.

::: tip Minimum Requirements
* Android 8.0 (Oreo) or higher (Android 11+ recommended).
* At least 2GB of RAM.
* Sufficient free internal or SD card storage for offline chapters.
:::

---

## ⚙️ 2. Storage Setup (Android Scoped Storage)

Android requires apps to use Scoped Storage to save downloaded chapters and backups.

1. During first launch, ShinKu will prompt you to select a **Storage Location**.
2. Tap **Select Directory** and choose a folder on your internal storage or SD card (e.g. `Documents/ShinKu` or `Manga`).
3. Tap **Use this folder** and confirm **Allow** when Android requests access.

::: warning Note on Downloads
Never select the system `Android/data` or `Android/obb` folders, as modern Android versions strictly restrict access to these paths.
:::

---

## 🔋 3. Disabling Battery Optimization (Crucial)

To allow ShinKu to automatically refresh your manga library in the background and download new chapters without interruption, you must disable Android battery optimization for ShinKu:

1. In ShinKu, navigate to **More → Settings → Advanced → Battery optimization**.
2. Tap **Disable battery optimization**.
3. In the system dialog, select **Allow** (or change ShinKu's battery mode to **Unrestricted** / **Don't optimize**).

::: info Why is this required?
Aggressive battery savers (common on Xiaomi/MIUI, Samsung OneUI, and OnePlus OxygenOS) kill background tasks after a few minutes, causing scheduled library updates and chapter downloads to stop abruptly.
:::

---

## 🚀 4. Next Steps

Now that your reader is installed and configured:
* Learn [How to Configure Extension Repositories](/guides/extension-repositories).
* Explore the [Sanctuary Jump-Back Deck & Library](/guides/library).
* Set up [AI Live Translation & Immersion Features](/guides/ai-features).
