# ARMO Poster App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a self-contained, offline Android application for `arm64-v8a` devices that renders locked HTML/CSS templates via Android's WebView into 2400×2400 PNG match posters (ARMO NOTES) with auto-fit laddering, match history, and bilingual UI (Persian/English).

**Architecture:** Jetpack Compose UI (Editor, Export, History) manages in-memory and disk-persisted `MatchData`. Rendering is performed by an offscreen `WebView` attached via `translationY = -5000f` using `WebViewAssetLoader`, measuring content height against available height across a 6-level CSS fit ladder, and drawing to a 2400×2400 ARGB_8888 `Bitmap`.

**Tech Stack:** Kotlin 2.0+, Jetpack Compose + Material 3, AndroidX WebKit (`WebViewAssetLoader`), Kotlinx Serialization JSON, Android MediaStore API, Gradle Version Catalogs.

**Spec:** `docs/superpowers/specs/2026-10-13-armo-poster-design.md`

## Global Constraints

- Target ABI: `arm64-v8a` only (`ndk { abiFilters += "arm64-v8a" }`).
- minSdk: 26 (Android 8.0 Oreo), compileSdk / targetSdk: 35.
- Completely offline: no network requests, no remote CDNs, all assets bundled in APK.
- No bundled browser engine: use Android framework `WebView`.
- Proguard / R8: keep `@android.webkit.JavascriptInterface` methods in release builds.
- Poster output resolution: 2400×2400 PNG (with 1200×1200 toggle for low memory).
- UI localization: Persian (RTL, default) + English; Poster content: always English (LTR).

## Review Focus

1. **Font Loading Race Condition:** Offscreen WebView draws before Anton font is loaded, producing wrong fallback metrics. Verified by waiting on `document.fonts.ready` + image `decode()` before measurement.
2. **Blank Bitmap Capture:** `WebView.draw(canvas)` returns an empty or single-color bitmap if hardware acceleration or layout is unready. Verified by bitmap pixel sampling and software layer fallback.
3. **Fit Ladder Level 6 Exhaustion:** Content exceeds the max tightening available. The engine must safely return `TooLong(overflowPx)` and prevent broken layout.
4. **JS Bridge Stripped by R8:** Release build fails silently without `@JavascriptInterface` Proguard preservation rules. Verified by checking Proguard configuration.
5. **Team Crest Resolution Fallback:** A custom or missing crest must render a neutral initials circle instead of crashing or showing a broken icon.

---

### Task 1: Project Scaffolding & arm64 Build Configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: Gradle build scripts configured for Android SDK 35, `arm64-v8a` ABI filter, Compose BOM, WebKit, and Kotlin Serialization.

- [ ] **Step 1: Write `gradle/libs.versions.toml`**
Define versions:
- AGP: 8.7.0
- Kotlin: 2.0.21
- Compose BOM: 2024.10.00
- AndroidX WebKit: 1.12.1
- Kotlinx Serialization: 1.7.3
- Navigation Compose: 2.8.3

- [ ] **Step 2: Write root `settings.gradle.kts` & `build.gradle.kts`**
Configure plugin management and repositories (google, mavenCentral).

- [ ] **Step 3: Write `app/build.gradle.kts` with arm64 filter and R8 rules**
Set `minSdk = 26`, `targetSdk = 35`, `ndk.abiFilters += "arm64-v8a"`, `androidResources.noCompress += listOf("ttf", "otf")`.
Configure dependencies: Compose Material 3, Navigation, WebKit, Kotlinx Serialization.

- [ ] **Step 4: Write `app/proguard-rules.pro`**
Add rule:
```
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
```

- [ ] **Step 5: Write `app/src/main/AndroidManifest.xml`**
Declare application, locale configuration, FileProvider for sharing, and MainActivity.

- [ ] **Step 6: Commit**
```bash
git add settings.gradle.kts build.gradle.kts gradle/ app/
git commit -m "chore: setup project scaffolding with arm64-v8a ABI filter"
```

