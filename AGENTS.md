# ShinKu Development Agents Guide

This document outlines the protocols for development to ensure efficiency and avoid redundant work.

## Build Failure Recovery Protocol
1. **Log Analysis**: Always read the full build log (`build.log`) when a build fails.
2. **Atomic Fixes**: Fix one module's compilation errors at a time (e.g., fix `domain` before `app`).
3. **Constructor Verification**: Before updating `DomainModule.kt`, always `read_file` the target class to verify its constructor parameters.
4. **No Loops**: If a fix causes the same error or a regression, revert and re-analyze the dependency chain.

## Feature Implementation Protocol
1. **ShinKu Grouping**: All new features must be grouped under a "ShinKu" settings section to maintain project identity.
2. **Database Integrity**: Migrations must be numbered sequentially. Always update both the `.sq` files and create a new `.sqm` migration.
3. **UI Consistency**: Use existing components from `eu.kanade.tachiyomi.presentation.core` and `com.shinku.reader.presentation`.

## Rebranding & Compatibility Protocol
1. **Source-API Integrity**: Do NOT move `source-api` or `network` packages out of `eu.kanade.tachiyomi`. These are the binary interface for external extensions.
2. **Backup Compatibility**: Use `@SerialName` with the legacy `eu.kanade.tachiyomi` prefix for all backup models to ensure old backups can be restored.
3. **Logo Usage**: Use `ic_shinku_foreground` for notifications and monochrome icons to avoid the "cropping" effect caused by the background.
4. **Link Hijacking**: Always include legacy `tachiyomi://` and `mihon://` intent filters in `AndroidManifest.xml` to catch external ecosystem links.

## Current Focus
- **Vibe Search**: Expanding natural language search capabilities.
- **Performance Profiles**: Monitoring stability of E-Ink and Low RAM modes.
- **Upstream Sync**: Keeping the `eu.kanade.tachiyomi` bridge classes updated with the latest fixes.
