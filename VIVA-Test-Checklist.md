# DentalScan AI: final test and rehearsal checklist (Viva Oct 9)

Do this on **Oct 7 to 8**. The feature work is frozen. From here only fix bugs.
Tick each box as you go. Anything that fails: note the screen and what you did, and send it to Claude Code.

---

## 0. Pre-flight (do these first)

- [ ] **Decide `DUMMY_MODE`.** File: `app/src/main/java/ml/TFLiteHelper.kt`, line 14: `const val DUMMY_MODE = true`.
  - `true`: every scan returns a fake "Caries 85%". The camera Capture button is always enabled. Good for testing layouts, **not** for the viva.
  - `false`: the real TFLite model runs and the camera only enables Capture when it sees teeth (see section 5).
  - For the viva you almost certainly want `false`. Test it fully **before** the viva day, with real photos.
- [ ] **Class order matches training.** `CLASS_NAMES = ["Calculus", "Caries", "Gingivitis"]` in `TFLiteHelper.kt`, line 20. This must be the same order your model was trained with. Test one image of each condition and confirm the label is right. A wrong order shows wrong conditions in front of the examiner.
- [ ] Phone battery above 80%, volume up (or voice off in Settings), screen timeout long.
- [ ] Phone storage has free space (the app saves 5 photos per scan).
- [ ] Install a **fresh build** on the phone you will use at the viva.
- [ ] Have a fallback: a second phone or emulator with the same build, and screenshots of every screen.

---

## 1. Test matrix (repeat for English, Urdu, then Dark mode)

Switch language and theme in **Settings → Save & Apply** (the app restarts on Home).

### Home
- [ ] Header, logo, profile pill, Settings and About buttons look right.
- [ ] Tap the whole scan card (including the subtitle), History, Compare, Profiles, Tips. Each opens the right screen.
- [ ] Transitions slide smoothly between screens.

### Camera (needs camera permission on first run)
- [ ] 5 steps advance: Front, Upper, Lower, Left, Right. Step icons and dots update.
- [ ] Skip appears only from step 2.
- [ ] Guide frame turns green when Capture is enabled.
- [ ] Capture works on each step; the last one opens Results.
- [ ] Try skipping some steps: Results still opens with the photos taken.

### Results
- [ ] Scanning animation shows for about 1.6 s, then the result card pops in.
- [ ] Confidence gauge fills and the number matches the percentage.
- [ ] Condition chip, severity chip, correct condition name and color.
- [ ] Bounding box and label on the image match the condition color.
- [ ] "Detected regions" list shows the detections.
- [ ] "Captured photos" row: scrolls, first photo says "Front · analysed", tapping opens the larger photo.
- [ ] Action Plan card and "About this condition" (tap to expand) work.
- [ ] **Save Scan** saves and returns; **Retake** goes back.
- [ ] Healthy photo: green card with a check, and Save is hidden.

### History
- [ ] Stats donut and counts match the saved scans.
- [ ] Rows show colored stripe, "N photos" badge, "82% · High".
- [ ] Tap a row: the saved scan opens (no Save/Retake), with the gauge, photos and regions.
- [ ] Progress banner appears with 2+ scans (see "Known issue" below).
- [ ] Empty state with a new profile; **Start a Scan** opens the camera.

### Comparison (needs 2+ scans)
- [ ] Chart shows Low/Medium/High, dates, colored points.
- [ ] Before/after slider: drag the divider fully both ways; the page does not scroll while dragging.
- [ ] Condition filter changes the chart and images; the slider hides when there are fewer than 2 scans.

### Settings, About, Tips, Disclaimer, Profiles
- [ ] Language, Theme, **Voice Feedback**, reminder switches all save and apply.
- [ ] Voice off: no speech anywhere. Voice on: speech returns.
- [ ] Clear All History shows the dialog; confirm it works (do this last).
- [ ] About shows the academic info, developers, app info and the Detection Model card.
- [ ] Disclaimer checkbox enables the Agree button.
- [ ] Profiles: add, edit, set active; the Active chip shows.

---

## 2. Edge cases that embarrass people in demos

- [ ] Press **back** during the scan animation, then scan again.
- [ ] Rotate the phone on Results and History (the app may not support landscape; make sure it doesn't crash).
- [ ] Deny the camera permission once and confirm the app handles it.
- [ ] Open the app after force-closing it.
- [ ] Airplane mode: the app should work fully offline.
- [ ] A very dark or blurry photo (real model): what does it return?

---

## 3. Known issues to be aware of

1. **History progress banner direction.** The History list is sorted newest first, but `showProgressFeedback` in `HistoryActivity.kt` treats the first half as the *oldest*. The banner may say "improving" when things are actually getting worse, and vice versa. The Comparison screen sorts correctly. This is a two-line fix: ask Claude Code before the viva.
2. **"Severity" is only a confidence band** (High above 80%, Medium above 60%, else Low). It is not a clinical severity. Be ready to say so.
3. **Only the front photo (photo 1) is analysed.** The other 4 are stored and shown, not analysed.
4. **The scanning animation lasts at least 1.6 s** for presentation. The inference itself is faster.
5. **Healthy results can't be saved** (nothing to record), so the Save button is hidden.
6. **No accuracy numbers are in the app.** The About screen shows only the model's facts (architecture, input size, classes, thresholds, size). Add your own mAP/dataset numbers to your slides or report.

---

## 4. Suggested 3-minute demo flow

1. **Home:** "Fully offline dental screening app, English and Urdu." Show the profile pill and the grid.
2. **Start New Scan:** walk through the 5-angle guided capture (voice instructions on). Mention the framing guide turning green.
3. **Results:** point out the scanning animation, the confidence gauge, the colored bounding boxes, the detected regions, the action plan, and the captured photos row.
4. **Save, then History:** show the stats donut, the row stripes, open the saved scan.
5. **Comparison:** show the trend chart and drag the before/after slider.
6. **Settings:** switch to Urdu or dark mode to show it works.
7. **Close:** show the About screen (model details) and the disclaimer.

Keep a second, pre-made profile with several saved scans so History and Comparison look full.

---

## 5. If the camera won't enable Capture (`DUMMY_MODE = false`)

With the real model on, Capture only enables when the guidance analyzer is confident it sees teeth (confidence above 0.7, not too dark, not blurry). If it stays disabled at the viva:
- Improve lighting, move closer, hold steady.
- Test this in the same room and lighting as the viva beforehand.
- Have a rehearsed fallback: show a pre-saved scan from History.

---

## 6. Likely examiner questions (prepare short answers)

- Why YOLOv8n? (small, fast, runs on-device in TFLite)
- Training data: how many images, how were they labelled, train/val/test split?
- Accuracy / mAP, and per-class performance? Limitations?
- Why is only the front view analysed?
- What does "severity" mean here?
- Does it replace a dentist? (No: educational awareness only; see the Disclaimer screen.)
- Privacy: images stay on the device; fully offline.
- What would you improve next? (multi-angle analysis, more data, clinical validation)
