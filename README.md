# Auto Bot 1.4.0

Setup now has a shared trigger delay from 0–10 seconds, default 3 seconds. It is added before the recording's first tap wait. A green countdown above the floating icons shows total time until the first tap in both normal and debug sessions. Pause, focus/geometry failure or Stop cancels pending playback. Later taps retain recorded timing. Changing the delay applies to the next trigger.

Watch-picture thresholds apply only to triggering. All rules then replay saved coordinates with recorded timing, without per-tap image matching or a minimum button score. Existing recordings work without re-recording. Foreground, pause, capture, screen geometry and Accessibility checks remain.

## Changes in this update

- Small blue numbered taps appear on the Setup screenshot and live zone editor. Tap the numbered-taps caption to review a step. New recordings save each step's own full screen; older recordings show their marker on the watch screenshot, which may differ from later menu screens.
- Recording, replay and tap diagnostics use the capture display dimensions. Accessibility-window metrics are logged for comparison but no longer set replay coordinates. Saved positions and working rule maps are preserved. This addresses a potential coordinate mismatch; the reported Leave miss still needs confirmation on the phone.
- The F launcher starts with diagnostics hidden. Opening Setup or calibration enables the setup diagnostics session.
- Runtime tap crosshairs are smaller. Button-picture match boxes are hidden by default and can be enabled from the overlay grip menu. They are optional visual references only.
- Watch zones trigger a recording; taps can be anywhere on the display, independently of the watch zone.


A clean native Android app with **two launcher shortcuts sharing one set of settings**:

- **Auto Bot** — retains the original Fortnite F icon. Its standalone launcher has its own Android task, starts/reuses consented capture, and opens official Fortnite. It no longer inherits Setup's rule cards. Only a brief launch/permission screen is shown when needed.
- **Auto Bot Setup** — configures the three independent watch areas and tap recordings.

This project contains the complete Java source. It does not patch or depend on the old companion APK. The F PNG is the only asset carried over from that APK. It does not contain or modify Fortnite itself.

## Install and start

Install [`Auto-Bot-1.4.0.apk`](artifacts/Auto-Bot-1.4.0.apk) over this rebuild's 1.0/1.1/1.2 without uninstalling. It uses the same signing key and preserves the existing setup as **Screen 1** under each rule. It is separate from the original lost-source companion (`com.kev.fortniteautobot` versus that app's old package). Turn off that old companion's automation/Accessibility service before testing this one.

1. Open **Auto Bot Setup**.
2. Grant **Overlay permission** and enable **Auto Bot taps** in Android Accessibility settings. Grant notifications using its button as well. Android/Samsung may require allowing restricted settings in the app's App info before enabling a sideloaded Accessibility service.
3. Tap **Open Fortnite**. Approve Android capture of the entire display when prompted. New capture sessions need fresh consent; an existing running session can be reused.
4. Configure each rule using the steps below. All three switches are **on by default**. A rule with no complete setup shows **SETUP** and does not send taps. Your later switch choices are saved.
5. For daily use tap the **Auto Bot** F icon. It opens Fortnite using the saved rules. There are no account logins or PC connections needed for normal use.

## Three separate rules

| Rule | Overlay icon | Saved setup | Initial watch threshold |
|---|---|---|---|
| Auto Ready | Tick | Ready indicator screenshot, watch rectangle, and its tap sequence | 90% |
| Auto Leave | Bus | Bus indicator screenshot, watch rectangle, and its tap sequence | 65% |
| Auto Party | Three silhouettes | Your chosen party indicator screenshot, watch rectangle, and its tap sequence | 90% |

Auto Party is a configurable watch-and-replay rule: record whichever party UI actions you want. It does not assume a particular friend, invite, or fixed party screen.

Each rule supports **multiple screen variants**, each with **its own watch screenshot, zone, threshold, and tap recording**. During recording, small target patches are also saved as visual references only. These are local setup assets, not additional watch rules.

