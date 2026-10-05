# Spend Like a Millionaire — Design Spec

Status: **DRAFT for review** · Date: 2026-10-05 · Platform: Android (Google Play)

## 1. Goal and success criteria

An offline Android app that makes the scale of billionaire wealth tangible: the user picks a
public billionaire, receives that person's net worth as a balance, fills a cart with anything from
a coffee to a football club, and sees how little of the fortune they have spent.

Success for v1:
- Published on Google Play, runs fully offline, no backend we operate.
- Smooth on a mid-range phone with a catalog of 300–500 items.
- Passes Play review (no real-person likeness, no third-party logos, parody disclaimer).
- Crash-free (Crashlytics) and shareable (receipt image) so organic sharing is possible.

Non-goals for v1: iOS, accounts, cloud sync, live prices, multiplayer/leaderboards, a 60-second
"speed-run" mode, in-app purchases. AdMob is **planned but built after the app is feature-complete**
(milestone M9, just before launch).

## 2. Requirements (agreed in grooming)

1. Pick a person; balance = their net worth from the most recent / most widely cited public source
   (Forbes or Bloomberg), shown with a source label and month, e.g. "Bloomberg, Sep 2026".
2. Catalog is large and open-ended, from a few dollars to multi-billion-dollar assets, including a
   few named high-value items (a stadium, a football club, a music group). Sort by price ascending
   or descending; filter by category; search by name.
3. Cart with quantities, running total, remaining balance and **% of fortune spent**.
4. Short onboarding (language, display currency, person); all editable later in Settings.
5. Display currency chosen by the user; prices are stored in USD and converted with a bundled,
   static FX table (labelled "approximate").
6. Item prices are curated estimates bundled in the app; no lookup date is shown per item. Named
   valuations (clubs, stadiums, bands) are marked "≈ estimate".
7. Fully offline. Small values (net worths) can be refreshed through Firebase Remote Config without
   a store update; the app must work identically when Remote Config is unreachable.
8. Shareable receipt image.
9. Parody disclaimer; original/open-licensed art only (see §9).

## 3. Approaches considered

| | Approach | Verdict |
|---|---|---|
| **A** | Native Kotlin + Jetpack Compose, single Gradle module, bundled JSON catalog, DataStore, manual DI | **Chosen.** Smallest moving parts for a small, offline, single-platform app. |
| B | Multi-module + Hilt + Room | Rejected (YAGNI): extra build time and KSP/Hilt version friction for ~5 screens and read-only data. Can be adopted later if the app grows. |
| C | Kotlin Multiplatform / Compose Multiplatform | Rejected: iOS is out of scope; revisit only if iOS becomes a goal. |

## 4. Tech stack

Versions were checked against Google Maven / Maven Central / Gradle on 2026-10-05 and are the
latest **stable** releases at that time. They will be pinned in `gradle/libs.versions.toml`;
the exact AGP ↔ Gradle ↔ Kotlin combination is verified when scaffolding (M0).

| Concern | Choice | Version |
|---|---|---|
| Language | Kotlin | 2.4.20 |
| Build | Gradle (wrapper), AGP | Gradle 9.8.0, AGP 9.4.1 |
| JDK | Microsoft OpenJDK (already installed) | 17 |
| UI | Jetpack Compose via BOM, Material 3 | BOM 2026.09.00 |
| Activity / lifecycle | activity-compose, lifecycle-viewmodel-compose | 1.13.0 / 2.11.0 |
| Navigation | Navigation Compose (type-safe routes) | 2.10.2 |
| Persistence | DataStore (Preferences) for settings and cart | 1.2.1 |
| Serialization | kotlinx.serialization (catalog/people/FX JSON in `assets/`) | 1.11.0 |
| Async | Kotlin Coroutines + Flow | 1.11.0 |
| Locale / emoji | AppCompat per-app locales, emoji2 | AppCompat 1.8.0, emoji2 1.7.0 |
| DI | Manual constructor injection via an `AppContainer` | — |
| Firebase | BoM: Crashlytics, Analytics, Remote Config | BoM 34.19.0 |
| Ads (M9 only) | play-services-ads, UMP SDK | 25.5.0 / 4.0.0 |
| Tests | JUnit4, Turbine, Compose UI test | JUnit 4.13.2, Turbine 1.2.1 |
| CI | GitHub Actions: build, unit tests, lint | — |

