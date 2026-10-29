# WebTool Runner

A resilient, offline-first Android host platform engineered to execute, edit, and sandbox standalone web applications and developer micro-tools locally. It provides a native bridge ecosystem that empowers web-based utilities with hardware acceleration, file system management, system clipboard access, and calendar scheduling without cloud reliance.

## Core Features

- **Sandboxed Web Runtime**: High-performance internal WebView container featuring hardware acceleration, dynamic viewport switching (responsive mobile & 1280px desktop emulation), custom web clients, and console diagnostics.
- **Embedded Offline Tool Suite**:
  - **HAR Reader**: Advanced multi-archive HTTP log analyzer with tree diagrams, native format-first body inspection, Arabic/Unicode search, and JSON table unrolling with XLSX spreadsheet export.
  - **CipherVault**: Offline cryptographic vault offering AES-256-GCM authenticated encryption/decryption and SHA-256/SHA-512 hashing for arbitrary files and text payloads.
  - **Dev Toolbox**: Zero-latency offline engineering toolkit featuring structured JSON formatting, Base64/URL encoding, UUID v4 generation, and casing transformers.
  - **Photo Scheduler**: Task and deadline management utility combining photo documentation with Android Calendar synchronization and ICS generation.
- **Full Project Lifecycle & Editor**: Built-in multi-file code editor with live syntax snippets, dynamic file explorer, SAF (Storage Access Framework) document import/export, and single-click ZIP archive packaging.
- **Bi-Directional Native Bridge**: Modern JavaScript-to-Android interop for native toasts, clipboard operations, haptic feedback, external browser intents, and streaming file downloads.
- **Edge-to-Edge Material Design 3**: Fully reactive Jetpack Compose interface with fluid layout adaptation, custom accent palette system, and dynamic light/dark theming.

## Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **Architecture**: Modern Android Architecture (MVVM, Clean Architecture, Unidirectional Data Flow)
- **UI Framework**: Jetpack Compose, Material Design 3, Jetpack Navigation Compose
- **Persistence**: Room Database (SQLite) with multi-file directory hoisting and storage management
- **Concurrency & Streams**: Kotlin Coroutines, StateFlow, SharedFlow
- **Web Engine**: Android WebView, WebChromeClient, WebViewClient with safe IPC bridge

```
com.example
├── data
│   ├── local        # Room Database & ToolDao
│   ├── model        # Tool entity definitions
│   └── repository   # ToolRepository & data orchestration
├── engine           # WebView clients, download manager, ZIP extractor & bridge
├── ui
│   ├── components   # Reusable UI widgets & AppFooter
│   ├── screens
│   │   ├── dashboard  # Grid viewer, search & tool launching
│   │   ├── editor     # In-app file tree explorer & source code editor
│   │   ├── runner     # WebView execution screen & native bridge host
│   │   └── settings   # Appearance & backup management dialog
│   └── theme        # Material 3 colors, glassmorphism & typography
└── MainActivity.kt  # Compose entry point & navigation graph
```

## Prerequisites & Setup

### Prerequisites

- **Android Studio**: Jellyfish (2023.3.1) or newer
- **JDK**: OpenJDK 17 or higher
- **Android SDK**: Compile SDK 36, Target SDK 36, Minimum SDK 24 (Android 7.0+)
- **Build System**: Gradle 8.x with Kotlin DSL

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/FAYSAL-Q8/webtool-runner.git
   cd webtool-runner
   ```
2. Copy the environment configuration template:
   ```bash
   cp .env.example .env
   ```
3. Open the project directory in Android Studio.
4. Allow Gradle to synchronize project dependencies.
5. Select a connected physical device or Android Virtual Device (AVD).
6. Build and execute:
   ```bash
   ./gradlew installDebug
   ```

## Usage & Execution Guide

1. **Launching Tools**: Open the application dashboard to browse the built-in micro-tools. Tap any card to launch the tool directly in the offline runner.
2. **Desktop Mode Emulation**: While inside the runner, tap the desktop icon in the top toolbar to switch to a 1280px desktop viewport for inspecting desktop-oriented web interfaces.
3. **Importing Tools**: Tap the **+** action button on the dashboard or editor to import external web tools packaged either as single `.html` files or `.zip` archives containing assets, scripts, and stylesheets.
4. **Exporting Projects**: Use the options menu on any tool card to export the complete multi-file project as a portable `.zip` archive to device storage.
5. **Editing Code**: Tap the code editor icon on any tool card to modify source code in real time, add files, insert bridge snippets, and preview changes instantly.

## Author

Developed by **Faysal Alibrahim**
- GitHub: [https://github.com/FAYSAL-Q8](https://github.com/FAYSAL-Q8)

## License

This project is licensed under the [MIT License](LICENSE).