---

### Task 2: Asset Bundle & Poster Template Preparation

**Files:**
- Create: `app/src/main/assets/fonts/Anton-Regular.ttf` (copy from source)
- Create: `app/src/main/assets/fonts/Inter.ttf` (copy from source)
- Create: `app/src/main/assets/logos/*.png` (20 clubs copied from source)
- Create: `app/src/main/assets/teams.json` (with added `color` hex field for each club)
- Create: `app/src/main/assets/poster/poster.html` (adapted with fit ladder classes & asset loader URLs)
- Create: `app/src/main/assets/poster/fit.js` (measurement & fit ladder functions)

**Interfaces:**
- Produces: Complete assets directory accessible via `WebViewAssetLoader` at `https://appassets.androidplatform.net/assets/`.

- [ ] **Step 1: Copy fonts and club logos from `~/.workbuddy-ai/skills/armo-match-poster/assets/`**
Transfer `fonts/` and `logos/` to `app/src/main/assets/`.

- [ ] **Step 2: Create `app/src/main/assets/teams.json` with primary club colors**
Enhance each premier league team entry with its kit color (e.g. Chelsea `#034694`, Bournemouth `#DA291C`, Arsenal `#EF0107`, Man City `#6CABDD`, etc.).

- [ ] **Step 3: Write `app/src/main/assets/poster/poster.html`**
Include font-faces, CSS variables, fit ladder classes (`.fit1` through `.fit6`), spacers (`gap-a`, `gap-b`, `data-spacer`), and template structure for dynamic DOM population.

- [ ] **Step 4: Write `app/src/main/assets/poster/fit.js`**
Implement `window.__armo` with `measure()`, `fit(n)`, and `build(payload)`. Ensure `measure()` ignores `[data-spacer]`.

- [ ] **Step 5: Verify asset integrity**
Check that all 20 team logos exist and `teams.json` parses cleanly.

- [ ] **Step 6: Commit**
```bash
git add app/src/main/assets/
git commit -m "feat(assets): bundle fonts, logos, teams.json and poster HTML template"
```

---

### Task 3: Domain Models & Derived Calculation Logic

**Files:**
- Create: `app/src/main/java/com/armo/poster/domain/model/MatchData.kt`
- Create: `app/src/main/java/com/armo/poster/domain/model/DerivedValues.kt`
- Test: `app/src/test/java/com/armo/poster/domain/DerivedValuesTest.kt`

**Interfaces:**
- Produces: `MatchData`, `TeamRef`, `PlayerBlock` data classes and `DerivedValues` calculation utilities (`calculateBarWidths`, `pairPlayerBlocks`, `formatHeadline`, `formatPanelLabel`).

- [ ] **Step 1: Write failing test `DerivedValuesTest.kt`**
Test:
- `calculateBarWidths(homeXg = 1.10, awayXg = 1.38)` -> `(homePct = 79.7, awayPct = 100.0)`
- `calculateBarWidths(homeXg = 2.0, awayXg = 2.0)` -> `(homePct = 100.0, awayPct = 100.0)`
- `pairPlayerBlocks(home = 4 blocks, away = 3 blocks)` -> 4 paired rows with empty block for index 3 on away side.
- Headline abbreviation format: `CHE vs BOU` capped at 3 chars.

- [ ] **Step 2: Implement `MatchData.kt` data classes with `@Serializable`**
Implement `MatchData`, `TeamRef`, `PlayerBlock`.

- [ ] **Step 3: Implement `DerivedValues.kt`**
Implement math for bar widths percentage, column pairing with padding, and panel labels `"$name ($formation)"`.