Targets: `minSdk 26`, `compileSdk 36`, `targetSdk 36` (Google Play requires new apps and updates to
target API 36 or higher since 2026-08-31). Release builds use R8 (minify + shrink resources).

## 5. Architecture

Single module `app`, package-by-layer inside, unidirectional data flow (MVVM + `StateFlow`).

```
app/src/main/java/<applicationId>/
  domain/        # pure Kotlin: Money, Person, Item, Category, CartLine, Currency, use-cases
  data/          # CatalogRepository (JSON from assets), PeopleRepository (+ RemoteConfig overlay),
                 # FxRepository, SettingsStore + CartStore (DataStore)
  ui/
    onboarding/  shop/  cart/  settings/  receipt/  theme/  components/
  platform/      # RemoteConfigSource, Analytics/Crash wrappers, ShareImage, AdsGateway (no-op until M9)
  AppContainer.kt  MainActivity.kt  App.kt
```

Rules:
- `domain` has no Android imports, so it is fast to unit test.
- ViewModels expose one immutable `UiState` per screen and take events as functions.
- Repositories are interfaces in `domain`/`data`; tests use fakes.
- `AdsGateway` is an interface with a no-op implementation now, so M9 does not touch screens.

### Money handling

- All prices and balances are `Long` **USD cents** (largest value ≈ 1.5e12 USD = 1.5e14 cents, well
  inside `Long`). Never `Double` for totals.
- Quantity is bounded by `floor(remaining / unitPrice)`, so `price * qty` cannot overflow or exceed
  the balance. "Buy max" uses the same formula.
- Currency conversion and display formatting happen only at the UI edge (`Double`/`BigDecimal` with
  `NumberFormat`). Large values use compact, localized formatting (e.g. "1.2B", "1,2 tỷ").
- % spent uses adaptive precision so tiny shares stay meaningful (e.g. "0.0004%").

## 6. Data model

All bundled under `app/src/main/assets/`.

`catalog.json` (array):
```json
{ "id": "coffee", "category": "food", "priceCents": 450,
  "name": { "en": "Coffee", "vi": "Cà phê" }, "icon": "☕", "estimate": false }
```
`people.json`:
```json
{ "id": "p_example", "name": { "en": "…", "vi": "…" }, "netWorthUsd": 914000000000,
  "source": "Bloomberg", "asOf": "2026-09", "avatar": { "style": "initials", "color": "#5B8DEF" } }
```
`fx.json`: `{ "base": "USD", "rates": { "VND": 25400.0, "EUR": 0.92, … } }` (static, labelled approximate).

A unit test validates the bundled data on every build: unique ids, positive prices, both languages
present, known categories, every `estimate=true` item is a named valuation.

Persistence (DataStore): selected person, currency, language, onboarding-done flag, cart
(`itemId → quantity`, serialized). Cart is saved so reopening the app keeps it.

### Remote Config

Keys (small JSON strings): `net_worth_overrides` = `{ "<personId>": { "usd": 0, "source": "", "asOf": "" } }`,
plus a `data_version`. Fetched on launch (min interval 12 h), cached by the SDK, **overlaid on the
bundled people**; any parse or network failure silently falls back to the bundled values.
Verified limits (Firebase docs): up to 3,000 parameters and 1,000,000 characters total across values,
so these keys are far below limits. The catalog itself stays bundled.

## 7. Screens and navigation

`Onboarding (language → currency → person)` → `Shop` (home) ⇄ `Cart/Receipt`, plus `Settings`.

- **Shop**: sticky balance bar (remaining, % spent), search, category chips, sort (price ↑/↓),
  lazy list/grid of items with quantity stepper and "buy max".
