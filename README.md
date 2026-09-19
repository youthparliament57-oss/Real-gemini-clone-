# Gemini Live Mobile Clone 🌌

<p align="center">
  <img src="public/assets/readme_banner.svg" alt="Gemini Live Mobile Clone Banner" width="100%" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-purple.svg?style=flat-square&logo=kotlin" alt="Kotlin Version" />
  <img src="https://img.shields.io/badge/Compose-BOM_2024.09-green.svg?style=flat-square&logo=jetpackcompose" alt="Compose BOM" />
  <img src="https://img.shields.io/badge/Gemini_API-Server_Side-blue.svg?style=flat-square&logo=google" alt="Gemini API" />
  <img src="https://img.shields.io/badge/Robolectric_Tests-Passed-brightgreen.svg?style=flat-square" alt="Tests Status" />
  <img src="https://img.shields.io/badge/GitHub_Actions-Active-violet.svg?style=flat-square&logo=githubactions" alt="CI Status" />
</p>

---

## ✨ Overview

Welcome to the **Gemini Live Mobile Clone**, a premium, production-grade Android implementation of a natural voice dialogue terminal powered by Jetpack Compose and the Gemini API. 

The application replicates the voice capabilities of modern AI interfaces, integrating highly fluid 3D physical stardust animations, system overlay permissions routing, screen scanning, and local screenshot testing.

---

## 🎨 Premium Visual Highlights

### 🌟 High-Fidelity 3D Golden-Ratio Stardust Orb (`GeminiLiveOrb.kt`)
*   **600 Particle Coordinates**: Simulated dynamically on an analytical spherical coordinate field using the Golden Ratio angle distribution ($137.5^\circ$).
*   **Differential Y-Swirling (Galactic Shear)**: Swirls independent horizontal belts of stardust at varying speeds to prevent artificial rigidity and produce organic fluid motion.
*   **True 3D Depth Sorting & Occlusion**: Dynamically computes depth values ($Z$-axis mapping). Particles in the foreground are rendered larger and brighter, while back-most particles recede gracefully in slate-grey tones to produce an advanced sense of depth.
*   **Real-time Vocal Resonance**: Animates, vibrates, and breathes outward based on direct micro-amplitude feedback from active vocal/speech frequencies.

### 📱 Perfect System Navigation & Edge-to-Edge Padding
*   **Virtual Key Clearance**: Solves bottom control layout clipping issues under virtual system navigation keys (Home, Back, Recents) using Jetpack's native `.navigationBarsPadding()` configurations.
*   **High Contrast Canvas**: Features a warm neutral off-white surface wrapped in an eye-safe gradient blending into subtle sky-blue accents.

---

## 🛠️ Key Technical Implementations

1.  **System-Alert Overlays (`GeminiOverlayService`)**: Houses a background service allowing the live floating conversational orb to persist and animate above external apps and the home launcher.
2.  **Interactive Contextual Screen Projection**: Sweeps the launcher view and matches local contexts (Maps, YouTube, etc.) to query Gemini on active screen states.
3.  **Advanced Sign-In & Configuration Resilience**: Refactored Gradle configurations (`signingConfigs`) to check for the presence of local release keys dynamically. If not found, it seamlessly signs with the default `debug.keystore` fallback instead of crashing the compiler.

---

## 🧬 Local Verification & Testing

The project is fully integrated with **Robolectric** and **Roborazzi** for fast, local JVM simulation and visual snapshot regression analysis.

### Run Unit & Robolectric Tests
```bash
./gradlew testDebugUnitTest
```

### Record Reference Screenshots (Roborazzi)
```bash
./gradlew recordRoborazziDebug
```

### Verify Screenshot Changes
```bash
./gradlew verifyRoborazziDebug
```

---

## 🤖 GitHub Actions CI/CD Pipeline (`android.yml`)

The repository includes a modern, high-speed automated integration workflow:
1.  **Repository Checkout**: Restores the code.
2.  **JDK 17 Setup**: Configures the Zulu JDK 17 with active Gradle caching.
3.  **Environment Config Recovery**: Safely configures local `.env` keys from `.env.example`.
4.  **Keystore Decoding**: Restores and decodes your original `debug.keystore.base64` binary to bypass local gitignore rules.
5.  **Automated Quality Checks**: Executes the unit and screenshot testing suite.
6.  **Apk Packaging**: Builds the final, signed release and debug APKs and packages them as a downloadable run artifact (**`gemini-live-debug-apk`**).

---

## 🔧 Troubleshooting & Deep Technical Fixed

### 1. KSP / Kotlin Symbol Processor NullPointerExceptions
*   **The Error**: `Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException: Cannot invoke Application.getService()` during the compilation of room models.
*   **The Resolution**: We identified that forcing the compiler to run inside the main Gradle process via `kotlin.compiler.execution.strategy=in-process` (in `gradle.properties`) prevents KSP from isolating the IntelliJ classloader. Commenting out this strategy allows Kotlin and KSP to launch separate background daemons, restoring complete compile-time stability.

### 2. Draw-Over-Apps System Lockup
*   **The Error**: The conversational screen appears to lock up or freeze when triggering screen-share features.
*   **The Resolution**: Because Android security policies require explicit system alert configurations, the app redirects the user to the system "Display over other apps" page. Ensure you scroll to the application in the list, enable the switch to **"Allowed"**, and click back to initiate the floating live overlay orb instantly!
