# Multi-angle analysis: what to test (written Oct 3, not yet built or run)

Nothing here has been compiled or tried on a phone. Build first; if it fails, send the first errors.
`DUMMY_MODE` is still `true`. Part A works in dummy mode; Part B needs the real model (`DUMMY_MODE = false`).
Last safe commit before this work: `796fed5` (git can return to it).

## Part A: with DUMMY_MODE = true

1. **Install over the existing app** (do not uninstall). Old scans in History must still be there and open.
2. **Camera:** do a scan taking all 5 photos, then one skipping steps 2 and 4.
   - Progress dots: a dot is filled only for steps that have a photo.
   - Skipping must still reach Results.
3. **Results, all 5 photos:**
   - Text under the title reads "Analysing angle 1 of 1…" (dummy mode analyses the first photo only).
   - Photo row shows 5 thumbnails labelled Front View, Upper Teeth, Lower Teeth, Left Side, Right Side; Front is outlined.
   - Tap another thumbnail: the main image switches to that photo (no boxes, it was not analysed in dummy mode) and the outline moves. Tap Front: the Caries box returns.
   - Long-press a thumbnail: enlarged dialog.
   - "Conditions found (1)" card: Caries, 85%, gauge fills, "Found in: Front View". Tapping it scrolls to the top and shows the front photo.
4. **Results, steps 2 and 4 skipped:** the row shows 3 thumbnails with the right names (Front View, Lower Teeth, Right Side).
5. **Save, then open it from History:** same thumbnails, same switching, Caries box on the front photo, Action Plan ticks still work.
6. **Old scans (saved before this change):** open one. Thumbnails say "Photo 2", "Photo 3"... (not angle names) unless the scan has exactly 5 photos. No crash.
7. **History row:** shows "Dental Caries" for single-condition scans; "N photos" badge still there.
8. **Urdu and dark mode:** angle names, "Found in", "Conditions found" in Urdu; text readable in dark mode.

## Part B: with the real model (DUMMY_MODE = false), the important one

1. Use photos where you know the answer. Check "Analysing angle 1 of 5", then 2 of 5, and so on, and that the photo being analysed appears under the scan line.
2. Total analysis time with 5 photos (expected 1 to 3 seconds). Tell me if it feels slow.
3. **Several conditions:** a scan whose photos show different conditions.
   - Result card shows "2 conditions detected"; "Conditions found (2)" has two rows, each with its own angle in "Found in".
   - The big result card, severity chip and "About this condition" use the worst (highest confidence) condition.
   - Tapping each condition row switches to the photo it was found in and shows its box.
4. **Same condition in several angles:** only the highest-confidence one appears in the list.
5. **Healthy in every angle:** green healthy card, no conditions section, Save hidden.
6. **False positives:** the threshold is low (10%). Check whether weak photos (upper/lower/side) add wrong conditions. If so, tell me; raising the threshold for angles 2 to 5 would be a separate decision.
7. Save, then check History shows e.g. "Caries, Calculus" and that Comparison still works (it uses the worst condition).
8. Reopen the saved scan: thumbnails switch with their own boxes, and the Action Plan matches what you saw before saving, including any ticks.

## Action Plan checklist: conflict review (checked in the code)

No conflict found, with two things to know:
- The plan is rebuilt from the saved detections, grouped per condition with the highest confidence. A new scan saves every detection from every angle, which gives the same conditions and urgency as the live result, so the step numbers (and saved ticks) line up. This is the property to verify in B8.
- Old scans keep their old detections, so their plan and ticks are unchanged.
- With real photos the plan can now have more steps than before (up to 3 conditions), because conditions from any angle count. That only affects new scans.
- Angle switching never rebuilds the Action Plan, so ticks are not reset by it.

## Known limits
- Dummy mode analyses only the first photo.
- Scans saved before this change only have the photos that were taken (skipped steps were not recorded), so their angle names cannot be known and show as "Photo N".
- Photos 2 to 5 are analysed with the same model as the front view; the model may do worse on them.
