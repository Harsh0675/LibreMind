# Building LibreMind

The repository contains a GitHub Actions workflow named **Build LibreMind APK**. It runs on pushes to `main` and can also be started manually.

The workflow installs Java and Gradle on the GitHub runner, runs `:app:assembleDebug`, and uploads `app-debug.apk` as the `LibreMind-APK` artifact.