# GalaxyClash

A 2D space shooter game for Android, developed with Kotlin and Jetpack Compose.

GalaxyClash combines arcade-style space combat with progressive levels, enemy AI, boss encounters, power-ups, and a structured game engine designed around reusable gameplay systems.

## Features

* 2D space shooter gameplay
* Player-controlled spacecraft
* Enemy ships with dedicated AI behavior
* Multiple boss encounters
* Asteroid system
* Projectile and collision systems
* Power-up system
* Progressive level system
* Particle effects
* Dynamic space background and star systems
* In-game HUD
* Pause and Game Over screens
* Settings and gameplay configuration
* Upgrade/improvement system
* Integrated background music and game audio
* Responsive Jetpack Compose interface
* Unit tests for core gameplay systems
* Android instrumentation tests

## Technologies

* Kotlin
* Jetpack Compose
* Android SDK
* Android Jetpack
* Gradle Kotlin DSL
* JUnit
* Android Instrumentation Tests
* Android Audio APIs

## Game Architecture

GalaxyClash is organized into dedicated gameplay systems to keep the game logic modular and maintainable.

Core systems include:

* **Game Engine** — Coordinates the main gameplay state and systems.
* **Enemy AI System** — Controls enemy behavior and movement logic.
* **Collision System** — Handles interactions between game entities and projectiles.
* **Level System** — Manages progressive gameplay stages.
* **Pickup System** — Handles collectible power-ups and their effects.
* **Background System** — Controls stars and space background elements.
* **Particle System** — Manages visual effects during gameplay.
* **Audio System** — Controls music and gameplay audio.
* **Boss System** — Handles boss entities and their gameplay behavior.

## Project Structure

```text
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cristian/galaxyclash/
│   │   │       ├── audio/
│   │   │       ├── game/
│   │   │       ├── ui/
│   │   │       ├── GalaxyClashApplication.kt
│   │   │       └── MainActivity.kt
│   │   └── res/
│   │       ├── drawable/
│   │       ├── raw/
│   │       ├── mipmap/
│   │       └── values/
│   │
│   └── test/
│       └── java/
│
├── build.gradle.kts
└── proguard-rules.pro
```

## Testing

The project includes automated tests covering important gameplay systems, including:

* Asteroid behavior
* Boss behavior
* Enemy AI
* Game engine logic
* Pickup systems
* Progression systems

Android instrumentation tests are also included to validate functionality in the Android environment.

## Android Configuration

* **Minimum SDK:** Android 7.0 (API 24)
* **Target SDK:** Android 16 / API 36+
* **Language:** Kotlin
* **UI:** Jetpack Compose

## Getting Started

Clone the repository:

```bash
git clone https://github.com/cristianevertondev/GalaxyClash.git
```

Open the project in Android Studio and allow Gradle to synchronize the project dependencies.

Then build and run the application on an Android device or emulator.

## Project Goals

GalaxyClash was created as a portfolio project to explore Android game development with Kotlin and Jetpack Compose while building reusable gameplay systems, structured game logic, automated testing, and a complete interactive game experience.

## Developer

Developed by **ELDREON STUDIOS**.

* GitHub: https://github.com/cristianevertondev
* LinkedIn: www.linkedin.com/in/cristian-everton-30388b438

---

*GalaxyClash is an independent Android game project developed for portfolio and learning purposes.*
