# DentalScan AI: UI analysis and prioritized visual improvements

Project: `C:\Users\ACTIVE\StudioProjects\DentalScanAi`
Viva: October 9th
Scope: visual changes only. All features, IDs and logic stay the same. Nothing has been changed yet.

Paths below are relative to `app/src/main/`.

---

## Root problems (they explain most of what looks "student project")

1. **The color scheme breaks your rule.** `colors.xml` has `calculus_purple`, and `ImageUtils.kt` draws Calculus boxes in `Color.MAGENTA`. It should be orange. Bounding boxes use the raw `Color.RED`, `YELLOW` and `GREEN`, and the camera and comparison screens use `android.R.color.holo_*`, the Android 4.0-era palette.
2. **Red is used for two different things.** `AnalysisActivity.showEducationalContent()` fills the Action Plan card red for "high severity". That is the same red as the Caries color, so a high-severity gingivitis result looks like caries. Severity needs its own visual language, such as a chip or a label.
3. **There is no design system.** Text sizes are hard-coded per view (13, 14, 15, 16, 18, 20, 24, 26, 28, 32sp), and colors like `#EEEEEE`, `#666666`, `#DDDDDD`, `#E65100`, `#2E7D32` and `#FFD740` are inline. `themes.xml` uses `DarkActionBar` and stock `Button`/`CardView`, and there are no text styles.
4. **Emojis are everywhere.** There are about 45 across layouts and Kotlin (full list at the end).
5. **The condition name is printed raw from the model.** The label is `gingivitis`, all lowercase, and also shows in sentences like "gingivitis Detected".

---

## Priority 0: shared foundation (about 1 hour, and it makes everything below cheaper)

| File | Change | Why |
|---|---|---|
| `res/values/colors.xml` | Replace `calculus_purple` with `calculus_orange #FB8C00`. Set Caries `#D32F2F`, Gingivitis `#FBC02D`. Add a light tint for each (`caries_bg #FDECEA`, `calculus_bg #FFF3E0`, `gingivitis_bg #FFFDE7`). Move to a deeper medical blue or teal primary (e.g. `#0B6E8A`) with a matching dark variant. Add `divider`, `surface_variant`, and `color_on_yellow` (dark text). | This is the single source for the red/orange/yellow scheme. The tints give you soft chips and card backgrounds instead of solid blocks. |
| `res/values-night/colors.xml` | Mirror the new tokens with dark-safe values. Also fix the theme: it maps `primary_light` to `colorPrimary`. | Dark mode currently drops the condition colors entirely. |
| `res/values/themes.xml` and `values-night/themes.xml` | Switch the parent to `Theme.MaterialComponents.DayNight.NoActionBar`. Add `materialButtonStyle`, `materialCardViewStyle` and `textInputStyle` overrides. Set the status bar to the background color with `windowLightStatusBar`. | Removes the default action bar and gives every screen the same buttons and cards without editing each XML. |
| new `res/values/styles.xml` | Text styles: `TextAppearance.App.Headline` (24sp medium), `Title` (18sp), `Body` (14sp, 4dp line spacing), `Caption` (12sp), `Label` (all-caps 11sp). Add `Widget.App.Button.Primary`/`Secondary`/`Text`, `Widget.App.Card` (16dp radius, 0dp elevation, 1dp divider stroke) and `Widget.App.Chip`. | Flat cards with thin outlines look clinical. The current elevated 4dp cards look like Material 1. |
| new `res/font/` (optional) | Add Inter or Roboto Flex via downloadable fonts and set `fontFamily` in the theme. | The cheapest typography upgrade. |
| new `res/drawable/ic_*.xml` | Add vector icons from Android Studio's Vector Asset tool (Material Symbols). Needed: `camera`, `history`, `compare`, `settings`, `person`, `info`, `lightbulb`, `warning`, `check_circle`, `medical_services`, `expand_more`, `chevron_right`, `arrow_up/down/left/right`, `face`, `school`, `phone_android`. | Every emoji replacement below points at these. |

---

