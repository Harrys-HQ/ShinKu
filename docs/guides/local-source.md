# Local Source Guide

ShinKu includes a built-in **Local Source** that allows you to read your own personal collection of DRM-free manga, comics, and light novels stored directly on your device's internal storage or SD card.

---

## 📁 Storage Structure

To allow ShinKu to detect your local comics, place your files inside the `local` subfolder of your ShinKu storage directory:

```
[Your ShinKu Storage Directory]/
└── local/
    ├── Berserk/
    │   ├── Chapter 01.cbz
    │   ├── Chapter 02.cbz
    │   └── cover.jpg (optional)
    └── One Piece/
        ├── Chapter 1000/
        │   ├── 001.jpg
        │   ├── 002.jpg
        │   └── 003.jpg
        └── Chapter 1001.zip
```

### Key Rules:
* Each series must have its own dedicated folder inside `local/` (e.g. `local/Berserk/`).
* Each chapter inside the series folder can be:
  * A compressed archive (`.cbz`, `.zip`, `.cbr`, `.rar`).
  * A subfolder containing individual image files (`.jpg`, `.png`, `.webp`, `.avif`).

---

## 🖼️ Custom Series Covers

To display custom cover art for your local series:
* Place an image named `cover.jpg` or `cover.png` inside the series folder (e.g. `local/Berserk/cover.jpg`).
* If no cover image is provided, ShinKu will automatically use the first page of the first chapter as the cover.

---

## 📑 ComicInfo.xml Support

ShinKu automatically reads embedded `ComicInfo.xml` metadata inside CBZ/ZIP archives to populate:
* Series Title & Author
* Chapter Number & Volume
* Genre tags and plot summary
