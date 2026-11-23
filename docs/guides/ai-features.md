# AI & Immersion Features Guide

ShinKu integrates cutting-edge artificial intelligence and atmospheric sensory cues to elevate manga reading beyond static scans.

---

## 🪄 AI Live Translation Engine

Read untranslated raw manga and foreign-language webtoons on the fly without waiting for community translations.

### Supported Translation Backends:
1. **Google Gemini API (Cloud):** High-precision cloud translation capable of nuanced dialogue understanding, slang translation, and honorific preservation.
2. **Universal OpenAI-Compatible API:** Connect to OpenRouter, DeepSeek, Groq, or a custom endpoint.
3. **On-Device Google ML Kit:** OCR and translation run on your device.

### How to Use Live Translation:
1. **Reader Bottom Bar**: Tap the center of your screen while reading. Tap the **Translate** icon (`文A`) in the bottom bar to toggle dialogue bubble overlays on or off.
2. **Translation Options**: Long-press the **Translate** icon in the bottom bar to:
   - **Inspect Page Bubbles**: View detected text blocks, compare original raw text against translated dialogue, or copy quotes.
   - **Pre-Translate Entire Chapter**: Translate all chapter pages in the background with persistent disk caching.
   - **Change Target Language**: Select English, Spanish, French, German, Indonesian, Tagalog, and more.
   - **Series glossary**: Add original terms and preferred translations for this series, and choose whether to preserve honorifics. The glossary is stored on this device. On-device translation substitutes matching terms locally. For a cloud request, matching entries appear in the prompt preview before you approve it.

For cloud translation of a page or selected area, the reader shows the destination and the exact text prompt for approval. The phone runs OCR on the image and removes recognizable credentials from the text before previewing it. Cloud translation sends the approved prompt with OCR text and language instructions. Page images, series title, genres, and library history stay on-device in this flow. Tap **Cancel** to skip the request. Keep the chapter translation dialog open to review each uncached page that needs cloud translation.

Automatic translation as you turn pages reuses saved translations, or uses on-device ML Kit in Automatic and on-device modes. In cloud-only mode, tap Translate to request a preview for an uncached page. ML Kit may download language models before its first translation for a language.

New saved results are kept separately for each set of language, provider, model, and glossary settings. After changing those settings, translate again to see results for the new profile. Use **Show older translations** to view saved overlays from before ShinKu tracked these settings.

---

## 🎯 Interactive Area OCR & In-Place Translation (BETA)

Translate custom areas, signs, background sound effects, or stubborn speech balloons that full-page OCR might miss:

1. **Activate Area OCR**: Long-press on any page while reading and select **Area OCR Translate** from the actions menu.
2. **Precision Viewport Snapshot**: The reader freezes a 1:1 snapshot of the screen with an interactive crop overlay.
3. **Target Any Text**: Drag the bounding box and corner touch handles over any text, sign, or dialogue bubble.
4. **In-Place Translation**: Tap **Translate** to render scanlation-styled translated text directly onto the manga art. Luminance-based paper detection automatically matches pure white or dark backgrounds with zero muddy gray artifacts.
5. **Inspect Details**: Tap **Details** to see the raw detected OCR text and translation side-by-side.

> [!IMPORTANT]
> **Beta Device Notice**: Area OCR is in active **Beta** and has been verified on a **OnePlus Ace 5 (Android 15)**. Display scaling, punch-hole cutouts, or non-standard aspect ratios may behave differently across other OEM devices. Community testing and bug reports on GitHub are warmly welcomed!

---

## 🔍 Vibe Search & "For You" Feed

Looking for something specific, but don't know the exact title or author?

* **Natural Language Queries:** Search using descriptive narrative tropes, moods, or themes (e.g., *"wholesome fantasy romance with an overpowered mage"*, *"dark cyberpunk psychological mystery"*).
* **AI "For You" Carousel:** ShinKu analyzes your reading journey velocity, top genres, and reading habits locally to recommend hidden gems matching your unique taste profile.

---

## 🎧 Atmospheric Audio & Mood Lighting

Immerse all your senses while reading:

* **Ambient Genre Sounds:** Subtle background audio soundscapes tailored to genres (gentle rain for slice-of-life, distant thunder for suspense, crackling fire for cozy fantasy).
* **Adaptive Mood Lighting:** Dynamically adjusts display color temperature based on chapter mood and ambient light sensors to reduce eye fatigue during late-night binge sessions.