### Multiple layouts / screen variants

Open a rule in Setup and use **Add screen**. Name it, then import/capture its picture, select its zone, and record the taps for that layout. Use the Screen dropdown to choose which variant you are editing. Rename/Remove affect that screen only. Up to 16 variants per rule are supported; one switch still controls the whole Ready, Leave, or Party/Join rule.

The app evaluates the saved variants at runtime and uses the matching screen's own tap coordinates. It does not reuse one layout's map on another. If more than one screen matches, it chooses the highest score; an exact tie uses the first saved screen. A near-tied current choice stays stable during confirmation so flickering scores do not keep resetting recognition. **Only one recording plays for that appearance**, even when multiple variants match; another replay requires all matching screens of that rule to disappear for three valid frames.

Long-pressing an in-game icon offers a screen chooser when that rule has multiple variants. Recording and screenshot saves stay bound to the selected screen, even if another settings view changes the selection. Existing 1.0/1.1 settings and old backup files are understood as Screen 1.

### Teach directly over Fortnite

1. In Setup choose **Set up / edit**, select the screen variant, then **Set zone over game**. Fortnite opens with floating zone controls. If capture is not running, the app first guides you through the normal capture consent. While already in-game, **hold a tick/bus/party icon**, choose its variant if prompted, and the controls open directly.
2. In **ZONE MODE**, drag a **green rectangle** around the desired stable indicator. Tap to position the **magenta crosshair**. Crosshairs and filled selection zones are visible over the actual live game. Drag the control panel by its status label if it covers the area you need.
3. Choose **Game input** to temporarily remove the selection surface and use Fortnite normally; choose **Select zone** to mark an area again. Automatic rules stay paused during setup.
4. Press **Save zone**. The controls/guides hide briefly; the app waits for a fresh clean frame, then saves the screenshot and selected region without baking in the coloured marks. You remain over Fortnite.
5. Press **Record taps**. If you have just drawn an unsaved zone, it is saved first. Otherwise the already saved watch screenshot is retained, even if you have navigated to another screen using Game input.
6. The recording panel starts paused. Arrange the starting game screen, press **Record**, and tap each control **once**, waiting for the recording controls to reappear after each tap. The app captures the target's appearance and forwards the tap through Android Accessibility. Record up to 30 taps.
7. Use **Pause** for normal game input or while arranging a screen. Only single short taps are recorded. The recording panel can also be dragged by its status label.
8. **Done** saves the recording and returns to the normal icon column over Fortnite; it does not bounce into Setup. **Cancel** keeps the previous tap recording. Setup's **Done** discards any unsaved zone edits. Resume selected rules from a paused icon or the grip menu.

For Ready include the Ready word/icon so Cancel and matchmaking do not look the same. For Leave exclude changing seconds and scenery where possible. Choose a distinctive party indicator for Party.

### Or import a normal screenshot

In each rule's Setup editor choose **Import screenshot**. Android's image picker accepts normal PNG/JPEG screenshots (and other image formats the phone supports). Use an uncropped landscape screenshot of the complete Fortnite display from the same phone and game UI scale. Import works without an active capture session.

The imported image fits inside the preview without stretching. **Drag to draw the green filled zone with a bright border/corners. Tap to move the magenta crosshair without erasing the zone.** Letterbox margins are excluded from coordinate mapping. Guides are annotations only and never become part of the recognition pixels or change the recorded tap coordinates. A new image starts with an empty draft zone so an old image's selection is not silently reused. Importing a draft does not replace the working watch template until you press **Save watch area**.

The percentage box is an image-similarity threshold (50–99), not a probability of correct recognition. After saving, use **Record taps** to record over the game, or **Set zone over game** for live editing.

The area and taps use normalized full-display coordinates. Keep the same landscape rotation, game UI scale, and display settings used for calibration. Changing rotation/aspect while saving a new watch area clears that rule's incompatible old tap sequence. Tap the recorded-tap count in the editor to review coordinates and timings.

