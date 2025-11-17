# Bookmark Manager

**Author:** LCST  
**Version:** 3.0

An Android application for managing browser bookmarks with support for CSV import/export, search, and Material Design 3 with AMOLED mode.

## Features

- ✅ **Bookmark Management**: Add, edit, and delete bookmarks with title and URL
- ✅ **Search**: Search bookmarks by title or URL in real-time
- ✅ **CSV Import/Export**: Import and export your bookmarks in CSV format
- ✅ **HTML Import**: Import bookmarks from Chrome's HTML export file
- ✅ **Material Design 3**: Modern interface with Material Design 3
- ✅ **AMOLED Mode**: Native support for dark mode optimized for AMOLED screens
- ✅ **Share Intent**: Receive URLs shared from other apps and save them as bookmarks
- ✅ **Open in Browser**: Open bookmarks directly in the default system browser
- ✅ **Compatibility**: Supports Android 19 (API level 19, Android 4.4 KitKat) and later versions

## Technologies Used

- **Kotlin**: Programming language
- **Room Database**: Local data persistence
- **Material Design 3**: Modern UI library
- **Coroutines & Flow**: Asynchronous programming
- **ViewBinding**: View binding
- **MVVM Architecture**: Architectural pattern
- **KSP**: Kotlin Symbol Processing for annotation processing

## Project Structure

```
app/
├── src/main/
│   ├── java/com/lcst/bookmarkmanager/
│   │   ├── data/              # Room entities, DAO, Database
│   │   ├── repository/         # Repository pattern
│   │   ├── viewmodel/          # ViewModels
│   │   ├── adapter/            # RecyclerView adapters
│   │   ├── util/               # Utility classes (CSV, HTML)
│   │   └── MainActivity.kt     # Main activity
│   ├── res/
│   │   ├── layout/             # XML layouts
│   │   ├── values/             # Strings, colors, themes
│   │   └── values-night/       # Dark theme resources
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## How to Build

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run the app on a device or emulator

## Requirements

- Android Studio Hedgehog or later
- Min SDK: 19 (Android 4.4 KitKat) - Note: modern libraries (Material Design 3, Lifecycle) require at least API 19
- Target SDK: 34 (Android 14)
- Kotlin 1.9.20+

## Detailed Features

### Bookmark Management
- Add new bookmarks with title and URL
- Edit existing bookmarks
- Delete bookmarks with confirmation

### Search
- Real-time search as you type
- Search both in title and URL
- Case-insensitive

### CSV Import/Export
- Standard CSV format with "Title,URL" header
- Support for quote escaping
- Compatible with other bookmark managers

### HTML Import
- Import bookmarks from Chrome's HTML export file (`bookmarks.html`)
- Automatically parses Chrome bookmark format
- Handles HTML entities and URL normalization

### Share Intent
- Appears as a share target from browsers
- Captures shared URLs and prompts for a title
- Saves shared URLs as new bookmarks

### AMOLED Theme
- Pure black background (#000000) in dark mode
- Optimized for AMOLED screens to save battery
- Automatic transition between light and dark mode

## License

This project is open source and available for personal and commercial use.

