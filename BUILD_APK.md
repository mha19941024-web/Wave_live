# Build Wave APK

This project includes a GitHub Actions workflow at `.github/workflows/build-apk.yml`.

It does not depend on `gradlew` or third-party GitHub Actions. The runner downloads Gradle 8.11.1, builds `app-debug.apk`, and creates a GitHub Release containing the APK on pushes to `main` or `master`.

For the LIVE button to create a Cloudflare live input, the Worker must have the required Cloudflare Stream secrets configured and the D1 migration `0005_live_gifts.sql` applied.