## Priority 1: Results/Analysis screen
Files: `res/layout/activity_analysis.xml`, `java/com/dentalscan/AnalysisActivity.kt`, `java/utils/ImageUtils.kt`

The examiners focus here during the demo.

1. **Result header card** (`activity_analysis.xml`).
   - Replace the bare `resultText` and `confidenceText` under the image with a card containing:
     - a colored condition chip (tint background and a colored dot, driven by the condition);
     - a 28sp title with the display name, e.g. "Dental Caries" instead of "Caries Detected";
     - a confidence row using a `LinearProgressIndicator`, colored by condition, with the percentage on the right;
     - a severity chip (Low/Medium/High).
   - Keep the same IDs. Add only new ones, for example `conditionChip` and `confidenceBar`.
   - Why: a horizontal bar with the number is the standard way clinical apps show a confidence score.
2. **Image container.**
   - Wrap `resultImageView` in a `MaterialCardView` with rounded corners, and replace the fixed 400dp height with a 4:3 aspect ratio or `maxHeight`.
   - Remove the flat gray `#EEEEEE` background.
   - Add a small legend row below it: red, orange and yellow dots with the three condition names.
3. **Bounding boxes** (`ImageUtils.drawDetections`).
   - Use the palette colors: Caries `#D32F2F`, Calculus `#FB8C00`, Gingivitis `#FBC02D`.
   - Raise stroke width to about 6f (or scale it to bitmap size), with rounded corners.
   - Draw the label on a filled pill in the same color instead of loose white text, which is unreadable on light teeth.
   - Match the label case to the display name, so it says "Gingivitis", not "gingivitis".
   - This is drawing code only, and nothing about detection changes.
4. **Action Plan card.**
   - Stop filling the whole card with the severity color.
   - Use a white or tinted card with a 4dp colored left stripe, an icon (`warning`, `schedule` or `check_circle`) in the header, and dark body text.
   - Why: white text on `#FFC107` yellow currently fails contrast, and a solid red block looks alarming rather than professional.
5. **Action Plan text** (`java/utils/ConditionEducation.kt`).
   - The messages begin with the warning, lightning and check emoji, and use `•` bullets in a single string.
   - Strip the emoji from the string and show the icon in the layout instead.
   - Keep the `•` bullets, or render them as separate rows.
6. **"About this condition" card.**
   - Replace `▼ Show` / `▲ Hide` (in the XML and in `AnalysisActivity.kt` lines 276 and 279) with a rotating `expand_more` icon.
   - Add section icons and dividers between What, Causes and Prevention.
   - Change titles like "Causes" to a small all-caps `Label` style.
7. **Buttons.**
   - Retake becomes an `OutlinedButton` with a `refresh` icon. Save becomes a filled button with a `save` icon.
   - Use 52dp height, 12dp radius and bottom-anchored placement.
   - Both are plain `Button` today, and they render as the default gray.
8. **Layout order and spacing.**
   - The disclaimer sits between the buttons and the two cards. Move it to the bottom as a muted caption with an `info` icon.
   - The cards have `marginHorizontal=16dp` inside a 16dp-padded ScrollView, so they are inset twice and narrower than the image. Remove the double padding.
9. **Loading and empty states.**
   - While "Analyzing..." shows, use an indeterminate progress indicator.
   - For "No dental issues detected", show a green `check_circle` and a soft green card.

---

## Priority 2: Home screen
Files: `res/layout/activity_main.xml`, `java/com/dentalscan/MainActivity.kt`

1. **Header.**
   - Put the logo, title and subtitle in a full-width tinted header, or a gradient with rounded bottom corners.
   - Drop the 32sp title in flat blue on gray.
2. **Active-profile badge** (`res/drawable/active_badge_bg.xml`).
   - Turn it into a chip with a person icon and a subtle stroke. A solid green pill reads as "success", not "current user".
3. **Primary action.**
   - Make **Start New Scan** a large hero card or button with a `photo_camera` icon.
   - Add a single line of context such as "5-angle guided scan".
