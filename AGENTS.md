# AGENT.md — Stack Game (Android / Kotlin)

## Project Overview

This project is an **Android game** written in **Kotlin with Jetpack Compose**.

The codebase **must follow an MVVM architecture** with a **DRY (Don’t Repeat Yourself) approach**, even though the gameplay can be prototyped quickly and may initially exist in a small number of files.

The game is implemented **without external engines or backend** and is designed to remain:
- fast to iterate
- easy to debug
- easy to extend
- production-ready despite its simplicity

---

## Architectural Rules (MANDATORY)

### MVVM

The project must respect **MVVM separation**:

- **Model**
    - Pure data classes (`Block`, `Particle`, `Snapshot`, etc.)
    - No Android or Compose dependencies
    - Deterministic logic only

- **ViewModel**
    - Owns all game state
    - Contains:
        - game loop logic
        - physics and collision
        - difficulty curve
        - rewind logic
        - camera target calculation
    - Exposes state via immutable observable data (`State`, `StateFlow`, or equivalent)
    - Must be testable without UI

- **View (Compose UI)**
    - Pure rendering and input handling
    - No gameplay logic
    - No physics, scoring, or difficulty computation
    - Canvas rendering only reflects ViewModel state

### DRY Principles

- No duplicated logic for:
    - difficulty calculations
    - block placement rules
    - camera positioning
- Constants must be centralized
- Calculations must be reusable and deterministic
- Visual effects may vary, but logic must be shared

---

## Core Gameplay

- A rectangular block falls vertically from the top of the screen.
- The player taps to drop the block.
- If the block is sufficiently aligned with the previous one:
    - It lands
    - The overlapping part remains
    - The non-overlapping part is discarded
- If alignment fails:
    - Game over
    - Screen shake + haptic feedback

### Scoring
- Score = number of successfully stacked blocks.

---

## Difficulty Curve

Difficulty increases dynamically with score using three parameters:

1. **Alignment tolerance**
    - Decreases progressively
    - Has a minimum threshold to stay playable

2. **Block width**
    - Shrinks as score increases
    - Makes alignment harder over time

3. **Fall speed**
    - Increases linearly with score

These parameters must be:
- deterministic
- centralized
- derived only from the score

---

## Rendering & Visuals

- Rendering is done using **Compose Canvas**
- Blocks are simple rectangles
- Visual feedback includes:
    - Screen shake on failure
    - Particle effects on successful placement
    - Extra particles on perfect alignment

### Background Progression

The background changes based on score:
- Low score: underground / soil tones
- Mid score: sky / city gradient
- High score: space gradient

At high score:
- Procedural stars are rendered (random white dots)

No external assets are used.

---

## Camera System

- The camera follows the stack vertically.
- Implemented as a **Canvas translation**, not by moving blocks.
- Camera movement must be:
    - smooth (animated interpolation)
    - deterministic
    - derived from stack height
- The camera system is part of the **ViewModel logic**, not the View.

---

## Game Loop

- Uses `withFrameNanos` for a manual frame loop.
- Delta time is computed from frame timestamps.
- No physics engine is used.
- All motion and collision logic is custom and simplified.
- The loop logic must reside in the **ViewModel**.

---

## Haptic Feedback

Haptic feedback is used for:
- Successful block placement
- Perfect alignment
- Game over

The **ViewModel emits events**, the **View triggers haptics**.

---

## Rewind System

- Before each successful placement, a **snapshot** is saved:
    - List of blocks
    - Current score
    - Camera target position
- Snapshots are stored in a bounded history.
- A **long press** triggers rewind:
    - Restores last snapshot
    - Resets game over state
    - Spawns a new falling block

Rewind logic lives in the **ViewModel**.

---

## Architecture Constraints

- MVVM architecture is mandatory
- DRY principles are mandatory
- No backend
- No external physics or rendering engine
- No dependency on game frameworks
- Deterministic gameplay
- Compose UI must remain dumb and declarative

---

## Design Goals

- Immediate understanding (playable in < 3 seconds)
- Strong visual + haptic feedback
- High replayability
- Minimal technical complexity
- Easy extension for:
    - Premium / Ultra features
    - Slow motion
    - Zen mode
    - Skins
    - Replay / ghost systems

---

## Intended Use of This File

This document is meant to:
- Serve as a **contract** for contributors and agents
- Guide debugging and refactoring
- Prevent architectural drift
- Ensure long-term maintainability

## Build validation

After editing code, ALWAYS run `./gradlew assembleDebug` to validate the build.