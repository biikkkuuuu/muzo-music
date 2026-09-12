# Muzi Music Releases

This document tracks all available releases for Muzi Music. 

## [v1.2.2] - 2026-08-28 (Latest)
[Download on GitHub](https://github.com/biikkkuuuu/muzi-music/releases/tag/v1.2.2)

**Bug Fixes**
- Fixed a crash that occurred when adding a song to a playlist, album, or artist before it was fully loaded.
- Fixed a crash caused by outdated saved settings after an app update; the app now falls back to a safe default instead of crashing.
- Fixed Spotify login issues where signing in with Google, Apple, or Facebook could show a black screen or fail to complete.
- Fixed the "Update Available" dialog not matching the app's overall theme and styling.

**Design Improvements**
- Updated input fields and dialog buttons (including in Spotify Import) to use a more rounded, modern look consistent with Material You design.

**Other Changes**
- Updated select app components to their latest stable versions for improved reliability.

## [v1.2.1] - 2026-08-28
[Download on GitHub](https://github.com/biikkkuuuu/muzi-music/releases/tag/v1.2.1)

Initial release of the updated Muzi Music repository.

---

## 📋 Pull Request & Release Note Guidelines

**ATTENTION CONTRIBUTORS:** To maintain a clean and standardized changelog, all community contributions added to this file MUST strictly follow this format:

`- \`<type>(<scope>): <summary>\` ([#PR_NUMBER](URL)) by @username`

- **PR Titles** must follow [Conventional Commits](https://www.conventionalcommits.org/).
- **Descriptions** must be clear, concise, and professional.
- PRs that do not follow this strict formatting will **not** be merged.