## Floating control

The overlay is one narrow column: a grip, **tick**, **bus**, and **party silhouettes**.

- **ON**: selected, configured, and monitoring.
- **OFF**: switch disabled.
- **SETUP**: selected but no complete watch/recording yet.
- **PAUSED**: selected, but all automation is paused.
- **ERROR**: that rule's replay failed; inspect Setup and toggle it off/on to retry after correcting it.

Tap an active rule icon to switch it off; tap an off icon to switch it on. Tap a selected paused icon to resume selected rules. Hold an icon to select its watch zone and record over Fortnite. Tap the grip to see scores/status and pause/resume or open Setup (including screenshot import).

Drag the grip to move the overlay. A circular **X target** appears near the bottom centre while dragging. Drop the overlay onto that target to remove it **and stop capture and all automation**. Dropping at the bottom corners only moves it. There is no permanent Close button. Dragging temporarily pauses gestures. The foreground notification also has a Stop action.

## Recognition and replay

Frames are sampled at up to four per second and downscaled to at most 960 pixels wide. Each rule needs three fresh matching frames. It dispatches once per appearance and requires three valid non-matching frames before a later appearance can trigger, with at least five seconds between triggers for a rule. Missing frames and app switches do not count as proof that an indicator disappeared.

Only one tap sequence runs at once. If more than one rule can trigger, the order is **Leave, Party, Ready**. Recorded taps play at their saved coordinates without image checks. Recorded waits are preserved (bounded to 0.3–15 seconds). Leaving Fortnite, locking the phone, disabling Accessibility, stale capture, rotation, pause, or stop invalidates pending work.

The status says **tap/recording sent** after successful Android dispatch. This is not proof that Fortnite accepted the action or reached the lobby. Watch the first real replay of each rule and test nonmatching screens.

## Constant live debug overlay

Enabled for Setup sessions by default; the F launcher starts with diagnostics off. Toggle **Setup diagnostics (F launcher starts without debug)** in Setup or use the grip menu. It keeps evaluating recognition while automation is paused, so you can test a watch picture without allowing automatic taps.

The overlay shows:

- Coloured watch outlines and score/threshold labels: green Ready, cyan Leave, yellow Party/Join.
- The selected screen variant, three-frame watch confirmation, and the exact current reason for waiting.
- The next recorded button patch, magenta aiming crosshair arms, tap number and target pixel coordinates, saved-coordinate playback status.
- A red marker for the last requested tap and Android's completed/rejected/cancelled result.
- Frame counter/age, capture size, rotation, actual foreground package, Accessibility state, pause state, and last action.

Reasons include wrong saved rotation/aspect, missing setup, disabled Accessibility, stale frames, game focus lost, a blank/flat watch area, score below threshold, waiting for the picture to clear, or the floating controls covering a watch/button area. Move the icon column away if that warning appears. Flat watch pictures are rejected; recorded taps do not require image detail.

The debug surface is non-touchable, with an opacity below Android's touch-obscuring limit. It draws **outside** the sampled watch/button areas, leaving transparent holes and a small interpolation margin so the debug graphics do not become recognition pixels. Crosshair arms can consequently have a gap over the sampled button itself; target coordinates remain explicit in the panel. The panel automatically chooses a corner with the least overlap. Use **Move debug panel** in the grip menu to cycle corners if its text is clipped by protected sampling areas. In-game zone editing already supplies its own coloured guides. Diagnostics are hidden briefly for clean screenshots and recording captures, then return.

Use **Export debug log** in Setup after a missed action. It includes the last in-game screen/variant diagnostics and recent Android tap/error events, even though opening Setup changes foreground focus. A completed Android tap is evidence of dispatch, not proof Fortnite acted on it.

## Export and import — entirely on the phone

