# تطبيق قيادة الرقمي — منصة التأجير وإدارة الأساطيل (`qiyada-key-car-rental-1289770`)
> منظومة car_rental المتكاملة مع المفتاح الرقمي وتيليمتريكس المركبات عبر بروتوكول OBD-II CAN-Bus

- **Project ID**: `#1289770`
- **Commercial Archetype**: `car_rental`
- **Bound Hardware Peripheral**: `OBD-II CAN-Bus Telematics + BLE Digital Key Fob`
- **Package Name**: `sa.factory.qiyadakeycarrental`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update]
- **Summary of Completed Fixes and Features**:
  - **BleDigitalKeyFobEngine:** Implemented Car Connectivity Consortium (CCC) aligned BLE Digital Key Fob engine for secure vehicle entry and start authorization. Features SHA-256 HMAC challenge-response cryptographic handshake, monotonic anti-replay sequence validation, in-cabin RSSI proximity geofencing (-62 dBm engine start cutoff, -72 dBm door lock/unlock cutoff), and complete vehicle lock/immobilizer state machine.
  - **ObdCanBusTelematicsEngine:** Implemented SAE J1979 / ISO 15765-4 OBD-II CAN-Bus PID decoding engine. Parses standard PIDs including Engine RPM (0x0C), Vehicle Speed (0x0D), Fuel Tank Level % (0x2F), Coolant Temperature (0x05), Run Time (0x1F), and Odometer (0x31). Includes Mode 03 Diagnostic Trouble Code (DTC) bitfield decoder and driving dynamics scoring (harsh braking/acceleration detection with 0-100 fleet eco rating).
  - **Jetpack Compose UI Hardening:** Fixed Kotlin single-quote string interpolation syntax hazards in `HomeScreen.kt` and `TelemetryScreen.kt`, resolved missing `FontWeight` and `sp` Compose imports in `MainActivity.kt`, and replaced deprecated divider components.
  - **Toolchain & Distribution:** Bundled complete Gradle wrapper scripts (`gradle/`, `gradlew`, `gradlew.bat`) and `.gitignore` for zero-friction cloud builds.
  - **Comprehensive Unit Regression Tests:** Established `QiyadaKeyRegressionTest.kt` with 9 unit tests verifying signature generation, replay attack rejection, in-cabin proximity enforcement, OBD PID formulas, DTC decoding, fleet eco scoring, and driver telemetry streaming.

- **System Architecture**:
  - Clean Architecture & MVVM with unidirectional data flow (`StateFlow` / `SharedFlow`).
  - Modular engine abstractions under `sa.factory.qiyadakeycarrental.engine` wired directly into `HardwareSensorDriver`.
  - WCAG 2.1 AAA contrast compliance and strict zero-emoji invariant throughout all user-facing strings and logs.

- **Build and Test Verification**:
  - Run unit tests: `./gradlew testDebugUnitTest`
  - Cloud Emulator Test: Executed on `hmoushim0-cloud/Android-test-harness` running Google Pixel 6 (API 34).
