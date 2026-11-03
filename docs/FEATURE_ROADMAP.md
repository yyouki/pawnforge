# PawnForge editor feature roadmap

PawnForge follows familiar Pawn IDE workflows from Pawno and Qawno while using an Android-first interface and its own original implementation and artwork. The feature set below is inspired by the public Qawno project; no proprietary binaries, logos, or copied source implementation are included.

Reference: https://github.com/openmultiplayer/qawno. The upstream project describes syntax highlighting, completion, a natives list, multiple tabs, a compiler output panel, open.mp launch support, a colour picker, and editing helpers.

## Current PawnForge 0.5.0

| Workflow | Current status |
|---|---|
| Open and save Pawn source files | Available through Android Storage Access Framework |
| Project workspace and include directories | Available; copies the selected tree into an app workspace and preserves nested folders |
| Native Pawn compilation | Available offline; produces AMX output and captures compiler diagnostics |
| Line numbers and basic syntax colours | Available |
| Auto-indent after Enter | Available |
| Undo and Redo | Available with bounded history |
| Cut, Copy, Paste, Select All | Available |
| Indent and Unindent selection | Available |
| Find Next and Find Previous | Available with wraparound |
| Pawn callback snippets | Available for common server callbacks |
| Compiler and Output panels | Available and retain their text when switching tabs |
| Adaptive launcher icon | Available on Android 8+; vector fallback on Android 7 |
| Permanent APK signing | Prepared; repository Actions secrets must be configured by the publisher |

## Recommended implementation order

### P0 — Protect code and make editing reliable

1. Add autosave/recovery drafts and a project backup/export action before any source is replaced.
2. Add a real project tree that browses Pawn source, include, config, and nested folders, with a clear current-file path.
3. Add multi-file tabs with per-tab modified markers and restore the last session.
4. Parse compiler output into structured errors and warnings. Tapping a diagnostic should open the relevant file and move the cursor to the reported line/column.
5. Validate compilation and exports on real Android 7, 10, 13, 14, 15, and 16 devices, with gesture and three-button navigation.

### P1 — Reach the useful Qawno/Pawno workflow

1. Add full Find/Replace with case sensitivity, whole-word search, and Replace All.
2. Build autocomplete from the current source, project include files, and known SA-MP/open.mp symbols. Show parameter hints and a compact native/function browser.
3. Add a larger snippet library: enum, player data, dialogs, timers, stock, public, forward, command callbacks, and common include templates.
4. Add brace matching, code folding, a configurable formatter, and keyboard shortcuts for moving selected lines.
5. Add compiler profiles with separate include sets and flags for SA-MP legacy and open.mp.

### P2 — Mobile-specific polish

1. Add a keyboard accessory row for Tab, braces, brackets, quotes, semicolon, and common Pawn tokens.
2. Add a colour picker for 0xRRGGBBAA values.
3. Add dark/light/system themes, adjustable font size, tab width, and editor spacing.
4. Add recent projects, quick-open, project templates, and share/export actions.
5. Add a native reference page with function signatures sourced from the selected local include set.

## Architecture recommendations

- Move editor state/history, project storage, compiler orchestration, and diagnostics parsing out of MainActivity.
- Keep project access through Storage Access Framework and persist the granted URI permissions.
- Treat the selected external project folder as the source of truth. App-private staging and autosave are working copies, not the only copy of user code.
- Keep compiler execution off the UI thread, serialize builds per workspace, and make cancellation explicit.
- Parse compiler output using the actual compiler format instead of relying on fragile text matching.
- Add unit tests for editor state, include resolution, compiler argument construction, and diagnostic parsing; add emulator/device tests for file access and system-bar insets.

## Licensing note

Feature ideas and expected workflows can be reimplemented independently. Do not copy code, icons, screenshots, or bundled assets from Pawno/Qawno unless their applicable licence explicitly permits that use and all notice requirements are followed.
