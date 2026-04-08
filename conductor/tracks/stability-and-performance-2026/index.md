# Track: Stability & Performance 2026

## Status
- [x] Fix XLog initialization race condition in `App.kt`
- [x] Add missing database indexes to `chapters.sq` and `mangas_categories.sq` (and `merged.sq`)
- [x] Verify build stability with `assembleDevRelease`

## Description
Address identified stability issues from crash logs and optimize database performance by adding missing indexes.

## Files
- [Implementation Plan](./plan.md)
- [App.kt](../../../app/src/main/java/com/shinku/reader/App.kt)
- [chapters.sq](../../../data/src/main/sqldelight/com/shinku/reader/data/chapters.sq)
- [mangas_categories.sq](../../../data/src/main/sqldelight/com/shinku/reader/data/mangas_categories.sq)
