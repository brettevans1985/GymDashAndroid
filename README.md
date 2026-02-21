# GymDash Companion

Android companion app for the GymDash fitness dashboard. Syncs health data from Health Connect to a GymDash server.

## Features

- **Health Connect Integration** - Reads heart rate, sleep, activity, SpO2, HRV, weight, respiratory rate, blood pressure, body temperature, VO2 max, and blood glucose data
- **Periodic Background Sync** - Configurable sync interval via WorkManager
- **Editable Dev Server URL** - Change the development server IP directly on the login screen without rebuilding
- **Login Persistence** - Username and dev server URL are remembered between sessions
- **Mock Data Mode** - Debug builds targeting non-production servers automatically send generated physiological data instead of reading from Health Connect

## Build Configuration

The app defines two server environments:

| Environment | Build Config Field | Default |
|---|---|---|
| Development | `DEFAULT_SERVER_URL` | `http://192.168.1.48:5000` |
| Production | `PRODUCTION_SERVER_URL` | Defined in `build.gradle.kts` |

When the Development server is selected on the login screen, the URL field is editable so you can point to any local server IP without rebuilding.

## Network Security

The app permits cleartext HTTP traffic via `network_security_config.xml` to support local development servers on the LAN. Both debug and release builds use HTTP for local server communication.

## Architecture

- **UI**: Jetpack Compose with Material 3
- **DI**: Hilt
- **Networking**: Retrofit + Moshi with a `BaseUrlInterceptor` for runtime URL switching
- **Local Storage**: DataStore (preferences), Room (sync history)
- **Health Data**: Health Connect API
- **Background Work**: WorkManager

## Building

Requires JDK 17+. Android Studio's bundled JBR is recommended:

```bash
JAVA_HOME="/path/to/android-studio/jbr" ./gradlew installDebug
```
