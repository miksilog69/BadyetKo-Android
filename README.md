# BadyetKo Android

Native Android Studio / Kotlin / Jetpack Compose edition of BadyetKo.

This is **not a WebView wrapper**. The UI is native Compose and the app connects directly to the existing BadyetKo Vercel APIs and Supabase authentication.

## Package
`com.badyetko.app`

## Included in this first native build
- Native My Budget screen
- Monthly income, savings, planned/spent/available totals
- Native shopping item list
- Checkbox = Purchased / unchecked = Planned
- Offline local budget persistence
- Cloud load/save through `/api/budget`
- Google sign-in through existing Supabase OAuth
- Shared Budgets dashboard connection
- Notifications/challenge/activity API connection
- Theme selector with Liquid Glass, Soft Frost, Warm Minimal, Pastel Finance, Clean White, Soft Neumorphic, Pixelated
- Android deep-link auth callback (`badyetko://login-callback`)
- GitHub Actions APK build workflow

## Supabase setup required for Google sign-in
Add `badyetko://login-callback` to the project's allowed redirect URLs in Supabase Auth settings. The web redirect can remain enabled.

## Automated APK build
The included `.github/workflows/build-apk.yml` builds a native debug APK and uploads it as a GitHub Actions artifact.
