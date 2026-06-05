# Slanotif 🔔

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Privacy](https://img.shields.io/badge/Privacy-100%25_Local-blue.svg)](#-privacy-first-architecture)
[![License](https://img.shields.io/badge/License-Apache_2.0-orange.svg)](LICENSE)

**Slanotif** (`com.hnkapps.slanotif`) is a lightweight, privacy-focused Android utility that allows you to assign custom, high-priority notification sounds to specific Slack channels, threads, or direct messages.

Android's official Slack application only supports a single global notification sound for all alerts. Slanotif solves this pain point, letting you know instantly whether an alert is a critical server outage, a client emergency, or a casual team chat.

---

## ✨ Features

*   **Per-Channel Custom Alerts**: Define specific channel names or keywords (e.g., `#critical-alerts`, `urgent`, `deploy`) and map them to custom audio tones.
*   **Hardware Silence Gestures**: Stop long or loud ringtones instantly by simply pressing your phone's **Volume buttons** or **Power button** (screen off/on).
*   **Highly Optimized**: Operates with minimal memory overhead, using low-latency media playback.
*   **Aesthetic Dashboard**: Includes a clean, modern user interface for managing channel settings and audio tones.

---

## 🛡️ Privacy-First Architecture

Unlike many notification managers, Slanotif is built with strict privacy constraints:
*   **No Internet Permission**: The app does not request or declare the Android `INTERNET` permission. It is technically impossible for the app to transmit your notification data.
*   **No Analytics or SDKs**: Zero integration with telemetry, crash-reporting tools, or advertising networks.
*   **100% Local**: All notification scanning and matching happen in-memory on your physical device.

---

## 🚀 How to Setup

### 1. Download and Install
Get the latest build directly from the [GitHub Releases](https://github.com/himanshu-kalundia/android-slack-modifier/releases) page and install the APK on your Android device (ensure "Install from Unknown Sources" is enabled in your browser/file manager).

### 2. Grant Notification Access
Slanotif requires permission to see incoming system notifications to filter Slack alerts:
1. Open the Slanotif app.
2. Tap the **Enable Notification Access** button.
3. Locate **Slanotif** in the system list and toggle the permission switch to **ON**.

### 3. Configure Your Rules
1. (Optional) Copy your custom audio files (MP3, WAV, or OGG) to your phone's storage.
2. Open Slanotif and tap the floating **`+`** (FAB) button.
3. Type the exact channel name or keyword you want to target (e.g., `production-alerts` or `boss-name`).
4. Tap **Choose Sound** and select either a **System Tone** or an **External Audio File**.
5. Tap **Save Rule**. Add as many custom alert rules as you need!

---

## 🛠️ Development & Building

To build the project locally or make modifications:

### Prerequisites
*   Android Studio (Iguana or newer recommended)
*   Android SDK 34
*   Gradle 8.5+

### Build Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/himanshu-kalundia/android-slack-modifier.git
   ```
2. Open Android Studio and select **Open an existing project**, choosing the `android_project` subdirectory.
3. Click the **Sync Project with Gradle Files** icon.
4. Clean and assemble the app:
   *   Go to **Build > Clean Project**
   *   Select **Build > Assemble Project** to generate the debug APK.

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
