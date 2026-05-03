# AGENTS.md

## Purpose
- This is a single-module Android app (`:app`) for streaming internet radio with local station persistence and foreground playback.
- Stack in use: Kotlin, Compose, Hilt, Room, Retrofit/Moshi, DataStore, Media3 (`README.md`, `app/build.gradle.kts`).

## Architecture That Matters
- Entry points: `WRadioApplication.kt` (Hilt app), `MainActivity.kt` (Compose root), `ui/navigation/MainScreen.kt` (NavHost + bottom nav + mini-player).
- Layering is practical Clean Architecture:
  - `domain/`: contracts and models (`domain/repository/StationRepository.kt`, `domain/model/Station.kt`).
  - `data/`: Room + Retrofit + repository implementations (`data/repository/StationRepositoryImpl.kt`).
  - `ui/`: screens + ViewModels using `StateFlow` and `hiltViewModel()`.
  - `service/`: playback service and controller bridge (`service/RadioService.kt`, `service/connection/WRadioPlayerClient.kt`).
- Shared playback state is centralized in `WRadioPlayerClient.playerState`; UI reads it through `MainViewModel` and screen ViewModels call client commands.

## Core Data/Control Flows
- Explore search does two remote queries in parallel (name + tag), dedupes by UUID, then maps into `ExploreStationWrapper` status against local DB (`ui/explore/ExploreViewModel.kt`).
- Import flow: explore -> `StationRepository.saveStation()` -> if station is currently previewing, playlist context is rebuilt (`updatePlaylistContext`).
- Home flow: selecting a station updates `lastPlayed`, then starts playlist playback from current sorted local list (`ui/home/HomeViewModel.kt`).
- Session analytics are in `RadioService`: listens to play/stop transitions, ignores sessions under 60s, rounds minutes, updates `totalPlayTime` (`MIN_LISTEN_THRESHOLD`).

## Project-Specific Conventions
- UI state pattern: sealed UI states (`ui/explore/ExploreUiState.kt`) + `stateIn(SharingStarted.WhileSubscribed(5000))` in ViewModels.
- Station logos must stay deterministic when favicon is missing; keep `StationLogo` UUID-based color fallback (`ui/common/StationLogo.kt`).
- Remote station names are normalized (`trim().take(80)`) in repository mapping; preserve this behavior when changing DTO/domain mapping.
- `StationEntity` stores tags as comma-separated string; conversions live in `data/local/entity/StationEntity.kt`.

## Integration/Dependency Notes
- Radio Browser endpoint is pinned to `https://de1.api.radio-browser.info/` (`di/NetworkModule.kt`).
- Media pipeline uses `OkHttpDataSource` with `Icy-MetaData: 1` and URL scheme cleanup (`icy://` -> `http://`) for stream compatibility (`di/ServiceModule.kt`, `WRadioPlayerClient.kt`).
- Buffer size is read once when building the ExoPlayer singleton (`runBlocking` in `ServiceModule`), so settings intentionally force app restart (`ui/settings/SettingsScreen.kt`).
- `AndroidManifest.xml` enables `usesCleartextTraffic="true"`; do not remove without validating stream compatibility.

## Change Checklist (Do This Before PR)
- If you change `Station` fields: update `domain/model/Station.kt`, `data/local/entity/StationEntity.kt`, repository mappers, and Room schema version in `data/local/WRadioDatabase.kt`.
- If you change playback behavior: validate both `RadioService` listener logic and `WRadioPlayerClient` state/error mapping.
- If you add dependencies: register via `gradle/libs.versions.toml` and consume via aliases in `app/build.gradle.kts`.

## Build/Test/Debug Commands (verified tasks)
- Windows wrapper commands from repo root:
  - `./gradlew.bat app:assembleDebug`
  - `./gradlew.bat app:testDebugUnitTest`
  - `./gradlew.bat app:lintDebug`
  - `./gradlew.bat app:connectedDebugAndroidTest` (requires device/emulator)
- Current automated tests are placeholder examples only (`app/src/test/.../ExampleUnitTest.kt`, `app/src/androidTest/.../ExampleInstrumentedTest.kt`), so rely on manual validation for playback/search/service changes.