- [ ] **Step 4: Run tests and verify PASS**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/armo/poster/domain/ app/src/test/
git commit -m "feat(domain): add MatchData models and derived calculations logic"
```

---

### Task 4: Team Registry & Match Storage

**Files:**
- Create: `app/src/main/java/com/armo/poster/data/TeamRegistry.kt`
- Create: `app/src/main/java/com/armo/poster/data/MatchStore.kt`
- Test: `app/src/test/java/com/armo/poster/data/TeamRegistryTest.kt`
- Test: `app/src/test/java/com/armo/poster/data/MatchStoreTest.kt`

**Interfaces:**
- Produces: `TeamRegistry` (`loadFromAssets()`, `getTeam(name): TeamRef?`, `allTeams(): List<TeamRef>`).
- Produces: `MatchStore` (`list(): List<MatchSummary>`, `load(id): MatchData`, `save(data): String`, `duplicate(id): String`, `delete(id)`).

- [ ] **Step 1: Write failing test `TeamRegistryTest.kt`**
Test parsing JSON, verifying 20 clubs are loaded, each club has non-empty `abbr`, `color`, and `crestAsset`.

- [ ] **Step 2: Write failing test `MatchStoreTest.kt`**
Test file-based CRUD: saving a `MatchData`, loading it back, verifying byte-for-byte serialization match, duplicating, deleting.

- [ ] **Step 3: Implement `TeamRegistry.kt`**
Read `assets/teams.json` using `kotlinx.serialization`.

- [ ] **Step 4: Implement `MatchStore.kt`**
Write each match to `<filesDir>/matches/<id>.json`. Protect against corrupt files by logging and skipping during `list()`.

- [ ] **Step 5: Run tests and verify PASS**

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/armo/poster/data/ app/src/test/
git commit -m "feat(data): implement TeamRegistry and file-based MatchStore"
```

---

### Task 5: HTML Builder & JavaScript Bridge

**Files:**
- Create: `app/src/main/java/com/armo/poster/renderer/HtmlBuilder.kt`
- Create: `app/src/main/java/com/armo/poster/renderer/PosterBridge.kt`
- Create: `app/src/main/java/com/armo/poster/renderer/RenderResult.kt`
- Test: `app/src/test/java/com/armo/poster/renderer/HtmlBuilderTest.kt`

