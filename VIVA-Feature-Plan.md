# DentalScan AI: Viva feature plan (Oct 9)

Project: `D:\Developer\DentalScanAi` (Android, Kotlin, YOLOv8n TFLite on device)
Last updated: Oct 2, 2026. Viva: Oct 9, 2026 (7 days).
Companion file: `DentalScanAI-UI-Improvement-Plan.md` (the visual redesign, already implemented in code but **not yet compiled or tested on a phone**).

---

## 0. Where things stand

- The UI redesign (Steps 1 to 6: theme, Results, Home, Camera, History, emoji sweep) is written but **has never been built**. A Gradle build was attempted from the assistant's side but the disk filled up, so nothing has been compiled.
- Disk space on C: is tight. The project now lives on D: (`D:\Developer\DentalScanAi`), and about 5.9 GB of installers were moved to the USB (`E:\data transfer on usb`).
- `TFLiteHelper.DUMMY_MODE` is **`true`**, so every scan currently returns a fake "Caries 85% High". It must be `false` for the real demo.
- Condition matching is now case-insensitive everywhere (`Gingivitis` vs `gingivitis`).

**Rule: get the build green and phone-tested before adding anything new.**

---

## 1. How long it takes with Claude Code

Claude Code writes the code quickly (minutes per item). The real time cost is **you building and testing on a phone**, because the assistant cannot compile or run the app on this machine. Each batch needs 1 to 2 build/fix cycles.

| Batch | Contents | Claude Code writing | Your build + test | Calendar time |
|---|---|---|---|---|
| A. Make the app demo-safe | Turn off `DUMMY_MODE`, verify class order, fix compile errors from Steps 1 to 6 | 20 to 40 min | 1 to 2 h (first build, device test) | Oct 2 to 3 |
| B. Results screen polish | Scanning animation, animated confidence gauge, staggered card reveal | 45 to 75 min | 30 min | Oct 4 |
| C. Multi-angle + chart | 5-photo thumbnail row (Results), photo-count badge (History), chart styling with Low/Medium/High labels | 60 to 90 min | 30 to 45 min | Oct 5 |
| D. Comparison slider + transitions | Before/after slider view, cheap slide/fade screen transitions | 60 to 90 min | 30 to 45 min | Oct 6 (feature freeze) |
| E. Optional extras | Findings list, stats donut on History, severity rework, mute switch, share report | 2 to 4 h total | 1 to 2 h | Only if time remains |
| F. Final test + rehearsal | English, Urdu, dark mode, full demo run | n/a | 3 to 4 h | Oct 7 to 8 |

**Total:** about 4 to 6 hours of Claude Code work for batches A to D, plus roughly 4 to 6 hours of your own build and test time. Spread over 4 days it is comfortable. All of it can realistically fit by Oct 6.

---

## 2. Feasibility, effort and risk per idea

| # | Idea | Do it? | Human-equivalent hours | With Claude Code | Risk |
|---|---|---|---|---|---|
| 1a | Animated confidence gauge | Yes | 2 to 3 | ~30 min | Low |
| 1b | Animated card reveal | Yes | 1 | ~15 min | Low |
| 1c / 3 | 5-angle thumbnails (Results + History) | Yes | 3 to 4 | ~45 min | Low to medium |
| 2a | Before/after draggable slider | Yes | 3 to 5 | ~45 min | Low to medium |
| 2b | Chart styling (labels, curve, animation) | Yes | 1.5 to 2 | ~30 min | Low |
| 4 | Scanning animation while analysing | Yes (best value) | 1.5 to 2 | ~30 min | Low |
| 5a | Cheap screen transitions (slide/fade) | Yes | 1 | ~15 min | Low |
| 5b | True shared-element transitions | No | 6+ | ~1.5 h | Medium |

### 1a. Animated confidence gauge
- **Approach:** Material `CircularProgressIndicator` (already in the Material 1.11 library) with `setProgressCompat(pct, true)`. Count the percentage up in the middle with a `ValueAnimator`. Color it with `ConditionStyle.color(...)`.
- **Files:** `res/layout/activity_analysis.xml`, `AnalysisActivity.kt` (`showResultUi`).
- **Risk:** additive only; detection and save logic untouched.
- **Option:** a hand-drawn arc (custom `View` and `drawArc`) looks nicer but costs more. Start with the Material one.

### 1b. Animated card reveal
- **Approach:** after the result is shown, fade each card in and slide it up about 24dp with a short stagger (`animate().alpha(1f).translationY(0f)`).
- **Files:** `AnalysisActivity.kt`.
- **Risk:** keep the final state identical to now so nothing breaks if animations are off.

### 3. 5-angle thumbnails (answers 1c as well)
- **Good news:** no database change. `ScanEntity.imagePath` already stores the JSON array of all photos, and `getAllImagePaths()` exists.
- **Results:** a horizontal row of rounded thumbnails under the main image; tap to enlarge in a dialog. Works for live scans and view-only scans.
- **History:** a small "N photos" badge on each row.
- **Files:** `activity_analysis.xml`, `AnalysisActivity.kt`, `item_scan_history.xml`, `ScanHistoryAdapter.kt`.
- **Risk:** load bitmaps with `inSampleSize` and off the main thread.
- **Caveat:** skipped steps are not recorded, so photo 2 may not be "Upper Teeth". Label them "Photo 1 to 5", or store the step name in the capture flow (riskier).
- **Only photo 1 (front) is analysed.** Keep it that way before the viva; say so openly. Analysing all 5 would change what is saved.

