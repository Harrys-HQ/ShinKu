# ShinKu Changelog

## 2.0.0 (2026-02-19)
- **New:** Rebranding to ShinKu 2.0.0 (Crimson).
- **New:** AI-powered **Vibe Search** with Google Gemini integration.
- **New:** **Reading Journey** statistics card in the More tab.
- **New:** **Dynamic Theme** overhaul with immersive cover-based coloring.
- **New:** **Smart Categorizer** for automated library organization.
- **New:** Replace Google Drive and SyncYomi with **Dropbox integration**.
- **New:** Add manual token entry for Dropbox sync.
- **New:** Transition to **Feed Menu** replacing the old Latest tab.
- **Fix:** Fixed library update progress notification being hidden by restrictions.
- **Fix:** Fixed stuck progress bar during library updates when excluded sources are present.
- **Fix:** Ensure manual updates bypass auto-update restrictions.
- **Improved:** Consolidate library update warning thresholds.

## 1.12.0 (2025-05-11)
- **Based on Mihon stable 0.18.0 (from 0.17.0)**
- Use Complete Category for sync completion (Thanks @lord-ne)
- Maintain sort order when receiving chapters from sync (Thanks @Lolle2000la)
- Don't sync when not connected to a network (Thanks @NGB-Was-Taken)
- Add QR code scan button for sync API key (Thanks @65-7a)
- Update NHentai Subdomain for cdn (Thanks @BrutuZ)
- Use the NHentai t1 cdn subdomain (Thanks @cfouche3005)
- Fix crash with migration list screen going into the background
- Improve recommendation screens for multi-recommendations (Thanks @timschneeb)
- Add global search shortcut to SmartSearch for merge (Thanks @timschneeb)
- Add notifications to the gallery updater
- Fix the gallery updater never updating in the background
- Update E-Hentai tags list
- Populate Author field and clear Description on a couple sources (Thanks @BrutuZ)
- Use Mangadex tracker links to associate mangas automatically with trackers (Thanks @timschneeb)
- Fix Mangadex alt title being removed by cleanDescription (Thanks @spicemace)

## 1.11.0 (2024-10-27)
- **Based on Mihon stable 0.17.0 (from 0.16.4)**
- **Add Cross-Device Sync (Thanks @kaiserbh)**
- Add mature to the lewd tag filter (Thanks @gelionexists)
- Add reset custom manga info (Thanks @LetrixZ)
- Add multiple tags at once in edit manga info (Thanks @NGB-Was-Taken)
- Fix sources loading too late for some screens
- Show Downloaded stage in reader chapter list (Thanks @NGB-Was-Taken)
- Fix Anilist
- Fix MAL
- Add an option to only show entries with new chapters (Thanks @timschneeb)
- Fix Merged Source chapters not showing source name
- Use DownloadChapters merged manga setting (Thanks @cuong-tran)
- Show local chapters as downloaded in Merged Manga (Thanks @NGB-Was-Taken)
- Fix ExHentai Page List errors
- Improve E-H/Exh chapter downloading
- Make manga page preview rows configurable (Thanks @LetrixZ)
- Use more of Mangadex Extension settings (Thanks @NGB-Was-Taken)
- Improve Encrypted Downloads (Thanks @Shamicen)
- Delete duplicate chapters when they are marked as read (Thanks @NGB-Was-Taken)
