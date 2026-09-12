<div align="center">
  <img src="assets/muzi_logo.png" alt="Muzi Music Logo" width="120"/>

  <h1>Muzi Music</h1>

  <p><b>A modern Android music app with ad-free streaming, synced lyrics, offline playback, and an intuitive user experience.</b></p>
  <p>Developed & Maintained by <a href="https://github.com/biikkkuuuu">@biikkkuuuu</a></p>
</div>

---

## Overview

Muzi Music delivers a seamless, premium listening experience by leveraging YouTube Music's vast library — without the ads. It adds powerful extras including offline downloads, real-time synchronized lyrics, audio equalization, and environment-aware music recognition.

---

- **Telegram**: [@biikkkuuuuu](https://t.me/biikkkuuuuu)
- **Instagram**: [@biikkkuuuu](https://instagram.com/biikkkuuuu)
- **GitHub**: [biikkkuuuu/muzi-music](https://github.com/biikkkuuuu/muzi-music)

---

## Table of Contents

- [Overview](#overview)
- [Screenshots](#screenshots)
- [Features](#features)
- [Installation & Setup](#installation--setup)
- [Support the Project](#support-the-project)
- [Contributors](#contributors)
- [Special Thanks](#special-thanks)
- [Legal Disclaimer](#legal-disclaimer--terms-of-use)

---

## Screenshots

<div align="left">
  <table style="margin: 0 auto; border-collapse: collapse;">
    <tr>
      <td align="center" style="padding: 15px; border: none;">
        <b>Home Screen</b><br><br>
        <img src="Screenshots/sc_1.png" alt="Home Screen" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 15px; border: none;">
        <b>Music Player</b><br><br>
        <img src="Screenshots/sc_2.png" alt="Music Player" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 15px; border: none;">
        <b>Synchronized Lyrics</b><br><br>
        <img src="Screenshots/sc_3.png" alt="Synchronized Lyrics" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
    </tr>
    <tr>
      <td align="center" style="padding: 15px; border: none;">
        <b>Search & Explore</b><br><br>
        <img src="Screenshots/sc_4.png" alt="Search & Explore" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 15px; border: none;">
        <b>Music Library</b><br><br>
        <img src="Screenshots/sc_5.png" alt="Music Library" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 15px; border: none;">
        <b>Muzi Find (Recognition)</b><br><br>
        <img src="Screenshots/sc_6.png" alt="Muzi Find" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
    </tr>
  </table>
</div>

---

## Features

### Highlights

> - **Data Saver Mode (Beta)** — Automatically reduces data usage during playback for limited connections.
> - **Settings Search Index** — Quickly find and navigate to any settings option instantly.
> - **Expressive Modern UI** — Material 3 Expressive, blur effects, dynamic Monet color palettes, and fluid animations.
> - **Import from Spotify** — Bring your playlists and tracks over with ease.
> - **Listen Together** — Sync music in real time across devices.
> - **Podcast Support** — Listen to podcasts alongside your music library.
> - **Local Media Support** — Play music files stored directly on your device.
> - **Dynamic Island Support** — Enhanced playback notifications on supported Android devices.

<br>

<details>
<summary><b>Streaming & Playback</b></summary>
<br>

- **Ad-Free** — Stream without any interruptions.
- **Data Saver Mode** — Reduce data consumption when streaming on cellular networks.
- **Seamless Playback** — Switch effortlessly between audio-only and video modes.
- **Background Playback** — Listen while using other apps or with the screen off.
- **Offline Mode** — Download tracks, albums, and playlists via a dedicated download manager.
- **Crossfade** — Smooth transitions between tracks.
- **Canvas Animations** — Visual animations while playing music.

</details>

<details>
<summary><b>Discovery & Music Intelligence</b></summary>
<br>

- **Muzi Find** — Identify songs playing around you using advanced audio recognition.
- **Muzi Brain** — An intelligent, on-device engine that analyzes your listening momentum and auto-injects perfectly aligned tracks into your queue.
- **Smart Recommendations** — Personalized suggestions based on your listening history.
- **Comprehensive Browsing** — Explore Charts, Podcasts, Moods, and Genres.

</details>

<details>
<summary><b>Lyrics</b></summary>
<br>

- **Multiple Lyric Animations** — Choose from various lyric display styles.
- **Word-by-Word Lyrics** — Precise per-word synchronization.
- **Lyrics+** — High-accuracy lyrics provider.
- **AI Translation** — Built-in translation integration for lyrics in any language.

</details>

<details>
<summary><b>Integrations</b></summary>
<br>

- **Music Sharing via Odesli** — Share songs as Song.link for cross-platform listening.
- **Set as Ringtone** — Directly set any song as your device ringtone.
- **Discord Rich Presence** — Broadcast your currently playing tracks to Discord.

</details>

<details>
<summary><b>Smart Playback</b></summary>
<br>

- **Pause on Mute** — Auto-pause when your device is muted.
- **Resume on Bluetooth** — Playback resumes when headphones or earbuds reconnect.

</details>

<details>
<summary><b>Customization</b></summary>
<br>

- **UI Density Scale** — Adjust interface spacing to your preference.
- **High Refresh Rate Support** — Smoother UI and animations on supported displays.
- **Hide Player Thumbnail** — Keep the player minimal without album art.
- **Crop Album Art** — Adjust album art display to fit your style.
- **Hide Video Songs** — Filter out video content from your feed.
- **Hide YouTube Shorts** — Keep Shorts out of your music browsing.

</details>

---

## Installation & Setup

### Android Installation

Download the latest pre-compiled APK from the [Releases Page](https://github.com/biikkkuuuu/muzi-music/releases/latest).

<details>
<summary><b>Building from Source</b></summary>
<br>

1. **Clone the Repository**

   ```bash
   git clone https://github.com/biikkkuuuu/muzi-music.git
   cd muzi-music
   ```

2. **Configure Android SDK**
   Create a `local.properties` file:

   ```bash
   echo "sdk.dir=/path/to/your/android/sdk" > local.properties
   ```

   _(For detailed paths on Windows/macOS/Linux, refer to [SETUP.md](SETUP.md))_

3. **Firebase Configuration (Optional)**
   Firebase is used for analytics and crash reporting. See instructions in [SETUP.md](SETUP.md#3-configure-firebase-optional) for adding your `google-services.json`.

4. **Build the Application**
   Muzi Music has build variants for FOSS and GMS (with Google Cast support).

   - To build the **FOSS** Universal Debug variant:
     ```bash
     ./gradlew assembleUniversalFossDebug
     ```
   - To build the **GMS** Universal Debug variant:
     ```bash
     ./gradlew assembleUniversalGmsDebug
     ```

   _(For release builds and details, refer to [SETUP.md](SETUP.md))_

</details>

---

## Support the Project

If Muzi Music has been useful to you, star the repository and follow the developer!

- **GitHub**: [biikkkuuuu](https://github.com/biikkkuuuu)
- **Telegram**: [@biikkkuuuuu](https://t.me/biikkkuuuuu)
- **Instagram**: [@biikkkuuuu](https://instagram.com/biikkkuuuu)

---

## Contributors

Without the support of this incredible open-source community, none of this would be possible. Thank you to everyone who has contributed to Muzi Music!

<!-- readme: contributors -start -->
<table>
<tr><td align="center"><a href="https://github.com/biikkkuuuu"><img src="https://avatars.githubusercontent.com/u/210472672?v=4" width="60" height="60" /></a></td></tr>
</table>
<!-- readme: contributors -end -->

---

## Special Thanks

Muzi Music stands on the shoulders of several excellent open-source projects. Sincere thanks to:

| Project | Description |
| :--- | :--- |
| **[Metrolist](https://github.com/MetrolistGroup/Metrolist)** & **[Vivi Music](https://github.com/vivizzz007/vivi-music)** | Foundational inspiration and architecture reference |
| **[ArchiveTune](https://github.com/koiverse/ArchiveTune)** | Material You UI inspiration |
| **[Better Lyrics](https://better-lyrics.boidu.dev/)** | Lyrics enhancement and synchronization |
| **[SimpMusic](https://github.com/maxrave-dev/SimpMusic)** | Lyrics implementation reference |
| **[Music Recognizer](https://github.com/aleksey-saenko/MusicRecognizer)** | Audio recognition (Muzi Find) |
| **[BravePipe](https://github.com/bravepipeproject/BravePipe)** | Decryption handling and backup playback engine |

---

## Legal Disclaimer & Terms of Use

### 1. 100% Free, Open-Source & Strictly Non-Commercial

Muzi Music is a fully open-source project (FOSS) created purely for educational purposes and personal use. We do not sell this application, nor do we monetize it in any way. There are no advertisements, no premium features, no subscriptions, and no hidden fees within the app. This project has absolutely no commercial value or financial intent.

### 2. A Custom Browser with Content Filtering

Muzi Music acts strictly as a specialized, third-party web browser and client. It simply parses the publicly available website content and APIs of YouTube and YouTube Music, rendering them in a custom user interface. The ad-free experience it provides is fundamentally no different from using a standard web browser (like Chrome, Firefox, or Brave) equipped with a common ad-blocking extension (such as uBlock Origin).

### 3. Support Content Creators

We deeply respect the hard work of artists, musicians, and content creators. We strongly encourage all users to subscribe to [YouTube Premium](https://www.youtube.com/premium). Purchasing a Premium subscription is the best way to financially support the creators you listen to and ensure the continued growth of the platform. Muzi Music is built as a proof-of-concept for developers and enthusiasts, not to harm creators' revenues.

### 4. No Hosting of Copyrighted Material

We do not host, upload, distribute, or store any audio, video, or copyrighted media files on our own servers. All content accessed through this application is stored entirely on Google's/YouTube's servers and remains the property of their respective copyright owners. The app merely acts as a conduit to stream publicly accessible links.

### 5. User Responsibility & Legal Contact

The software is provided "AS IS", without warranty of any kind. The developers of Muzi Music do not encourage or condone piracy. Users are solely responsible for ensuring their usage of this app complies with their local copyright laws and the Terms of Service of the platforms they access.

Because we do not host any media files, we cannot process DMCA takedown requests for audio or video content. For inquiries regarding the open-source code itself, please contact via GitHub: [biikkkuuuu](https://github.com/biikkkuuuu).

---

<div align="center">
  <p>Licensed under <a href="LICENSE">GPL-3.0</a></p>
</div>