### 2a. Before/after slider
- **Approach:** a custom `BeforeAfterView` that draws the "before" bitmap, then the "after" bitmap clipped at a draggable X, with a handle icon. No new library needed (important with the tight disk).
- **Placement:** a new card **above** the existing side-by-side cards, so nothing is removed.
- **Files:** new `BeforeAfterView.kt`, `activity_comparison.xml`, `ComparisonActivity.kt`.
- **Risk:** scale both bitmaps identically (center-crop) and sample them down to avoid out-of-memory.

### 2b. Chart styling
- **Is MPAndroidChart available?** Yes: `com.github.PhilJay:MPAndroidChart:v3.1.0` is in `build.gradle` and already used in `ComparisonActivity.kt`.
- **Upgrades:** cubic curve, gradient fill, `animateX(800)`, **Y labels "Low / Medium / High" instead of 1/2/3**, dates on the X axis.
- **Files:** `ComparisonActivity.kt` only.

### 4. Scanning animation
- **Approach:** a horizontal gradient "scan line" `View` over the image card, animated up and down with `ObjectAnimator` (`translationY`, repeat reverse) while `analyzeImage()` runs. Stop and fade out when the result arrives. Add a pulsing "Analyzing…" label.
- **Files:** `activity_analysis.xml`, `AnalysisActivity.kt`.
- **Honest caveat:** YOLOv8n inference is fast (a few hundred ms), so enforce a minimum display time of about 1.2 s so the animation is visible. This is a UX choice; say so if asked.

### 5. Transitions
- **Camera to Results:** not a good fit for a shared element (the camera preview does not exist on the results screen), and it risks conflicts with `finish()` in `CameraActivity`.
- **History to Results:** feasible with Material's `MaterialContainerTransform` (min SDK 25 is fine), but 6+ hours and medium risk around `BaseActivity`, view-only mode and back navigation.
- **Recommendation:** do the cheap version (slide/fade between all screens via `overridePendingTransition` or Material's `MaterialSharedAxis`). Skip true shared-element transitions.

---

## 3. Additional improvements (ordered by value)

1. **Use the real model.** Set `DUMMY_MODE = false` and test with real photos. Verify `CLASS_NAMES = ["Calculus", "Caries", "Gingivitis"]` matches your training class order. A wrong order mislabels results in front of the examiner. (Minutes.)
2. **Severity is just a confidence band** (High above 80%, Medium above 60%). An examiner may ask what it means. Either rename it "confidence level", or compute it from the detection box size as a share of the image (1 to 2 h). Decide before the viva.
3. **Findings list for all detections** (~2 h). Only the top detection is shown, but `currentDetections` holds all. A small list shows the model finds multiple conditions.
4. **Stats donut on History** (~2 to 3 h): scans by condition, using MPAndroidChart `PieChart`.
5. **Share / export a report** (~3 to 5 h): PDF or image through the share sheet. Needs a `FileProvider`; the riskiest extra. Only if time is left.
6. **Healthy scans cannot be saved.** `saveScan()` returns when nothing is detected, so the Save button looks broken. At minimum disable or hide it (30 min). Saving a "Healthy" scan properly touches History and Comparison.
7. **Model details on the About screen** (30 min): accuracy, dataset size, classes. Examiners ask this.
8. **Mute switch for voice feedback** in Settings (1 h), so voice does not interrupt the demo.

---

## 4. Recommended schedule

| Day | Work |
|---|---|
| Oct 2 to 3 | Batch A: build green, phone-tested, `DUMMY_MODE` off, class order verified, crashes fixed |
| Oct 4 | Batch B: scanning animation, gauge, card reveal |
| Oct 5 | Batch C: thumbnails, History badge, chart labels and styling |
| Oct 6 | Batch D: before/after slider, cheap transitions. **Feature freeze.** |
| Oct 7 to 8 | Batch F: full test (English, Urdu, dark mode), rehearse, fix only bugs |
| Oct 9 | Viva |

If time gets tight, drop in this order: true shared-element transitions, the slider, share/export. The gauge, scanning animation, thumbnails and chart labels give the most impact per hour.

---

## 5. Prompt to paste into Claude chat (to continue elsewhere)

```
I'm preparing the viva (Oct 9) for DentalScan AI, an Android app in Kotlin that detects
Caries, Calculus and Gingivitis on-device with a YOLOv8n TFLite model. The app works; I only
want additions, nothing removed or broken.

Context:
- minSdk 25, Material Components 1.11, MPAndroidChart v3.1.0 (already used), Room, CameraX.
- A visual redesign was already done (teal theme, ConditionStyle helper, Material cards).
- Only photo 1 of up to 5 captured photos is analysed; all photo paths are stored as a JSON
  array in ScanEntity.imagePath (getAllImagePaths() exists).
- ConditionStyle maps the raw label (Caries / Gingivitis / Calculus, matched case-insensitively)
  to display name and color: red = Caries, orange = Calculus, yellow = Gingivitis.

Please help me implement, one at a time, keeping all existing IDs, listeners and logic:
1. Scanning animation over the image while inference runs (min ~1.2 s).
2. Animated circular confidence gauge (CircularProgressIndicator + count-up).
3. Staggered fade/slide reveal of the result cards.
4. Horizontal thumbnail row of all captured photos on the Results screen
   (tap to enlarge) and a "N photos" badge on History rows.
5. MPAndroidChart styling on the Comparison screen (Low/Medium/High Y labels,
   dates on X axis, smooth curve, animateX).
6. Before/after draggable slider (custom View) added above the existing side-by-side images.
7. Simple slide/fade transitions between screens.

For each, tell me which files change, what to test, and any risk to existing features.
```
