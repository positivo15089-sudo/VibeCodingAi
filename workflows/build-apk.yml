name: Build APK

on:
  workflow_dispatch:
  push:
    branches: [ "main", "master" ]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Install Android SDK 35
        run: sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

      - name: Setup Gradle 8.13
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.13'

      - name: Run unit tests
        run: gradle testDebugUnitTest --stacktrace

      - name: Build debug APK
        run: gradle assembleDebug --stacktrace

      - name: Verify APK exists
        run: test -f app/build/outputs/apk/debug/app-debug.apk

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: VibeCodingAI-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
          retention-days: 30

      - name: Upload build reports on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: build-reports
          path: |
            app/build/reports/
            app/build/test-results/
          if-no-files-found: ignore