In Setup use **Export all setup** to save a ZIP through Android's file picker, including every screen variant of all three rules, current switch choices, watch pictures, thresholds, coordinates, button patches, and recordings. Use **Import setup** to restore a ZIP made by this app. No cable, debugging, root, or PC is needed. Backup format 2 includes the variant catalog; this version also imports older format-1 backups. Older app versions should not be used to restore these new multi-screen backups.

Import pauses automation, validates the archive and rule assets, stages it separately, then replaces the setup. An invalid archive does not replace the current setup. The previous rule directory is retained internally when a valid import replaces it. Android overlay, notification, and Accessibility permissions still have to be granted separately.

## Source and signing key

The private signing key is intentionally excluded from Git. Keep the original key backed up privately: it is required to update existing installations. To use it locally, place it at `signing/auto-bot.jks` (ignored by Git). The personal build configuration uses alias `autobot` and store/key password `android`; the private key itself is not included.

Signing certificate SHA-256:

`724059b1c7b04806988ee1cef7b35f90af8d4706f6b3607a34c32ebd88b892a9`

### Android Studio / Gradle

Open this folder in Android Studio with JDK 17 and Android SDK platform 35/build-tools 35.0.0. The project uses Android Gradle Plugin 8.7.3 and Gradle 8.9. The official Gradle wrapper and distribution checksums are included.

```powershell
./gradlew.bat assembleDebug
./gradlew.bat lintDebug
```

The normal Gradle APK is `app/build/outputs/apk/debug/app-debug.apk`. With the original key present, debug and release builds use that key. Without it, Gradle debug uses the standard development key and release is unsigned. A different key cannot update an existing installation. The portable script creates a new personal key if none exists; that new key also cannot update existing installations.

### Portable Windows build

No Android Studio or Gradle is required for the alternate build. In PowerShell 7:

```powershell
./setup-tools.ps1
./build.ps1 -ToolsDir ./tools
```

The setup script downloads portable Temurin JDK 17, Android platform 35, and build tools 35.0.0 into this folder, verifying pinned SHA-256 checksums. `build.ps1` runs the JVM tests, compiles the Java/resources, converts to DEX, aligns/signs/verifies the APK, and writes `artifacts/Auto-Bot-1.4.0.apk`. It does not use Apktool or the old APK. The bundled JSON-Java JAR is used only by storage tests, with its checksum checked; it is not included in the Android app, which uses Android's built-in JSON APIs.

## Validation and remaining phone tests

Completed: clean Java compilation; AAPT2 resource/manifest packaging; D8; APK alignment/signature verification; both launcher names/icons verified; original F asset checksum matched; Gradle `assembleDebug` and `lintDebug` (no errors; nonblocking UI/localization warnings remain); JVM tests for independent triggers, confirmation, interruption/freshness, flat/textured matching, coordinates, drop-zone and image-preview mapping, variant selection/tie stability, one-shot behavior for overlapping variants, debug mask/panel geometry, and bounded archive extraction. Storage tests exercise the actual RuleStore with test-only file/preferences scaffolding: legacy migration, unchanged original data on read, independent pictures/tap maps, frozen recording selection, parent switches, full export catalog, and rejecting saves to a deleted screen. These tests do not mock image processing as successful.

No phone was connected for this build. The reported launcher/diagnostic problems were inspected in source and addressed; reproducing the user's intermittent Fortnite behavior still needs phone evidence. Installation, launch/task behavior, capture, diagnostics appearance/touch pass-through, recognition with overlays, variant selection on real screens, complete tap sequences, setup export/import, and performance need supervised testing. This build provides live reasons and exportable evidence rather than claiming the intermittent issue has been proven resolved on the device.

References used: [Android MediaProjection](https://developer.android.com/media/grow/media-projection), [Accessibility gestures](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService), [Overlay touch pass-through rules](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_TOUCHABLE), [AGP 8.7 compatibility](https://developer.android.com/build/releases/agp-8-7-0-release-notes).
