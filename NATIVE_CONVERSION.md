# Native conversion notes

This project deliberately does not embed the BadyetKo website in a WebView.

The Android app uses:
- Kotlin + Jetpack Compose for the interface
- SharedPreferences JSON for offline-first local budget storage
- OkHttp for the existing Vercel API (`/api/budget`, `/api/collab`, `/api/challenge`, `/api/activity`)
- Supabase OAuth in a Chrome Custom Tab, returning to `badyetko://login-callback`

The existing website remains independent and can continue to be deployed on Vercel.
