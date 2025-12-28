# AGENT.md — Stack Game (Android / Kotlin)

## Project Overview

This project is an **Android game** written in **Kotlin with Jetpack Compose**.

It is a **modern, maintainable codebase** that uses:
-   **MVVM** architecture.
-   **Koin** for Dependency Injection.
-   **Ktor** for network requests (Auth/API).
-   **DataStore / EncryptedSharedPreferences** for secure local storage.

The game implements **custom physics** (center of mass, angular velocity) without an external engine.

---

## Architectural Rules (MANDATORY)

### 1. MVVM & Unidirectional Data Flow
-   **ViewModel** (`GameViewModel`, `MainViewModel`, etc.):
    -   Owns the **Truth** (State).
    -   Exposes `StateFlow<UiState>`.
    -   Handles ALL logic: Physics, Scoring, Camera, Navigation events.
-   **View** (Compose):
    -   **Stateless** where possible.
    -   Renders `UiState`.
    -   Sends user intents (taps, gestures) to ViewModel.
    -   **No business logic** in Composables.

### 2. Dependency Injection (Koin)
-   All dependencies (ViewModels, Repositories, UseCases) must be declared in `AppModule.kt` or strictly organized Koin modules.
-   Inject via constructor injection in ViewModels.
-   Resolve via `koinViewModel()` in Compose.

### 3. Data & Networking (Ktor)
-   **ApiClient**: Uses Ktor for network calls.
    -   Currently mocks a backend by fetching static JSON files (e.g., `user.json`).
    -   Handles User Authentication and Subscription validation.
-   **UserPreferences**: Manages local persistence.

---

## Core Gameplay

### Physics System
Unlike a simple stacker, this game uses **2D Physics**:
-   **Stability Check**: Blocks falling outside the "Goldilocks zone" (too much overhang) become unstable.
-   **Angular Velocity**: Unstable blocks rotate based on overlap direction.
-   **Tower Balance**: The entire stack's **Center of Mass** is calculated (`checkTowerBalance`). If it drifts outside the base, the tower topples.
-   **Gravity**: Blocks fall, accelerate, and can tumble off-screen.

### Lives System
-   Player starts with **Lives** (Default: 3).
-   **Life Lost**: When a block falls off the screen.
-   **Game Over**: When `Lives == 0` OR the Tower loses balance (immediate collapse).
-   **Bonuses**: Extra lives awarded at specific score intervals (capped by Subscription tier).

### Paliers & Milestones
-   **Celebrations**: Visual effects triggered at score milestones (every 10 points).
-   **Difficulty Curve**:
    -   Block width decreases.
    -   Speed increases.
    -   Physics tolerance tightens.

### Rewind Feature
-   **Snapshot System**: Every successful move saves a `GameSnapshot` (Stack state, Score, Camera).
-   **Rewind Action**: Long-press triggers `onRewind()`.
-   **Constraint**: Only available for **PREMIUM/ULTRA** users.

---

## User System & Subscriptions

The app features a tiered user system:

1.  **Context**: `User` object retrieved via `ApiClient`.
2.  **Tiers**:
    -   **FREE**: Contains Ads (`showAdOverlay`), Limited Lives.
    -   **PREMIUM**: No Ads, More Lives (4), Rewind unlocked.
    -   **ULTRA**: No Ads, Max Lives (5), All features + Future perks.

### Auth Flow
-   Users "login" via email (mapped to a JSON file on the "backend").
-   Session persisted securely.

---

## Rendering & Visuals
-   **Compose Canvas**: Manual drawing of Blocks and Particles.
-   **Particles**:
    -   Explosion on placement.
    -   Color inherited from block.
-   **Camera**:
    -   Smoothly tracks the top of the stack.
    -   Implemented as `cameraY` offset in `GameState`.

---

## Development Workflow

### Build Validation
After editing code, **ALWAYS** run:
```bash
./gradlew assembleDebug
```

### Adding Features
1.  Define **Model** (Data class).
2.  Update **ViewModel** (Logic/State).
3.  Update **Compose UI** (Render).
4.  Register in **Koin** (if new service).

### Design Philosophy
-   **Immediate Fun**: < 3s to start playing.
-   **Game Feel**: Heavy use of Haptics (`VibrateSuccess`, `VibrateFail`) and Screen Shake.
-   **Determinism**: Physics logic should be testable without the UI.