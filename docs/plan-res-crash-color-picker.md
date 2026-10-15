# Color Picker & Resolution Switch Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the `ClassCastException` on 1200x1200 resolution export and add a custom team color picker with hue spectrum slider, brightness slider, presets, and reset capability to the match editor.

**Architecture:** 
- In `PosterRenderer.kt`, use `FrameLayout.LayoutParams` instead of `ViewGroup.LayoutParams` for `hostViewGroup` and `offscreenWebView` to satisfy DecorView's layout cast requirements.
- In `EditorViewModel.kt`, add `updateHomeColor`, `updateAwayColor`, `resetHomeColorToDefault`, and `resetAwayColorToDefault`.
- In `EditorScreen.kt`, create `ColorPickerDialog` with a rainbow hue slider, brightness slider, hex text editor, quick color presets, and reset button. Add a color customization button beside the home/away team dropdown and xG rows.
- Bump `versionCode` to 4 (`versionName = "1.0.3"`) in `build.gradle.kts` and assemble APK.

**Tech Stack:** Kotlin, Jetpack Compose, Android FrameLayout, HSV Color Math (`android.graphics.Color`), Gradle 8.10.2.

---

### Task 1: Fix Resolution Toggle Crash in `PosterRenderer.kt`
- [ ] Replace `ViewGroup.LayoutParams` with `FrameLayout.LayoutParams` at lines 34, 108, and 115 in `PosterRenderer.kt`.
- [ ] Import `android.widget.FrameLayout`.

### Task 2: Add Color Customization Methods in `EditorViewModel.kt`
- [ ] Implement `updateHomeColor(colorHex: String)`
- [ ] Implement `updateAwayColor(colorHex: String)`
- [ ] Implement `resetHomeColorToDefault()`
- [ ] Implement `resetAwayColorToDefault()`

### Task 3: Implement `ColorPickerDialog` and Color Pickers in `EditorScreen.kt`
- [ ] Create `ColorPickerDialog` composable with:
  - Rainbow Hue spectrum slider (0..360f)
  - Saturation and Brightness/Value sliders
  - Live preview box with Hex display and input
  - Football team color presets
  - "بازگردانی به رنگ پیش‌فرض تیم" button
- [ ] Add color picker trigger buttons next to Home and Away Team dropdowns.
- [ ] Add clickable color dots to Home and Away xG fields.

### Task 4: Bump Version and Build APK
- [ ] In `app/build.gradle.kts`: bump `versionCode = 4`, `versionName = "1.0.3"`.
- [ ] Run `build_apk.ps1` to compile and deploy to `C:\Users\HOS3EIN\Desktop\ARMO-Poster-arm64.apk`.
- [ ] Verify output file size and timestamp.