- **Cart/Receipt**: lines, total, remaining, % spent, "Share receipt" (renders a bitmap and uses
  the Android share sheet), "Clear cart".
- **Settings**: change person / currency / language, parody disclaimer, privacy policy link, app
  version.
- Light and dark themes; supports font scaling and TalkBack labels.

## 8. Localization

English and Vietnamese. UI strings in `strings.xml`; catalog/people names carry both languages in
JSON. Language is applied with AppCompat per-app locales (so MainActivity extends
`AppCompatActivity`) and persists across launches.

## 9. Art, assets and legal posture

- Avatars: stylized, non-resembling (initials, colors, abstract shapes). No photo or AI image that
  depicts or resembles a real person. Generated art is acceptable only when it does not look like
  anyone real and contains no third-party logos.
- Item icons: emoji rendered with emoji2 (zero license cost). A custom vector icon set can replace
  them later without changing the data schema (`icon` may later reference a drawable name).
- Named items (stadium, club, band) are shown by name only, with generic icons and "≈ estimate".
- In-app and on-store disclaimer: parody / entertainment, not affiliated with or endorsed by anyone
  mentioned; net worths and prices are rough public estimates.
- The app and repo do not use any person's name in their title. This is a product-risk posture, not
  legal advice.

## 10. Quality and CI

- Unit tests: money math (overflow bounds, max quantity), formatting, sort/filter/search, FX,
  ViewModel state (Turbine), data-validation test (§6).
- A small set of Compose UI tests for the critical flow (onboard → add item → cart).
- CI on every push/PR: `./gradlew assembleDebug testDebugUnitTest lintDebug`.
- Firebase must not block contributors: `google-services.json` is gitignored (public repo). The
  google-services/Crashlytics plugins are applied only when that file exists, and code uses safe
  wrappers so a build without Firebase still runs.
- No secrets in the repo: keystores, `keystore.properties`, `local.properties` are gitignored.

## 11. Milestones

| # | Milestone | Outcome |
|---|---|---|
| M0 | Foundation | Gradle project, version catalog, theme, nav skeleton, CI, lint, applicationId set |
| M1 | Domain + data | Money/models, JSON loaders, DataStore stores, repositories, unit tests |
| M2 | Shop | Balance bar, list, search, filter, sort, quantity controls |
| M3 | Cart + receipt | Cart screen, % spent comparison, share receipt image |
| M4 | Onboarding + settings | Language/currency/person flow, locale switching, FX conversion |
| M5 | Content | 300–500 items (EN/VI), people list, avatars, validation test green |
| M6 | Firebase | Crashlytics, Analytics, Remote Config net-worth overlay with fallbacks |
| M7 | Polish + QA | Performance, accessibility, dark mode, edge cases, R8 build tested on device |
| M8 | Release prep | Privacy policy (GitHub Pages), store listing + assets, signing, AAB, closed test |
| M9 | AdMob | `AdsGateway` implementation, UMP consent, test IDs → real IDs, update privacy policy + Data Safety |

AdMob is deliberately last: it adds consent flows and policy surface, and the app should be
complete and stable first.

## 12. Release path (summary)

Play Console account, privacy policy URL, Data Safety (declare Firebase Analytics/Crashlytics, and
AdMob at M9), content rating, store graphics, Play App Signing with an upload keystore stored
outside the repo, AAB upload, closed testing (verify the current tester/duration requirement in the
Console at that time), then staged production rollout.

## 13. Open items (need your decision before M0 or at the noted milestone)

1. **`applicationId`** — permanent once published (proposal: `com.michaeo04.spendlikeamillionaire`).
2. Final app name (working title "Spend Like a Millionaire") and short store tagline.
3. Repo **license** (MIT, or all-rights-reserved as today).
4. Initial list of people (proposal: 10–15 public billionaires, names + stylized avatars only).
5. Catalog authoring: Claude drafts 300–500 items with sourced estimates; you review categories
   and the "named valuation" items.
6. Developer-account identity shown on the store (individual vs organization) — affects tester rules.
