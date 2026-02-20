# wRadio 📻

A modern, robust, and lightweight Android Radio Player built with **Jetpack Compose** and **Clean Architecture**.

wRadio allows users to explore thousands of stations via the Radio Browser API, manage their favorite list locally, and listen to high-quality streams with real-time metadata support.

## 🛠 Tech Stack & Libraries

*   **Language:** [Kotlin](https://kotlinlang.org/)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material3)
*   **Architecture:** Clean Architecture (Data, Domain, UI) + MVVM
*   **Dependency Injection:** [Hilt](https://dagger.dev/hilt/)
*   **Asynchronous:** Coroutines & Flow
*   **Network:**
    *   [Retrofit](https://square.github.io/retrofit/) & [Moshi](https://github.com/square/moshi) (API)
    *   [OkHttp](https://square.github.io/okhttp/) (Custom Interceptors for ICY Metadata)
*   **Database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite abstraction)
*   **Media Engine:** [Media3 / ExoPlayer](https://developer.android.com/media/media3)
*   **Image Loading:** [Coil](https://coil-kt.github.io/coil/)

## ✨ Key Features (v1.0)

*   **Smart Exploration:** Search stations by name, tag, or country using the [Radio Browser API](https://api.radio-browser.info/).
*   **Robust Playback:** powered by Media3/ExoPlayer with custom `OkHttpDataSource` to handle ICY/Shoutcast streams reliably.
*   **Real-time Metadata:** Decodes Stream Titles (Artist - Song) on the fly for supported stations.
*   **Local Management:** Save, edit, and delete custom stations locally (Room Database).
*   **Anti-Zapping Logic:** Smart statistics tracking that filters out short listening sessions ("zapping") to ensure accurate play time data.
*   **Deterministic UI:** Custom `StationLogo` component that generates consistent, vibrant branding for stations without logos based on their UUID.
*   **User-Friendly Error Handling:** Translated technical errors into human-readable messages (Network, Stream Offline, etc.).

## 🏗 Architecture

The project follows strict **Clean Architecture** principles:

1.  **Domain Layer:** Contains Business Logic, Use Cases, and Repository Interfaces. Pure Kotlin, no Android dependencies.
2.  **Data Layer:** Implements Repositories, manages Data Sources (API/DB), and handles Mappers.
3.  **UI Layer:** Jetpack Compose screens and ViewModels.

## 🚀 Roadmap (v2.0)

*   [ ] **Stream Validation:** Auto-check connectivity (Ping/Head) before saving.
*   [ ] **Smart Sorting:** Sort "My Radios" by Recently Added, Most Played, A-Z.
*   [ ] **Fullscreen Player:** Immersive UI with larger artwork.
*   [ ] **Queue Management:** `gotoNext()` / `gotoPrevious()` logic.
*   [ ] **Import/Export:** JSON backup for local stations.
*   [ ] **Local Search:** Filter within "My Radios".

## 🔧 Setup & Build

1.  Clone the repository:
    ```bash
    git clone https://github.com/MasterOfPuppets/wradio.git
    ```
2.  Open in **Android Studio** (Ladybug or newer).
3.  Sync Gradle.
4.  Build & Run.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.