**Interfaces:**
- Produces: `HtmlBuilder.buildPayload(matchData: MatchData): String` (JSON payload matching `poster.html`'s `window.__armo.build`).
- Produces: `PosterBridge` interface with `@JavascriptInterface` annotated methods `onReady()`, `onFitReport(json: String)`, `onError(message: String)`.
- Produces: `RenderResult` sealed class (`Success`, `TooLong`, `Failed`).

- [ ] **Step 1: Write failing test `HtmlBuilderTest.kt`**
Test payload generation for CHE vs BOU reference data, verifying calculated bar widths, escaped HTML strings, and paired rows.

- [ ] **Step 2: Implement `HtmlBuilder.kt`**
Serialize `MatchData` with derived values into JSON string for JS consumption.

- [ ] **Step 3: Implement `PosterBridge.kt` & `RenderResult.kt`**
Define JS bridge callback interfaces and thread-safe coroutine continuations.

- [ ] **Step 4: Run tests and verify PASS**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/armo/poster/renderer/ app/src/test/
git commit -m "feat(renderer): add HtmlBuilder, PosterBridge and RenderResult types"
```

---

### Task 6: PosterRenderer Engine with Offscreen WebView

**Files:**
- Create: `app/src/main/java/com/armo/poster/renderer/PosterRenderer.kt`
- Create: `app/src/main/java/com/armo/poster/renderer/FitLadder.kt`
- Test: `app/src/test/java/com/armo/poster/renderer/FitLadderTest.kt`

**Interfaces:**
- Produces: `PosterRenderer(context: Context, hostViewGroup: ViewGroup)`.
- API: `suspend fun render(data: MatchData, scale: Int = 2): RenderResult`.

- [ ] **Step 1: Write failing test `FitLadderTest.kt`**
Test fit ladder stepping logic: given mock `contentH` and `innerH`, verify progression from level 0 to level 6, or `TooLong` when level 6 is exceeded.

- [ ] **Step 2: Implement `FitLadder.kt`**
Encapsulate fit decision logic and ladder limits.

- [ ] **Step 3: Implement `PosterRenderer.kt`**
1. Configure offscreen WebView (1200×1200, software layer, `translationY = -5000f`).
2. Set up `WebViewAssetLoader` with `AssetsPathHandler`.
3. Load `https://appassets.androidplatform.net/assets/poster/poster.html`.
4. Inject payload and await `PosterBridge.onReady()` (with 3000ms timeout fallback).
5. Run measurement loop across fit ladder.
6. Allocate `Bitmap.createBitmap(2400, 2400, ARGB_8888)` (scaled 2x) or 1200×1200 (scale 1x).
7. Draw WebView onto `Canvas` and perform blank-bitmap validation check.

- [ ] **Step 4: Verify test suite PASS**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/armo/poster/renderer/ app/src/test/
git commit -m "feat(renderer): implement offscreen WebView PosterRenderer with auto-fit ladder"
```

---

### Task 7: Gallery Export & Share Helpers

**Files:**
- Create: `app/src/main/java/com/armo/poster/export/ExportHelper.kt`
- Create: `app/src/main/java/com/armo/poster/export/ShareHelper.kt`
- Create: `app/src/main/res/xml/file_paths.xml`

**Interfaces:**
- Produces: `ExportHelper.saveToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri?`
- Produces: `ShareHelper.sharePoster(context: Context, bitmap: Bitmap, fileName: String)`

- [ ] **Step 1: Write `res/xml/file_paths.xml`**
Expose cache path `<cache-path name="shared_posters" path="posters/" />`.

- [ ] **Step 2: Implement `ExportHelper.kt`**
Save PNG using `MediaStore.Images.Media` (`RELATIVE_PATH = Pictures/ARMO NOTES`, `IS_PENDING = 1` then `0`).

- [ ] **Step 3: Implement `ShareHelper.kt`**
Write bitmap to cache directory, create URI via `FileProvider.getUriForFile`, launch `Intent.ACTION_SEND` with `image/png`. Fallback to gallery save + toast if no app handles intent.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/armo/poster/export/ app/src/main/res/xml/
git commit -m "feat(export): add MediaStore gallery saver and FileProvider share helper"
```

---

### Task 8: Localization (Persian RTL & English) and Design Theme

**Files:**
- Create: `app/src/main/res/values/strings.xml` (English default)
- Create: `app/src/main/res/values-fa/strings.xml` (Persian translations)
- Create: `app/src/main/res/xml/locales_config.xml`
- Create: `app/src/main/java/com/armo/poster/ui/theme/Color.kt`
- Create: `app/src/main/java/com/armo/poster/ui/theme/Theme.kt`

**Interfaces:**
- Produces: Full bilingual string resources for form fields, errors, fit warnings, dialogs, and Material 3 theme matching ARMO aesthetic (dark green `#0B1A14`, volt accent `#D4FF00`).

- [ ] **Step 1: Create `res/values/strings.xml` & `res/values-fa/strings.xml`**
Define all strings (Gameweek, Home Team, Away Team, Formations, xG, Game Notes, Player Notes, Takeaways, Export, Share, History, Fits, Overflow, etc.).

- [ ] **Step 2: Configure `locales_config.xml`**
Support `en` and `fa`.

- [ ] **Step 3: Define Compose Theme and Palette**
Implement dark theme with ARMO palette (`--bg: #0B1A14`, `--volt: #D4FF00`, `--panel: rgba(255,255,255,0.035)`).

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/ app/src/main/java/com/armo/poster/ui/theme/
git commit -m "feat(ui): add Persian and English string resources and dark theme"
```

---

### Task 9: UI Screens (Editor, Export, History) & Navigation

**Files:**
- Create: `app/src/main/java/com/armo/poster/ui/editor/EditorScreen.kt`
- Create: `app/src/main/java/com/armo/poster/ui/editor/EditorViewModel.kt`
- Create: `app/src/main/java/com/armo/poster/ui/export/ExportScreen.kt`
- Create: `app/src/main/java/com/armo/poster/ui/export/ExportViewModel.kt`
- Create: `app/src/main/java/com/armo/poster/ui/history/HistoryScreen.kt`
- Create: `app/src/main/java/com/armo/poster/ui/history/HistoryViewModel.kt`
- Create: `app/src/main/java/com/armo/poster/ui/navigation/NavRoutes.kt`
- Create: `app/src/main/java/com/armo/poster/MainActivity.kt`

**Interfaces:**
- Produces: Complete Compose user interface allowing editing, live mini preview, exporting with fit check status, and duplicating prior matches.

- [ ] **Step 1: Implement Navigation & `MainActivity.kt`**
Setup NavHost with `Editor`, `Export`, `History` destinations and offscreen WebView frame host.

- [ ] **Step 2: Implement `EditorScreen.kt` & `EditorViewModel.kt`**
- Sticky mini-preview WebView at the top (debounced by 250ms).
- Form cards for GW label, team selection dropdowns (20 clubs with kit color swatch), formations, xG inputs.
- Player cards (one player per card, preventing multiple players per block).
- Single-line takeaway fields.
- Abbreviation input capped at 3 characters.

- [ ] **Step 3: Implement `ExportScreen.kt` & `ExportViewModel.kt`**
- Scaled full-size preview.
- Fit banner: shows green `FITS` or amber `TooLong` warning with overflow pixel count.
- Resolution toggle (2400×2400 deliverable vs 1200×1200 low-RAM mode).
- "Save to Gallery" and "Share to X" buttons.

- [ ] **Step 4: Implement `HistoryScreen.kt` & `HistoryViewModel.kt`**
- List of saved posters with timestamp and match headline (`CHE vs BOU · GW6`).
- Actions: Open in editor, Duplicate (weekly workflow under 2 mins), Delete.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/armo/poster/ui/ app/src/main/java/com/armo/poster/MainActivity.kt
git commit -m "feat(ui): implement EditorScreen, ExportScreen, HistoryScreen and NavGraph"
```

---

### Task 10: Reference Data Verification (CHE vs BOU) & Documentation

**Files:**
- Create: `app/src/main/java/com/armo/poster/data/SampleData.kt`
- Create: `README.md`
- Test: `app/src/test/java/com/armo/poster/ReferenceDataTest.kt`

**Interfaces:**
- Produces: Bundled CHE vs BOU reference match data for immediate 1-click test in the app, and complete build instructions for Android Studio.

- [ ] **Step 1: Write `SampleData.kt`**
Populate exact data for Chelsea vs Bournemouth (GW6) from the spec:
- Chelsea (3-4-2-1), xG: 1.10, Bournemouth (4-2-3-1), xG: 1.38
- Notes: Rogers, Palmer, Henderson, Joao Pedro vs Tavernier, Rayan, Evanilson.
- Takeaways: "Love the 3 Chelsea upfront But Rogers > Palmer for me", "Bournemouth: only Tavernier when fixtures improve".

- [ ] **Step 2: Write `ReferenceDataTest.kt`**
Assert that `SampleData.cheVsBou` conforms to all layout constraints: bar widths, max abbreviations, block counts, and produces `FITS` under the fit ladder.

- [ ] **Step 3: Write `README.md`**
Document how to open the project in Android Studio, build the `arm64-v8a` debug APK, and generate the release keystore.

- [ ] **Step 4: Run tests and verify PASS**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/armo/poster/data/SampleData.kt README.md app/src/test/
git commit -m "feat: add reference data verification and Android Studio setup documentation"
```
