# Library refinement — implementation review

Implements the Library portion of the approved local shinku-phase1-mockup-v1.png. No new image generation or external design draft was requested or used. This is a native Compose implementation using existing MaterialTheme roles and Sanctuary tokens.

Changes:

- Continue Reading carousel keeps its existing recent-title data and navigation. Cover uses the existing medium Sanctuary shape; title permits up to three lines, progress permits two. Resume and Details both have visible labels, minimum 48 dp height, and wrap into another action row if needed. Labels use resource strings; no reading data or preferences were rewritten.
- Comfortable Grid retains badges, selection and continue-reading interactions. It now supplies a theme-colored caption surface and three title lines, keeping titles separate from cover art and rows aligned. Other shared comfortable-grid consumers retain their old default presentation. Compact and cover-only modes remain available.
- Library grid gutters have slightly larger horizontal insets. Category pills use selectable-tab semantics, at least 48 dp height, and bring the selected category into view when the pager changes.
- Home provides the measured floating-dock bottom inset to Library, which adds clearance at the end of its scroll content. Content can pass behind the glass dock and the final row can scroll above it. Library selection actions and tablet navigation remain existing components.

Current status: Library and glass-dock changes are implemented, built, and installed on the phone. A direct screenshot confirmed the frosted dock and labeled Continue Reading actions on the current dark lavender theme. Comfortable Grid, large text, light themes, scrolling and final-row clearance still need device review. The owner confirmed the phone is unavailable for the resumed local verification session; no device checks are claimed for that session.

Review on the next connected phone session: default/long titles, carousel Resume vs Details, a large font scale (button wrapping), dark/light/AMOLED or dynamic palettes, portrait/landscape, category swipe and selected-pill visibility, search and multiselect, final grid row above the dock, dock auto-hide, and tablet rail. Compare the actual output to the approved mockup before expanding the styling into manga details and More/settings. The board is a visual direction, not a pixel-perfect specification across all window sizes.

No manga detail, settings, reader, source, backup or database behavior was redesigned in this Library UI batch. Existing optimization changes remain in the working tree. No publication or release is implied by a local preview build.

Initial Library build: compileDevDebugKotlin and assembleDevRelease passed. All five APK signatures verified; build times/checksums in build/library-redesign-checksums.json. The subsequent glass-dock build and installation supersede this initial artifact; see the correction history below.


## Dock review correction

The owner observed an opaque black background where a glass-like dock was expected. The original shared NavigationBar used a nearly opaque surface and a dark full-width scrim. The Library pass also reserved the entire bottom region outside the scroll viewport, leaving no content behind the dock.

Correction: use a real captured-content backdrop effect (pinned dev.chrisbanes.haze:haze 1.6.10, compatible with the project's existing Compose dependency floor), remove the full-width dark scrim, draw the dock surface transparently over a theme-tinted blur with foreground icons/labels kept sharp, and move Library's safe clearance to the end of its scroll content through LocalHomeBottomInset. The last row can scroll above the dock while content can pass behind it. Existing home tabs share the dock appearance; tablet rail remains existing behavior. Haze supplies a readable tint fallback when blur is unavailable. Theme colors remain MaterialTheme roles; no fixed black/purple surface was introduced.

API reference consulted: https://chrisbanes.github.io/haze/1.5.4/usage/ and the pinned 1.6.10 HazeStyle source at https://github.com/chrisbanes/haze/blob/1.6.10/haze/src/commonMain/kotlin/dev/chrisbanes/haze/HazeStyle.kt . Version choice avoids requiring the newer Compose baseline of later releases.

The initial phone screenshot used Compact Grid (cover-overlay mode); Comfortable Grid must be selected for the new three-line caption presentation. No claim of final glass visual approval until the corrected build is installed and inspected.

Glass build history: Kotlin compilation and optimized devRelease assembly passed; all five APK signatures verified (build/glass-dock-checksums.json). The first installation attempt failed because the phone disconnected; the later successful installation is recorded below.

Installation update: owner reconnected; verified corrected ARM64 APK was installed successfully. Direct screenshot confirmed frosted dock with underlying cover colors and crisp icons/labels on the current dark lavender theme. Caption mode/scroll checks were interrupted when the phone dozed and locked; they remain pending. UIAutomator stale-XML handling in the local helper was corrected.
