## APK build

GitHub Actions builds the debug APK on every push to `main`. Open the repository's **Actions** tab, select **Build LibreMind APK**, open the latest successful run, and download the `LibreMind-APK` artifact.

The workflow uses Gradle's GitHub Actions integration and uploads the generated APK as an artifact.
