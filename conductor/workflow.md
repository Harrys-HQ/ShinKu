# ShinKu Workflow

## Conductor System
ShinKu uses the `conductor/` directory to manage complex refactors and new features. This ensures that goals are documented and verified systematically.

### Track Structure
Each work track is located in `conductor/tracks/<track-id>/` and contains:
- `index.md`: Overview and document links.
- `spec.md`: Detailed requirements and the "why" behind the change.
- `plan.md`: Step-by-step implementation phases and checklist.

### Adding a New Track
1.  **Register:** Add the track entry to `conductor/tracks.md`.
2.  **Initialize:** Create the directory and the three core files.
3.  **Index:** Link the track in `conductor/index.md`.

## Development Process
1. **Upstream Monitoring:** Regularly check `com.shinku.reader.mihon/main` and `sy/master` for updates.
2. **Feature Development:** Implement custom features on top of the base.
3. **Build Verification:** MANDATORY build check using `./gradlew assembleDevRelease`.

## Syncing with Upstream
- Use `git fetch sy` and `git fetch com.shinku.reader.mihon`.
- Merge or cherry-pick relevant fixes from `com.shinku.reader.mihon/main`.
- Rebase or merge from `sy/master` for SY-specific updates.

