# Setup Instructions

This document provides instructions for setting up the Muzi Music project for development.

## Prerequisites

- Android Studio (latest version recommended)
- Android SDK (API level as specified in `build.gradle.kts`)
- JDK 17 / 21
- Git

## Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/biikkkuuuu/muzi-music.git
cd muzi-music
```

### 2. Configure Local Properties

Create a `local.properties` file:

```properties
sdk.dir=/path/to/your/android/sdk
```

**Example paths:**

- macOS: `/Users/username/Library/Android/sdk`
- Linux: `/home/username/Android/sdk`
- Windows: `C:\\Users\\username\\AppData\\Local\\Android\\sdk`

### 3. Configure Firebase (Optional)

Firebase is used for analytics and crash reporting. If you want to use these features:

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app with package name `com.biikkkuuuu.muzi` (and `com.biikkkuuuu.muzi.debug` for debug builds)
3. Download the `google-services.json` file
4. Place it in the `app/` directory

**Note:** If you skip Firebase setup, the app will still build and run, but analytics and crash reporting will be disabled.

### 4. Configure Release Signing (Optional)

For release builds, you need to configure signing credentials. Set these as environment variables or in `gradle.properties`:

```bash
# Environment variables
export KEYSTORE_PATH=/path/to/your/keystore.jks
export STORE_PASSWORD=your_store_password
export KEY_ALIAS=your_key_alias
export KEY_PASSWORD=your_key_password
```

Or add to `gradle.properties` (never commit this file):

```properties
KEYSTORE_PATH=/path/to/your/keystore.jks
STORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

### 5. Build the Project

Open the project in Android Studio or build from the command line:

```bash
# Debug build (FOSS)
./gradlew assembleUniversalFossDebug

# Debug build (GMS - Google Cast support)
./gradlew assembleUniversalGmsDebug

# Release build (requires signing configuration)
./gradlew assembleUniversalFossRelease
```

*(On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`)*

### 6. Configure AI Translation (Optional)

Muzi Music supports AI-powered lyrics translation. You can configure this in **Settings -> AI Settings**.

#### Using OpenRouter

1. Get an API Key from [OpenRouter](https://openrouter.ai/).
2. In the app, go to **Settings -> AI Settings**.
3. Set **Provider** to **OpenRouter**.
4. Enter your **API Key**.

---

Developed by [@biikkkuuuu](https://github.com/biikkkuuuu)