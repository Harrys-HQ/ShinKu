# Specification: UI Performance & Resource Optimization

## Problem
As ShinKu evolves from Tachiyomi/Mihon, it carries legacy resources and UI patterns that may not be optimal for modern Compose performance. Unstable domain models can cause excessive recomposition, and a cluttered resource folder increases APK size and build times.

## Goals
1. **Compose Stability:** Ensure all domain models used in Composables are treated as `@Stable` or `@Immutable`.
2. **Resource Cleanup:** Identify and remove unused icons, strings, and layouts left over from deprecated features or flavors.
3. **Database Maintenance:** Implement automated `VACUUM` and indexing health checks.
4. **Binary Size:** Reduce the final APK size through better resource management.

## Requirements
- Use the Compose Compiler Metrics to identify unstable classes.
- Ensure no "regression" in UI functionality after resource pruning.
- Database maintenance must run in the background (WorkManager) and not impact user experience.
