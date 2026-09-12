# MUZI Architecture & Frontend Design Specification (DESIGN.md)

This document provides a comprehensive technical overview and design specification of the actual frontend implementation and backend integration for **MUZI** (built with Echo UI/UX on top of MUZI backend and InnerTube data services).

---

## 1. System & Architectural Overview

```
               ┌──────────────────────────────────────────────┐
               │         Echo UI / UX Layer (Jetpack Compose) │
               │  - Home, Search, Library, Explore, Player    │
               │  - Material 3 Expressive + iOS Blur/Aesthetic│
               └──────────────────────┬───────────────────────┘
                                      │
               ┌──────────────────────▼───────────────────────┐
               │    Frontend ViewModels & State Management    │
               │  - HomeViewModel, SearchViewModel, etc.      │
               │  - MediaSession & ExoPlayer Playback Queue   │
               └──────────────────────┬───────────────────────┘
                                      │
               ┌──────────────────────▼───────────────────────┐
               │           MUZI / InnerTube API Layer         │
               │  - InnerTube Client / YouTube Engine         │
               │  - NewPipe / PipePipe Video Stream Extractor │
               │  - Room Database (Local Caching & History)   │
               └──────────────────────────────────────────────┘
```

The application preserves the rich, fluid frontend of Echo Music with seamless bindings to MUZI's backend service layer (`innertube`, `core`, and `playback` engines).

---

## 2. Screen Inventory & Hierarchy

1. **Home Screen (`HomeScreen.kt`)**
   - Header with customizable greetings, profile avatar, quick filters, and action icons.
   - Quick Picks Carousel / Speed Dial grid for rapid track resume.
   - Dynamic Music Shelves (Trending Hindi, New Releases, Monsoon Acoustic, Party Dance, etc.).
   - Moods & Genres expressive starburst and pill grid.
   - History & Frequent Artists shelf.

2. **Search Screen (`SearchScreen.kt`)**
   - Search input bar with animated clear/filter actions.
   - Live search suggestions and history chips.
   - Multi-tab search results: Songs, Videos, Albums, Artists, Featured Playlists, Community Playlists.
   - Online query debounce and instant playback launch.

3. **Music Player (`PlayerBottomSheet.kt` & Full Player)**
   - **Mini Player:** Floating or docked pill with album art thumbnail, scrolling marquee song title, artist subtitle, play/pause toggle, and skip controls.
   - **Full Player:** Immersive sheet with animated canvas/artwork background, adaptive dynamic Monet coloring, waveform/squiggly seekbar, lyrics view (synced & plain), playback speed/pitch controller, sleep timer, and sound equalizer shortcuts.
   - **Queue Sheet (`Queue.kt`):** Draggable reordering, swipe-to-dismiss, endless auto-radio loader, and upcoming playback list.

4. **Artist Screen (`ArtistScreen.kt`)**
   - Artist banner artwork header with sticky app-bar collapsing animation.
   - Top songs, Discography (Albums, EPs, Singles), Similar Artists, and YouTube Channel links.

5. **Album & Playlist Screen (`AlbumScreen.kt`, `PlaylistScreen.kt`)**
   - Dynamic header with hero cover art, title, release year, tracks count, duration, and download/share actions.
   - Tracklist items displaying track index, explicit badges, duration, and overflow context menu.

6. **Library Screen (`LibraryScreen.kt`)**
   - Tabs: Playlists, Liked Songs, Downloaded Tracks, Artists, and Local Media.
   - Offline playback status indicator and storage management.

7. **Settings & Utilities**
   - Settings Screen (`SettingsScreen.kt`) with categorized groups: Appearance, Player & Audio, Content & Location, Cache & Storage, Backup & Restore, and About.
   - Equalizer & Audio Device Switcher Bottom Sheet (`AudioDeviceBottomSheet.kt`).

---

## 3. UI Component System & Tokens

### Color & Theming
- **Dynamic Theming:** Dynamic color tokens derived from `MaterialTheme.colorScheme` with support for Material You Monet pastel palettes and Pure AMOLED Black (`pureBlack`) mode.
- **Translucency & Glassmorphism:** Heavy usage of `surfaceVariant.copy(alpha = 0.3f)` or `surface.copy(alpha = 0.65f)` with blur backing for glass-like container surfaces.
- **Accent Elevation:** Flat elevation (0.dp) paired with translucent borders and soft ambient shadows.

### Shapes & Typography
- **Corner Radii:** Prominent rounded corners: `RoundedCornerShape(24.dp)` to `RoundedCornerShape(28.dp)` for cards, and `CircleShape` for floating controls.
- **Typography:** Uses Google Sans Flex typography hierarchy:
  - Header Titles: `titleLarge` / `headlineSmall` (bold/semi-bold, 20sp–24sp).
  - List Items & Subtitles: `bodyMedium` (14sp) and `labelSmall` (11sp).

---

## 4. Navigation & Gesture Interactions

- **Bottom Navigation:** Custom floating navigation bar (`AppFloatingNavBar`) supporting automatic show/hide on scroll via `rememberFloatingTabBarScrollConnection`.
- **Pull to Refresh:** Integrated pull-to-refresh swipe interaction triggering asynchronous backend queries.
- **Swipe Gestures:** Swipe left/right on Mini Player to skip tracks; swipe down on Full Player to minimize.
- **Bottom Sheets & Dialogs:** Modal bottom sheets for playlist addition, audio output routing, sleep timer configuration, and ringtone trimming.

---

## 5. API & Data Integration Layer

- **Backend Provider:** InnerTube API (`com.music.innertube.YouTube`) powering:
  - `YouTube.search(query, filter)`
  - `YouTube.next(WatchEndpoint)` for endless auto-radio and queue discovery.
  - `YouTube.album(browseId)` and `YouTube.artist(browseId)`.
  - `YouTube.playlist(browseId)` and `YouTube.browse(browseId)`.
- **Media Extraction:** High-performance stream resolution via NewPipe / PipePipe extractors with PoToken authentication support.
- **Local Persistence:** Room Database for caching search queries, favorite tracks, offline downloads, and play history.

---

## 6. Build & Packaging Verification

- **Build Target:** Android API 36 (Compile & Target SDK), Min SDK 26.
- **Build Status:** Successfully compiled and packaged via `./gradlew assembleUniversalFossDebug`.
- **Artifact:** `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk` (112 MB).
