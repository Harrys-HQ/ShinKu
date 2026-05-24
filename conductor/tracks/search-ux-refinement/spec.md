# Specification: Search UX Refinement

## Problem
The current global search implementation feels static and somewhat fragmented. The transition between entering a query and seeing results lacks visual feedback (beyond a simple progress bar), and the filter chips are placed in a way that feels disconnected from the main search action.

## Goals
1. **Unified Toolbar:** Integrate the search bar and filter chips into a cohesive, scroll-responsive unit.
2. **Dynamic Loading:** Use shimmer effects and staggered animations for search results.
3. **Improved Interactivity:** Better focus management and keyboard handling.
4. **Visual Polish:** Use modern Compose patterns for chip elevation and transition.

## Requirements
- Support scroll-to-collapse behavior for the entire search header.
- Maintain existing image search and source filtering functionality.
- Performance: Ensure that animations do not drop frames even with many search results.
