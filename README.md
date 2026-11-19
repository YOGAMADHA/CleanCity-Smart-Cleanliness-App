# CleanCity – Smart City Cleanliness Management Android App

CleanCity is a native Android application built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Firebase Authentication (Google Sign-In via Jetpack Credential Manager)**, and **Cloud Firestore**.

## Features

### Citizen Module
- **Citizen Portal & Registration**: Google Sign-In via Credential Manager (`GetSignInWithGoogleOption`) followed by profile registration (Name, verified Email, Phone Number, Ward/Neighborhood).
- **Citizen Home Dashboard**: Personalized welcome banner, quick **Report Cleanliness Issue** button, KPI counters (Total, Pending, Resolved), and recent complaints.
- **Report Complaint**: Title, 6 waste categories (*Garbage Overflow*, *Roadside Waste*, *Illegal Dumping*, *Drainage Issues*, *Public Area Cleanliness*, *Other*), description, camera capture (`TakePicture` + `FileProvider`), gallery selection (zero-permission Android Photo Picker), GPS location capture (`FusedLocationProviderClient` + `Geocoder`), and unique tracking ID generation (`CC-YYYY-XXXXXX`).
- **My Complaints & Tracking**: Real-time Firestore updates, search by Complaint ID or title, status filter chips, and detailed status history timeline with municipal admin remarks.
- **Profile**: View/edit contact details, workspace switcher (for authorized admins), and sign out.

### Admin Module
- **Admin Portal**: Role-verified login backed by Cloud Firestore security rules.
- **Admin Dashboard**: City-wide KPIs (*Total*, *New*, *In Progress*, *Resolved*), category distribution bars, and priority dispatch queue.
- **Manage Complaints**: Search by Complaint ID, citizen name, or street; filter by status; inspect uploaded photos and citizen contact info; update complaint status (*Reported* / *In Progress* / *Resolved*) and post official remarks synced in real time to citizen accounts.

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3 (`Outfit`, `Plus Jakarta Sans`, `JetBrains Mono` bundled in `res/font/`)
- **Navigation**: Jetpack Navigation Compose
- **Backend**: Firebase Authentication + Cloud Firestore (`ai-studio-android-1385ea20-87f2-47b7-8dd4-1b6aa0f015ac`)
- **Hardware APIs**: Camera (`FileProvider`), Android Photo Picker (`PickVisualMedia`), Google Play Services Location (`FusedLocationProviderClient`)

## Setup & Installation
1. **Run in AI Studio Streaming Emulator**: Open the live preview directly in Google AI Studio.
2. **Export Project ZIP / APK**:
   - Use the AI Studio menu to **Export ZIP** or **Download APK**.
   - To install the APK on an Android phone, transfer the APK to your device, allow installation from trusted sources when prompted, and open the APK file.
3. **Local Build (Android Studio / Gradle)**:
   - Open the exported project in Android Studio.
   - Ensure `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` are present (already provisioned).
   - Build & run on a device or emulator with Google Play Services (`gradle :app:assembleDebug`).

## Firestore Security Rules Testing
Run local security rules tests with the Firestore emulator:
```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```
