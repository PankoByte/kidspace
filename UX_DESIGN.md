# Kid Space: parent experience redesign

The parent’s task is to lend their own phone for a short period, with a chosen set of apps and a clear way to take it back. The app should make that task visible before asking the parent to understand Android settings.

## What was getting in the way

- The first meaningful setup step was an Accessibility permission. Parents had not yet made the choices that explain why the permission exists.
- “Handover” described an action but did not identify children or restricted app access.
- The same illustrated, serif-heavy visual language appeared on adult setup and child screens. It weakened the distinction between configuring the phone and handing it over.
- The main screen summarized app counts rather than showing the child’s available apps.
- The default-home request was explained briefly, without enough context about the Home button or returning to the normal launcher.

## The implemented flow

```text
Welcome: Your phone. Just their apps.
  Interactive example: chosen apps + timer + parent PIN
          |
1. Choose apps
   Familiar app icons, selected rows, search, selection count
          |
2. Choose a session limit
   15 / 30 / 45 / 60 minutes, custom, or no limit
          |
3. Enable app protection
   Reason -> Android instructions -> disclosure -> verified return
          |
4. Create and confirm parent PIN
          |
Parent home: review their space and session length
          |
Start kid mode
   Explain Home button -> Android home selection -> child home
          |
Parents -> PIN -> normal launcher
```

The welcome screen is not counted as a setup task. Existing stored step IDs remain unchanged; an explicit ordered list controls the new sequence and back navigation. The PIN remains the completion boundary, preserving existing installations.

The child’s playful home and time’s-up treatment remain appropriate for the person using those screens. The redesign concentrates on the adult’s preparation, permission decisions, review and return path.

## Visual direction

The organizing image is a bounded collection of app tiles. It appears in the brand mark, the welcome example and the parent’s review. It shows what the product does without needing a feature carousel.

| Token | Value | Purpose |
| --- | --- | --- |
| Background | `#F5F7FC` | Quiet parent workspace |
| Surface | `#FFFFFF` | App tiles and editable controls |
| Ink | `#192C4D` | Adult headings and readable text |
| Action | `#365DD9` | Primary actions and setup progress |
| Soft blue | `#E8EEFF` | Guided examples and Android instructions |
| Preview mint | `#DCEFEA` | The child’s chosen space |

Karla is used for parent headings, numbers and body text. The welcome headline is 38 sp; other screen titles are 30 sp. Body copy is 15 sp, with smaller supporting text reserved for secondary details. Content is left aligned; primary actions remain at the bottom while long content scrolls. The existing Fraunces type remains on the child home.

The app icon now uses four app tiles instead of the Android project template icon. This helps parents recognize the same app when Android asks them to select a home app.

## Specific interaction decisions

- Show the real selected app icons and labels on the parent home, not just a count. This is a compact preview of the choices, not a full interactive child session.
- Call the permission “App protection” in the product. Mention “Accessibility” where the parent needs to find it in Android.
- Do not offer a misleading “Not now” on a required protection step. Back navigation is available, and setup can resume later.
- After returning from Android settings, check that the service is actually connected. A switch marked on by Android alone is not enough.
- Explain the home-app request immediately before opening it. Explain both the Home button behavior and the return to the parent’s launcher.
- If the request is cancelled, put the recovery message directly above Start so it cannot disappear below the fold.
- Keep checkbox and radio-button selection semantics, labelled search, generous touch targets and scrolling content for larger text.
- The parent home explicitly says kid mode is off. “App protection is ready” means configured, not that a session has already begun.

## Naming options

| Name | Strength | Tradeoff |
| --- | --- | --- |
| **Kid Space** | Most immediate explanation of a limited area for the child; used as the working name in this build | Descriptive and generic; availability and potential confusion need checking before release |
| **KidTurn** | Connects naturally to lending a phone for a turn; more distinctive as a brand | Needs the tagline to communicate app restrictions |
| **Kids Only** | Clearly suggests that only selected child-facing content belongs here | Can sound like a children’s content service |

Recommended positioning: **“Your phone. Just their apps.”** This communicates the shared-phone use case more directly than a broad parental-control claim. No trademark, store-name or domain availability investigation was performed.

## Product boundaries to carry into future work

Allowing an app gives access to its existing account and content. It does not create a separate Android user, remove purchases, hide photos inside that app, or filter its content. The app picker now explains this at the point where the parent chooses access.

PIN recovery is not implemented. The PIN screen now tells parents this before they save a PIN. A deliberate recovery design is a useful next product decision.

Normal launcher restoration can vary by device. The handover explanation acknowledges that some phones ask the parent to choose their normal home app again.

These checks validate the redesigned experience on the supplied emulator; they are not an exhaustive assessment of resistance to every Android escape path or manufacturer variation.

## Verification

- Debug build and all 27 existing unit tests passed.
- Android lint completed with no errors. Remaining warnings concern dependency/tool versions, existing API/resource details and existing implementation suggestions.
- Walked through welcome, interactive Games example, app search and selection, session time, Accessibility consent, return-to-app status, PIN creation and confirmation, and parent home.
- Checked the Android home-app explanation, cancellation recovery and successful session start.
- Opened an allowed app, used Home to return to child mode, and verified that an unselected Android Settings screen was redirected to the child home.
- Verified an incorrect PIN is rejected and the configured PIN returns to the normal Pixel launcher.
- Checked the parent home and handover dialog at 360 × 640 dp with 1.3 font scale. Their content scrolls and primary actions remain available. Restored the emulator’s original display and font settings afterward.
- Emulator setup uses PIN `1234`, Clock and YouTube as test selections, and a 30-minute session. Those selections are test fixtures, not recommendations for children.

Screenshots from the walkthrough are in `.codex-qa/`. The redesigned APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Code map

This is a native Kotlin / Jetpack Compose application. `MainActivity` chooses parent onboarding, parent routes or the active child session. `LauncherViewModel` connects the UI to Room-backed allowed apps and profiles, DataStore settings, PIN verification and the session manager. `KidSessionManager` persists session transitions and timer state. The Accessibility service and `GuardRuntime` enforce the app allowlist; `HomeSwitch` enables the launcher alias only for handover. The redesign builds on those existing boundaries.

Primary UI changes live in `kid/setup/Onboarding.kt`, `AppPicker.kt`, `ParentHome.kt` and the new `SpacePreview.kt`, with shared parent typography, color and button changes in `core/theme` and `core/ui/Components.kt`.
