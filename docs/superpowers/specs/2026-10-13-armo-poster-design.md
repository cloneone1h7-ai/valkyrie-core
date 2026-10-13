# ARMO Poster — Android App Design Spec

**Date:** 2026-10-13
**Status:** Approved for planning
**Project folder:** `armo-poster-app/`

---

## خلاصهٔ فارسی (برای مرور سریع)

اپ اندروید سبک (arm64) که همان پوستر ARMO NOTES را می‌سازد: فرم را پر می‌کنی، اپ با WebView خود اندروید
HTML قالب را رندر می‌کند و PNG ۲۴۰۰×۲۴۰۰ می‌دهد — بدون سرور، بدون اینترنت، بدون Node.
قالب، فونت‌ها، ۲۰ لوگو و رنگ‌های تیمی همه داخل اپ هستند. رابط کاربری دوزبانه (فارسی RTL + انگلیسی).
سه صفحه: ویرایش (با پیش‌نمایش زندهٔ کوچک)، خروجی (پیش‌نمایش کامل + ذخیره + اشتراک در X)، تاریخچه.

---

## 1. Purpose

Replace the desktop-only pipeline (Node + Playwright + a local HTML file) with a self-contained
Android app that produces the identical ARMO NOTES poster from a form, on the phone, offline.

**Success criteria**

1. Given the CHE vs BOU reference data, the app produces a 2400×2400 PNG that a human comparing it
   side by side with `outputs/ARMO-NOTES-CHE-BOU.png` calls "the same poster".
2. The user never types HTML, CSS, or a file path. All input is form fields and pickers.
3. Text never overlaps a frame: the app's own fit check reports `FITS` for the reference data, and
   refuses to silently break the design when content is too long.
4. A new gameweek takes under two minutes: duplicate the previous match, change the text, export.
5. Works with no network connection and with no desktop machine involved.

**Non-goals (v1)**

- No server, no cloud rendering, no account, no sync.
- No AI parsing of pasted prose into structured fields (see §14).
- No iOS, no web build.
- No Canva integration.

## 2. Constraints

| Constraint | Value | Why |
|---|---|---|
| Target ABI | `arm64-v8a` only | User's phone; halves APK size |
| minSdk | 26 (Android 8) | Covers ~all current devices; `WebViewAssetLoader` and modern file APIs available |
| compileSdk / targetSdk | 35 | Current Play requirement |
| Offline | Required | No runtime downloads of fonts, crests, or anything else |
| No bundled browser engine | Required | Chromium would add ~100 MB; Android's WebView is already Chromium |
| Toolchain | Android Studio on the user's Windows machine (Path B) | User's explicit choice |

## 3. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ EditorScreen (Compose)                                      │
│   form fields  ──►  MatchData (in-memory state)             │
│                          │                                  │
│                          ├──► MiniPreview (scaled WebView)   │
│                          │                                  │
│                          └──► [Render] button                │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ PosterRenderer                                              │
│   1. build JSON payload from MatchData                      │
│   2. offscreen WebView (1200×1200, software layer)          │
│   3. load poster.html via WebViewAssetLoader                │
│   4. inject payload, await ready signals                    │
│   5. measure content vs available height                    │
│   6. run fit ladder until FITS or ladder exhausted          │
│   7. draw to Bitmap(2400×2400) with canvas.scale(2,2)       │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
        ┌──────────────────┴──────────────────┐
        ▼                                     ▼
  MediaStore (Pictures/ARMO NOTES)      ShareHelper → ACTION_SEND
