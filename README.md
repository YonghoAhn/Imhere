# Imhere

Android client prototype for a **2020 Soongsil University capstone project** exploring proximity detection and location-aware assistance using Bluetooth Low Energy.

The app combines BLE beacon detection, background location tracking, and a small Firebase-backed prototype flow.

## Features

- BLE beacon scanning and monitoring
- Beacon advertising experiments
- Foreground background-tracking service
- Fused location updates
- Nearby-device alert with sound and vibration
- Automatic restart / service lifecycle experiments
- Anonymous Firebase Authentication
- Firestore-backed prototype state

## Tech stack

- Kotlin
- Android SDK
- Bluetooth Low Energy
- AltBeacon Android Beacon Library
- Google Play Services Location
- Firebase Authentication / Firestore

## Repository notes

This repository is an **archived prototype**, not a production application. It targets Android APIs and libraries from 2020 and may require modernization before it can be built on a current Android toolchain.

Firebase project configuration such as `google-services.json` is intentionally **not included** in this repository. No API key, service-account key, signing keystore, password, or other reusable credential is committed in the current tree.

The source contains project-specific BLE/GATT identifiers and demonstrates a location-data flow. Anyone reusing the code should replace those identifiers and configure their own secured backend before deployment.
