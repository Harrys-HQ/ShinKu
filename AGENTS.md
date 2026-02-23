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
3. **UI Consistency**: Use existing components from `tachiyomi.presentation.core` and `eu.kanade.presentation`.

## Current Focus
- **Smart Categorizer**: Ensure user categories are completely hidden when enabled.
- **Dead Source Scanner**: Optimize for background execution.
- **Failed Updates**: Maintain the `last_update_error` flag accurately.