```

**Why WebView and not native Canvas drawing.** The approved design already exists as locked HTML/CSS,
and Android's WebView is the same Chromium engine that produced the approved PNGs. Re-implementing
the layout in `android.graphics.Canvas` would mean hand-writing text wrapping, bullet indentation and
bar sizing — a large surface for silent divergence from the approved design. WebView keeps the design
in one place.

**Why two WebViews.** A single on-screen WebView cannot be captured above the screen's own
resolution. The mini preview is for editing feedback; the offscreen renderer is the source of truth
for output pixels. They share the same HTML and the same JSON, so they cannot drift.

## 4. Components

Each unit below states what it does, how it is used, and what it depends on.

### 4.1 `PosterRenderer`

- **Does:** turns a `MatchData` into a `Bitmap`. Owns the offscreen WebView lifecycle.
- **Used by:** `ExportViewModel`. API: `suspend fun render(data: MatchData, scale: Int): RenderResult`.
- **Depends on:** `HtmlBuilder`, `PosterBridge`, `assets/poster/*`.
- **`RenderResult`** is a sealed type: `Success(bitmap, fitLevel, metrics)` or
  `TooLong(overflowPx, fitLevel)` or `Failed(reason)`.

Attach strategy: the offscreen WebView is added to a `FrameLayout` host inside the Activity content
with `translationY = -5000f` (attached and laid out, but off-screen) and an explicit
`measure(EXACTLY 1200, EXACTLY 1200)` / `layout(0, 0, 1200, 1200)`. It is not removed between
renders — only reloaded — so the WebView process stays warm.

### 4.2 `PosterBridge`

- **Does:** the `@JavascriptInterface` surface between JS and Kotlin.
- **Methods:** `onReady()`, `onFitReport(json)`, `onError(message)`.
- **Depends on:** nothing.
- **R8 note:** the release build must keep these methods — see §9.

### 4.3 Fit engine (JS, in `fit.js`)

- **Does:** measures content height against available height and applies cumulative fit classes.
- **Used by:** `PosterRenderer` (Kotlin calls `window.__armo.measure()` and `window.__armo.fit(n)`).
- **Depends on:** the `.inner` and `.slide` elements in `poster.html`.

```js
window.__armo = {
  measure() {
    const inner = document.querySelector('.inner');
    let contentH = 0;
    for (const el of inner.children) {
      if (el.dataset.spacer !== undefined) continue;   // flexible spacers never count
      contentH = Math.max(contentH, el.offsetTop + el.offsetHeight);
    }
    return { innerH: inner.clientHeight, contentH: Math.round(contentH) };
  },
  fit(n) { document.querySelector('.slide').className = 'slide' + (n ? ' fit' + n : ''); }
};
```

The fit classes are declared in `poster.html`'s stylesheet (§7) so they are inert at level 0 and
inspectable in a plain browser.

### 4.4 `MatchData`, `TeamRegistry`, `MatchStore`

- **`MatchData`** — the whole poster payload. Kotlin data class, `@Serializable`. §5.
- **`TeamRegistry`** — reads `assets/teams.json` once at startup into a map of club name →
  `(abbr, color, crestAssetPath)`. Read-only after init.
- **`MatchStore`** — CRUD over JSON files in `filesDir/matches/`. API:
  `list(): List<MatchSummary>`, `load(id): MatchData`, `save(data): String`, `duplicate(id): String`,
  `delete(id)`. One file per match so a corrupt file cannot take down the history.

### 4.5 Screens

| Screen | Responsibility | Notes |
|---|---|---|
| `EditorScreen` | Form for every variable field; sticky mini live preview at the top | Debounce preview updates by 250 ms |
| `ExportScreen` | Full-size preview (fit to screen), Save to gallery, Share to X, fit-status banner | Shows `FITS` or the "text too long" message |
| `HistoryScreen` | List of saved matches; open, duplicate, delete | "Duplicate" is the weekly workflow |

Navigation: three destinations, no nested graphs. State held in a single `EditorViewModel` plus a
`HistoryViewModel`.

### 4.6 `ShareHelper`

- **Does:** writes the PNG to a cache file, exposes it via `FileProvider`, fires
  `Intent.ACTION_SEND` with type `image/png`.
- **Depends on:** `res/xml/file_paths.xml`.
- **Fallback:** if no app handles the intent, save to gallery and show a toast instead of crashing.

### 4.7 i18n

- Bilingual: Persian (default for the user) and English.
- `res/values/strings.xml` (English, default) + `res/values-fa/strings.xml`.
- Switch via `AppCompatDelegate.setApplicationLocales(...)`; declared with
  `android:localeConfig` so Android 13+ shows the app in system per-app language settings.
- Compose picks up RTL automatically from the locale. **The poster itself is always LTR English** —
  only the app chrome is localised.

## 5. Data model

```kotlin
@Serializable
data class MatchData(
    val id: String,
    val createdAt: Long,
    val gwLabel: String,              // "GW6"  → rendered as "NOTES · GW6"
    val home: TeamRef,
    val away: TeamRef,
    val homeFormation: String,        // "3-4-2-1"
    val awayFormation: String,        // "4-2-3-1"
    val homeXg: Double,               // 1.10
    val awayXg: Double,               // 1.38
    val gameNotes: List<String>,      // 1–2 bullets
    val homeBlocks: List<PlayerBlock>,// left column
    val awayBlocks: List<PlayerBlock>,// right column
    val takeaways: List<String>       // 1–3 single-line bullets
)

@Serializable
data class TeamRef(
    val name: String,                 // "Chelsea"  (full name, shown in the panel)
    val abbr: String,                 // "CHE"      (headline, max 3 chars)
    val color: String,                // "#034694"
    val crestAsset: String?           // "logos/chelsea-apisports.png"; null = custom crest
)

@Serializable
data class PlayerBlock(
    val name: String,
    val notes: List<String>           // 1–3 lines
)
```

**Derived values are never stored.** They are computed at render time so they cannot go stale:

| Derived | Rule |
|---|---|
| Bar widths | larger xG = 100%; other = `xg / max * 100%` |
| Panel labels | `"$name ($formation)"` |
| Headline | `"$homeAbbr vs $awayAbbr"` |
| Column pairing | row *i* pairs `homeBlocks[i]` with `awayBlocks[i]`; the shorter list is padded with empty blocks |

## 6. Rendering pipeline

```
1.  payload = json.encodeToString(MatchData)
2.  webView.loadUrl("https://appassets.androidplatform.net/assets/poster/poster.html")
3.  onPageFinished →
4.      evaluateJavascript("window.__armo.build($payload)")
5.      await PosterBridge.onReady()          ← fires when BOTH:
6.          • document.fonts.ready has resolved
7.          • Promise.all([...document.images].map(i => i.decode())) resolved
8.      timeout after 3000 ms → proceed with a warning flag
9.  n = 0
10. repeat up to 6 times:
11.     metrics = evaluateJavascript("JSON.stringify(window.__armo.measure())")
12.     if metrics.contentH <= metrics.innerH → break
13.     n += 1; evaluateJavascript("window.__armo.fit($n)")
14.     (allow one animation frame for layout before re-measuring)
15. if still overflowing after n == 6 → return TooLong(overflowPx, 6)
16. bitmap = Bitmap.createBitmap(2400, 2400, ARGB_8888)
17. canvas = Canvas(bitmap); canvas.scale(2f, 2f)
18. webView.draw(canvas)
19. return Success(bitmap, n, metrics)
```

Step 6 is the single most important correctness detail: capturing before Anton has loaded silently
substitutes a fallback font and invalidates every measured dimension.

**Bitmap scale.** `scale = 2` produces 2400×2400 (the deliverable). A user-visible toggle offers
`scale = 1` (1200×1200) for low-memory devices.

## 7. Auto-fit ladder

Declared in `poster.html` as inert CSS. At level 0 the rendered poster is byte-identical to the
locked design; each level adds a class that tightens spacing.

| Level | Class | Change |
|---|---|---|
| 1 | `.slide.fit1` | `.rows .r { padding: 8px 0 7px }` |
| 2 | `.slide.fit2` | `.rows .r { padding: 6px 0 5px }` |
| 3 | `.slide.fit3` | `.bullets li { margin-top: 1px; line-height: 1.28 }` |
| 4 | `.slide.fit4` | section gaps `14px → 10px`, `12px → 8px` |
| 5 | `.slide.fit5` | `.panel { padding: 10px 16px }` |
| 6 | `.slide.fit6` | `.matchrow img { 92px }`, `.headline { font-size: 74px }` |

Levels are cumulative (1+2+…+n applied together; later rules win by source order).

**Floor:** no level shrinks body text below 19px, player names below 29px, or the takeaway title
below 27px. If level 6 is exhausted, the app reports `TooLong` with the remaining overflow in pixels
and asks the user to shorten a note. It never crops, never clips, and never silently drops a line.

To support level 4 the two fixed spacer divs in the template gain a class (`gap-a`, `gap-b`) and the
flexible trailing spacer gains `data-spacer`. These are additive attributes: they change no rendered
pixel at level 0.

## 8. Layout rules enforced by the app

These are enforced in code, not left to the user:

1. **One player = one block.** The form renders one card per player; there is no field where several
   players can be typed into one block. This structurally prevents the "Tavernier, Rayan & Evanilson"
   failure mode.
2. **Takeaway bullets are single-line.** Each takeaway is its own single-line text field.
3. **Row-aligned columns.** Pairing and padding happen in `HtmlBuilder`; the template's
   `align-items: center` then centres short right-hand blocks against their row.
4. **Team colour is automatic.** Picked from `TeamRegistry`; the user can override it with a colour
   picker but cannot leave it empty.
5. **Bar length is automatic** and not user-editable.
6. **Locked elements are not exposed in the UI at all:** the FPL/ARMO chip, the two watermarks, the
   "BIGGEST TAKEAWAY" title, the background, the card softness, the hairline separators.
7. **Abbreviation length.** The headline field is capped at 3 characters.

## 9. Build and release configuration

```kotlin
android {
    defaultConfig {
        minSdk = 26
        targetSdk = 35
        ndk { abiFilters += "arm64-v8a" }
    }
    androidResources { noCompress += listOf("ttf", "otf") }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

`proguard-rules.pro` must contain:

```
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
```

Without this, R8 strips the JS bridge in release builds and rendering fails only in release — a
classic and expensive-to-diagnose bug.

Signing: v1 ships a debug-signed APK (installable immediately). A `release` signing config is
included and documented; the user runs one `keytool` command to create
`keystore/armo-release.jks` and the config picks it up from `keystore.properties` (git-ignored).

Dependencies kept deliberately small:

- `androidx.compose` (BOM) + Material 3
- `androidx.activity:activity-compose`
- `androidx.navigation:navigation-compose`
- `androidx.webkit:webkit` (for `WebViewAssetLoader`)
- `androidx.appcompat:appcompat` (for `AppCompatDelegate` locale APIs)
- `kotlinx-serialization-json`

Saving to the gallery uses the framework `MediaStore` API directly — no extra dependency.
No Room, no Hilt, no Retrofit, no image-loading library.

**Version set** (Gradle / AGP / Kotlin / Compose BOM) is pinned to a known-good combination in the
project. Android Studio may offer an AGP upgrade on first open; accepting it is optional. The first
build verifies the combination, and any correction is a one-line change in the version catalog.

## 10. Asset pipeline

Copied from the existing skill kit (`~/.workbuddy-ai/skills/armo-match-poster/assets/`) into
`app/src/main/assets/`:

| From | To | Size |
|---|---|---|
| `template.html` | `poster/poster.html` (+ fit ladder, + `gap-a`/`gap-b`/`data-spacer`) | 8 KB |
| *(new)* | `poster/fit.js` | 1 KB |
| `fonts/Anton-Regular.ttf`, `fonts/Inter.ttf` | `fonts/` | ~500 KB |
| `logos/*.png` (20 clubs) | `logos/` | ~1.1 MB |
| `teams.json` | `teams.json` (+ `color` per club) | 4 KB |

`teams.json` gains a `color` field per club (primary kit colour) because the app needs it at runtime;
the existing `id`, `abbr` and `crest` fields are unchanged.

**Crest resolution at runtime:** `TeamRegistry` maps a club to `logos/<slug>-apisports.png`. A
club outside the bundled 20 uses a crest the user picks from the gallery, copied into
`filesDir/crests/` and referenced as an absolute `file://` URL in the payload. If a crest is missing
entirely, the app draws a neutral circle with the abbreviation rather than a broken-image icon.

## 11. Error handling

| Failure | Behaviour |
|---|---|
| Fonts not ready within 3 s | Render anyway, mark `fontTimeout = true`, warn in the export banner |
| Content still overflowing after level 6 | `TooLong`; banner: "متن X پیکسل بلندتر است — یک یادداشت را کوتاه کن" with the overflow amount |
| `Bitmap` allocation fails (OOM) | Retry once at `scale = 1`; if that fails, surface a clear error |
| Crest file missing | Neutral placeholder circle with the abbreviation; non-fatal warning |
| `webView.draw()` produces a blank bitmap | Detect by sampling: if the bitmap is a single flat colour, retry once after `invalidate()`; if still blank, report failure rather than shipping a blank PNG |
| No app handles the share intent | Save to gallery + toast |
| Corrupt match JSON | Skip that file in `HistoryScreen`, log its name, keep the rest |

## 12. Testing

**JVM unit tests**
- `BarWidths` — 1.10/1.38 → 79.7%/100%; equal values → 100%/100%; zero handling.
- `ColumnPairing` — 4 left + 3 right → 4 rows, last right block empty; 3+4 → symmetric.
- `TeamRegistry` — parses all 20 clubs, every `crest` path resolves to a bundled asset.
- `MatchStore` — save → list → load → duplicate round-trips `MatchData` exactly.
- `FitLadder` — given `contentH`/`innerH` pairs, selects the expected level; returns `TooLong` past 6.
- `AbbrValidation` — rejects >3 characters.

**On-device (instrumented / manual)**
- `PosterRenderer` smoke test: render the CHE vs BOU reference data, assert the bitmap is 2400×2400
  and not a single flat colour.
- Visual check against `outputs/ARMO-NOTES-CHE-BOU.png`.
- Manual checklist (carried over from the desktop skill):
  1. Export banner reports `FITS`.
  2. No text touching or overlapping the panel or the takeaway card.
  3. No wrapped takeaway bullet.
  4. Hairline rows align across both columns.
  5. Both crests present.
  6. Every number and name matches what was typed.

Pixel-diff golden tests are deliberately **not** used: WebView versions differ across devices and
antialiasing would produce false failures.

## 13. Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Offscreen `WebView.draw()` unreliable on some devices | Medium | Software layer + explicit measure/layout + blank-bitmap detection with retry (§11). Verify on the user's phone first; this is the first thing to build. |
| Font loading race produces wrong metrics | Medium | JS ready signal on `document.fonts.ready` + image `decode()`; 3 s timeout with a visible warning |
| R8 strips the JS bridge in release | Medium | Explicit keep rule (§9); test a release build, not only debug |
| Preview and export diverge | Low | Both drive the same HTML and the same JSON; the mini preview is the same WebView at a different scale |
| Bitmap memory (23 MB at 2×) | Low | 1× toggle; single bitmap in flight; recycle after save |
| User's Android Studio version rejects the pinned AGP | Low | Version catalog makes the fix a one-line change |

## 14. Out of scope for v1 (candidates for v2)

- Parsing pasted prose into structured fields (would need on-device heuristics or an API call; the
  form already prevents the failure mode it would fix).
- Auto-fetching crests for clubs outside the bundled 20 from a CDN.
- Multiple poster themes or a landscape/4:5 variant.
- Batch generation for several matches at once.
- Direct posting to X via the X API (v1 uses the system share sheet, which requires no credentials).

## 15. Open questions

None. All decisions below were resolved with the user before this spec was written.

| Decision | Resolution |
|---|---|
| Native app vs PWA vs both | Native Android app |
| Build path | Android Studio on the user's machine |
| v1 scope | Full: 20-club picker with auto colour, all form fields, auto-fit, history |
| UI language | Bilingual — Persian (RTL) + English |
| Poster language | Always English, unaffected by UI language |