4. **Secondary actions.**
   - Replace the stack of six full-width buttons with a 2×2 grid of icon cards (History, Compare, Profiles, Tips).
   - Move Settings and About to small icon or text buttons in the header.
   - The stack is the most "student" thing on the screen. Every button keeps its ID and listener.
5. **Tips button.** It uses the `secondary` cyan fill, which is off-brand. Make it a card like the others.
6. **Card wrapper.** Remove the outer `CardView` around all the buttons (a card inside the scrolling page adds nothing).
7. **Optional: a footer disclaimer** ("Screening aid, not a diagnosis").

---

## Priority 3: Camera/Scan screen
Files: `res/layout/activity_camera.xml`, `java/com/dentalscan/CameraActivity.kt`, `res/drawable/camera_guide_frame.xml`

1. **Guide frame.**
   - Replace the neon `#00FF00` dashed rectangle with white corner brackets (four L-shaped corners), or a rounded outline.
   - It should turn green when `captureButton` becomes enabled. That is a drawable-state change, not new logic.
2. **Top and bottom panels.**
   - Swap flat `#CC000000` slabs for a gradient scrim (black to transparent) and drop the padding-heavy box look.
3. **Step dots** (`capturedDotsText`, `CameraActivity.updateCapturedDots`, lines 208-210).
   - The emoji-as-progress (green, blue and white circles) and the `○○○○○` text is the worst offender here.
   - Replace it with 5 small `View` circles or a stepper: filled white for done, ringed for current, dim for upcoming.
4. **Step illustration** (`stepIllustrationEmoji`, `CameraActivity.kt` lines 66-106).
   - Show an `ImageView` with a vector icon instead of the smiley and four arrow emoji.
   - Use `face`, `arrow_upward`, `arrow_downward`, `arrow_back` and `arrow_forward`, or draw simple tooth-view icons.
   - Move the card from the hard-coded `marginTop=140dp` into a position anchored under the top panel, so it never overlaps the guide frame on smaller phones.
5. **Guidance banner.**
   - `guidanceText` uses `holo_green_dark` and `holo_orange_dark` (lines 197, 340, 342).
   - Use palette colors and a rounded pill instead of a full-width colored bar. Add a check icon when it's "Perfect! Capture now".
6. **Capture button.**
   - Replace the camera emoji in "Capture" (XML line 175, Kotlin lines 182 and 184) with a large circular shutter button.
   - Or use a Material button with a camera icon, `iconGravity="textStart"`, and text without the emoji.
   - Keep the disabled state visibly dimmed.
7. **Skip button.** Make it a text button with white text, not an outlined one.

---

## Priority 4: History screen
Files: `res/layout/activity_history.xml`, `res/layout/item_scan_history.xml`, `java/adapter/ScanHistoryAdapter.kt`, `java/com/dentalscan/HistoryActivity.kt`

1. **Header.** Give the screen the same header treatment as Home, instead of a bare "Scan History" TextView.
2. **Row layout** (`item_scan_history.xml`).
   - Use a flat outlined card and a 56dp rounded-corner thumbnail.
   - Replace the `›` character with a `chevron_right` icon.
   - Remove the gray `text_secondary` thumbnail background, which shows through as a dark block when the image is missing.
3. **Condition color coding** (`ScanHistoryAdapter.onBindViewHolder`).
   - Add a colored chip or a left color stripe on each row, driven by `scan.condition` (red, orange or yellow).
   - Right now every row looks identical, so you can't scan the list.
   - This is one mapping function shared with the Analysis screen.
4. **Condition text.** Show a proper display name and put confidence and severity on their own line, like "82% · High", not "Confidence: 82% (High)".
5. **Progress feedback banner** (`progressFeedbackText`, `HistoryActivity.kt` lines 96-127).
   - It is a full-width solid block with party, check and warning emoji inside the message text.
   - Turn it into a rounded card at the top with an icon (`trending_up`, `check_circle` or `warning`).
   - Use tinted backgrounds, not solid `caries_red` / `healthy_green` / `primary`.
