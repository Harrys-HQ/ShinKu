# ShinKu Workflow

## Development Process
1. **Upstream Monitoring:** Regularly check `com.shinku.reader.mihon/main` and `sy/master` for updates.
2. **Feature Development:** Implement custom features on top of the base.
3. **Build Verification:** Ensure every change builds successfully using `./gradlew assembleStandardDebug`.

## Syncing with Upstream
- Use `git fetch sy` and `git fetch com.shinku.reader.mihon`.
- Merge or cherry-pick relevant fixes from `com.shinku.reader.mihon/main`.
- Rebase or merge from `sy/master` for SY-specific updates.