6. **Empty state.** Replace the plain "No scans yet" text with an illustration or icon, a heading and a "Start a scan" button.
7. **Divider spacing.** Drop the `marginHorizontal=16dp` on the item plus the parent's 16dp padding (same double inset as Analysis).

---

## Priority 5: Everything else

| Screen | File(s) | Change |
|---|---|---|
| Tips | `activity_tips.xml`, `TipsActivity.kt` (lines 33-39) | Replace the lightbulb, toothbrush, salad, check and warning emoji section headers with icon plus title rows. Use tinted card backgrounds per section. "Warning Signs" should use the warning color. |
| About | `activity_about.xml`, `AboutActivity.kt` (lines 27-79) | Replace the eight emoji with icon-plus-text rows (`school`, `account_balance`, `menu_book`, `event`, `person`, `code`, `phone_android`, `warning`). Use a two-column label/value list for the academic details. This screen matters at a viva. |
| Disclaimer | `activity_disclaimer.xml` | Replace the medical, warning and check emoji with `medical_services`, `warning` and `check_circle` vector icons. The `#E65100` and `#2E7D32` inline colors become palette tokens. |
| Comparison | `activity_comparison.xml`, `ComparisonActivity.kt` (lines 168, 177, 206-207) | Use palette colors for chart lines (not `holo_blue_dark` / `holo_red_dark`). Wrap the Before/After images in rounded cards. Restyle the Spinner as an exposed dropdown. Replace `✓` / `⚠` in messages with icons. |
| Settings | `activity_settings.xml` | Swap the old `Switch` for `SwitchMaterial`. Group items in cards with section headers. Replace the inline `#DDDDDD` dividers with a shared divider drawable. |
| Profiles | `activity_profile.xml`, `item_profile.xml`, `res/drawable/circle_avatar_bg.xml` | Replace `● Active` with a chip. Use avatar colors that differ per profile (or by relationship). Turn the header block into a toolbar. |
| Add/Edit profile | `activity_add_edit_profile.xml` | Replace the bare `EditText` and `Spinner` on `surface_white` with `TextInputLayout` outlined fields. |
| Launcher | `res/drawable/ic_tooth_logo.xml`, `ic_launcher_*` | Check that the logo holds up at small sizes and on the new header. |

---

## Full emoji and symbol replacement list

About 45 in total. Most are in Kotlin strings, so the fix touches code, but it is display-only.

**Layouts**
- `activity_about.xml` (3)
- `activity_analysis.xml` (`▼`)
- `activity_camera.xml` (`○○○○○`, smiley, camera)
- `activity_disclaimer.xml` (3)
- `activity_tips.xml` (5)
- `item_profile.xml` (`●`)

**Kotlin**
- `AboutActivity.kt` (about 10)
- `CameraActivity.kt` (about 8)
- `TipsActivity.kt` (4)
- `HistoryActivity.kt` (3)
- `ComparisonActivity.kt` (2)
- `AnalysisActivity.kt` (`▲` / `▼`)
- `ConditionEducation.kt` (3, at the start of each action-plan message)

---

## Suggested order

1. **Priority 0.** Colors, theme and styles.
2. **Analysis screen** (items 1-7) plus the bounding-box colors.
3. **Home screen** grid and header.
4. **Camera** dots, guide frame and buttons.
5. **History** rows with condition color coding.
6. **Emoji sweep** across Tips, About and Disclaimer.
7. **Everything else.**

---

## Things to watch

- Keep every existing `android:id` and every `findViewById` target. All the changes above work with the same IDs, and new views just get new IDs.
- The model label is `gingivitis` (lowercase), and `AnalysisActivity` maps that string. Add display-name and color mapping in one helper (for example `ConditionStyle.kt`) and leave the raw label alone in storage and in the model output.
- The Urdu translation for `gingivitis` is "پلاک" (plaque), which is not the same condition. That is a content point, not visual, so it is not in the list above. It may be worth checking before the viva.
- Yellow (`#FBC02D`) with white text fails contrast, so anything on yellow needs dark text.
