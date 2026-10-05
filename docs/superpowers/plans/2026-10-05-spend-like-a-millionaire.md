# Spend Like a Millionaire Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and ship-ready v1 of an offline Android app where the user picks a billionaire and spends their net worth on a large catalog.

**Architecture:** Single Gradle module `app`, Kotlin + Compose, MVVM with `StateFlow`, pure-Kotlin `domain`, bundled JSON catalog, DataStore persistence, manual DI (`AppContainer`). Money is `Long` USD cents.

**Tech Stack:** Kotlin 2.4.20, Gradle 9.8.0, AGP 9.4.1, Compose BOM 2026.09.00, Navigation Compose 2.10.2, DataStore 1.2.1, kotlinx.serialization 1.11.0, Coroutines 1.11.0, Firebase BoM 34.19.0, JUnit4 + Turbine 1.2.1.

**Spec:** `docs/superpowers/specs/2026-10-05-spend-like-a-millionaire-design.md`

## Global Constraints

- applicationId and root package: `com.michaeo04.spendlikeamillionaire`; app name "Spend Like a Millionaire".
- `minSdk 26`, `compileSdk 36`, `targetSdk 36`; JDK 17; release builds use R8 (minify + shrink resources).
- All prices/balances/totals are `Long` USD cents. Never `Double` for totals. Conversion/formatting only at the UI edge.
- Quantity bound = `floor(remaining / unitPrice)`; `price * qty` must never overflow or exceed the balance.
- Offline-first: app fully works with no network; Remote Config only overlays small net-worth values and silently falls back.
- Languages: English + Vietnamese (UI strings in `strings.xml`, catalog/people names carry both languages).
- No real-person likeness, no third-party logos; parody disclaimer in-app. No person's name in the app/repo title.
- Public repo: never commit keystores, `keystore.properties`, `local.properties`, `.env`, `google-services.json`.
- Firebase plugins applied only if `app/google-services.json` exists; code must run without Firebase.
- Commit identity is repo-local (noreply email). Commit messages end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Environment (already set): `JAVA_HOME` JDK17, `ANDROID_HOME=D:\Android\Sdk`, `GRADLE_USER_HOME=D:\Android\gradle`, AVD `Pixel_8_API_36`. PowerShell sessions must reload PATH from the User/Machine env.
- AdMob (spec M9) is OUT OF SCOPE of this plan; it is built after the app is complete.

## Review Focus

1. Quantity input that is 0, negative, or larger than affordable → clamped to `[0, max]`, never throws (T1, T3).
2. Persisted cart contains an item id that no longer exists in the bundled catalog (after an app update) → that line is dropped, no crash (T3).
3. Remote Config returns malformed JSON, zero, or negative net worth, or an unknown person id → bundled value is used (T9).
4. User switches to a poorer person while the cart total exceeds the new balance → cart is cleared after confirmation; balance can never go negative (T3, T7).
5. Currency with no FX rate, zero-decimal currencies (VND), and values ≥ 1e12 → formatting never crashes or overflows, falls back to USD (T1, T7).

## File Structure

```
settings.gradle.kts  build.gradle.kts  gradle.properties  gradle/libs.versions.toml
.github/workflows/ci.yml  LICENSE
app/build.gradle.kts  app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/assets/{catalog.json,people.json,fx.json}
app/src/main/res/{values/strings.xml, values-vi/strings.xml, xml/locales_config.xml, ...}
app/src/main/java/com/michaeo04/spendlikeamillionaire/
  MainActivity.kt  App.kt  AppContainer.kt
  domain/{Models.kt, Money.kt, CartMath.kt, Formatting.kt}
  data/{Dtos.kt, AssetCatalogRepository.kt, AssetPeopleRepository.kt, AssetFxRepository.kt,
        DataStoreSettingsStore.kt, DataStoreCartStore.kt, Repositories.kt}
  platform/{RemoteConfigSource.kt, ShareReceipt.kt, AdsGateway.kt}
  ui/{theme/Theme.kt, nav/AppNav.kt, onboarding/*, shop/*, cart/*, settings/*, components/*}
app/src/test/java/.../{domain/*, data/*, ui/*}
```

---

### Task 0: Project foundation (Gradle, CI, license, hello screen)

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/xml/locales_config.xml`, `app/src/main/java/com/michaeo04/spendlikeamillionaire/MainActivity.kt`, `.github/workflows/ci.yml`, `LICENSE`, wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`)
- Test: `app/src/test/java/com/michaeo04/spendlikeamillionaire/SanityTest.kt`

**Interfaces:**
- Produces: a buildable Compose app whose `MainActivity` extends `AppCompatActivity` and shows text; version catalog aliases used by all later tasks (`libs.androidx.compose.bom`, `libs.kotlinx.serialization.json`, `libs.androidx.datastore.preferences`, `libs.androidx.navigation.compose`, `libs.turbine`, …).

- [ ] **Step 1: Generate the Gradle wrapper** — download Gradle 9.8.0 to `D:\Android\gradle-dist`, run `gradle wrapper --gradle-version 9.8.0` in the repo root.
- [ ] **Step 2: Write the build files** (version catalog with the versions in Tech Stack; `app/build.gradle.kts` with namespace/applicationId above, compose enabled, `kotlin-serialization` plugin, unit-test deps, `testOptions.unitTests.isReturnDefaultValues = true`; conditional google-services apply).
- [ ] **Step 3: Write failing sanity test** `SanityTest` asserting `1 + 1 == 2`; run `./gradlew testDebugUnitTest` → FAIL until the project compiles.
- [ ] **Step 4: Write manifest, resources, `MainActivity` (hello text), CI workflow, MIT LICENSE.**
- [ ] **Step 5: Run** `./gradlew assembleDebug testDebugUnitTest lintDebug` → PASS.
- [ ] **Step 6: Commit** `build: scaffold Android project, CI and MIT license`.

### Task 1: Money math and formatting (pure Kotlin, TDD)

**Files:**
- Create: `domain/Money.kt`, `domain/Formatting.kt`
- Test: `app/src/test/.../domain/MoneyTest.kt`, `FormattingTest.kt`

**Interfaces:**
- Produces:
```kotlin
object Money {
    fun maxQuantity(remainingCents: Long, unitCents: Long): Long      // 0 if either <= 0
    fun lineTotal(unitCents: Long, quantity: Long): Long              // Math.multiplyExact
    fun percentSpent(spentCents: Long, totalCents: Long): Double      // 0.0 when total <= 0
    fun clampQuantity(requested: Long, max: Long): Long               // coerceIn(0, max.coerceAtLeast(0))
}
object Formatting {
    fun percent(p: Double, locale: java.util.Locale): String          // adaptive precision, "0.0004%"
    fun money(usdCents: Long, currency: String, rate: Double?, locale: java.util.Locale): String
    // rate==null -> falls back to USD; uses compact form ("1.2B"/"1,2 tỷ") for >= 1_000_000 major units
}
```

- [ ] **Step 1: Write failing tests** covering: `maxQuantity(1000,300)==3`, zero/negative inputs → 0, `maxQuantity(Long.MAX_VALUE,1)==Long.MAX_VALUE`, `lineTotal` overflow throws `ArithmeticException`, `clampQuantity(-5,10)==0`, `clampQuantity(99,10)==10`, `percentSpent(0,0)==0.0`, `percent(0.0004)` shows ≥2 significant digits, `percent(100.0)=="100%"`, `money(…, "VND", null, …)` falls back to USD and does not throw, `money(150_000_000_000_000L,"USD",1.0,…)` (1.5e12 USD) formats compactly.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests "*MoneyTest" --tests "*FormattingTest"` → FAIL (unresolved).
- [ ] **Step 3: Implement** `Money` and `Formatting` (BigDecimal for the conversion, `NumberFormat.getCompactNumberInstance` fallback implemented manually for API 26: thresholds K/M/B/T with localized suffix map for en and vi).
- [ ] **Step 4: Run tests** → PASS.
- [ ] **Step 5: Commit** `feat(domain): money math and formatting`.

### Task 2: Domain models, JSON data, repositories, validation test

**Files:**
- Create: `domain/Models.kt`, `data/Dtos.kt`, `data/Repositories.kt`, `data/AssetCatalogRepository.kt`, `data/AssetPeopleRepository.kt`, `data/AssetFxRepository.kt`, `assets/catalog.json` (seed ~40 items), `assets/people.json` (seed 5), `assets/fx.json`
- Test: `data/BundledDataValidationTest.kt`, `data/DtoParsingTest.kt`

**Interfaces:**
- Produces:
```kotlin
enum class Category { FOOD, SHOPPING, TECH, TRANSPORT, HOME, TRAVEL, FUN, SPORTS_MUSIC, MEGA }
data class LocalizedText(val en: String, val vi: String) { fun get(lang: String): String }
data class Item(val id: String, val category: Category, val priceCents: Long,
                val name: LocalizedText, val icon: String, val estimate: Boolean)
data class Person(val id: String, val name: LocalizedText, val netWorthUsd: Long,
                  val source: String, val asOf: String, val avatarColor: String)
interface CatalogRepository { suspend fun items(): List<Item> }
interface PeopleRepository  { suspend fun people(): List<Person> }
interface FxRepository      { suspend fun rates(): Map<String, Double> }   // includes "USD" -> 1.0
```
- JSON parsing is a pure function `parseCatalog(json: String): List<Item>` etc. in `Dtos.kt` so tests need no Android.

- [ ] **Step 1: Write failing tests**: `DtoParsingTest` (valid item parses; unknown category is skipped not fatal; missing `vi` falls back to `en`); `BundledDataValidationTest` reads `src/main/assets/*.json` from disk and asserts unique ids, `priceCents > 0`, non-blank names in both languages, `estimate=true` items only in categories SPORTS_MUSIC/MEGA, every person has `netWorthUsd > 0` and an `asOf` matching `\d{4}-\d{2}`, fx contains `USD`.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** models, DTOs (`@Serializable`, `ignoreUnknownKeys`), parse functions, asset-backed repositories (read via `Context.assets`, cached with `lazy`), write seed JSON.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(data): domain models, JSON loaders and data validation`.

### Task 3: Cart logic and stores (TDD)

**Files:**
- Create: `domain/CartMath.kt`, `data/DataStoreSettingsStore.kt`, `data/DataStoreCartStore.kt`
- Test: `domain/CartMathTest.kt`, `data/CartSanitizeTest.kt`

**Interfaces:**
- Consumes: `Money`, `Item`.
- Produces:
```kotlin
data class Cart(val lines: Map<String, Long> = emptyMap())
object CartMath {
    fun total(cart: Cart, items: Map<String, Item>): Long             // unknown ids ignored
    fun setQuantity(cart: Cart, item: Item, requested: Long, balanceCents: Long, items: Map<String, Item>): Cart
        // clamps to floor((balance - totalOthers)/price); 0 removes the line
    fun sanitize(cart: Cart, items: Map<String, Item>, balanceCents: Long): Cart // drops unknown ids, trims to affordability
}
data class Settings(val onboarded: Boolean, val personId: String?, val currency: String, val language: String)
interface SettingsStore { val settings: Flow<Settings>; suspend fun update(transform: (Settings) -> Settings) }
interface CartStore { val cart: Flow<Cart>; suspend fun save(cart: Cart) }
```

- [ ] **Step 1: Write failing tests**: set 3 of $300 item with $1000 balance → 3; requested 99 → 3; requested −1 → line removed; two items share the balance correctly; `sanitize` drops an id missing from catalog; `sanitize` trims a cart whose total exceeds a smaller balance (never negative remaining).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** `CartMath`; DataStore implementations (cart serialized as JSON string under one preferences key; decode failure → empty cart).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(domain): cart math, sanitize and DataStore stores`.

### Task 4: App shell — theme, AppContainer, navigation skeleton

**Files:**
- Create: `App.kt`, `AppContainer.kt`, `ui/theme/Theme.kt`, `ui/nav/AppNav.kt`, `platform/AdsGateway.kt` (interface + `NoOpAdsGateway`); Modify: `AndroidManifest.xml` (application class), `MainActivity.kt`
- Test: `ui/nav/StartDestinationTest.kt`

**Interfaces:**
- Produces: `AppContainer(context)` exposing `catalog`, `people`, `fx`, `settingsStore`, `cartStore`, `ads`; `sealed interface Route { Onboarding, Shop, Cart, Settings }` (type-safe `@Serializable` objects); `fun startRoute(settings: Settings): Route` (Onboarding when `!onboarded`, else Shop); Material 3 light/dark theme.

- [ ] **Step 1: Failing test** for `startRoute`. **Step 2: Run** → FAIL. **Step 3: Implement** shell + placeholder screens. **Step 4: Run tests + `assembleDebug`** → PASS. **Step 5: Commit** `feat(ui): app shell, theme and navigation`.

### Task 5: Shop screen (sort, filter, search, quantity)

**Files:**
- Create: `ui/shop/ShopViewModel.kt`, `ui/shop/ShopScreen.kt`, `ui/components/BalanceBar.kt`, `ui/components/ItemRow.kt`
- Test: `ui/shop/ShopViewModelTest.kt`

**Interfaces:**
- Consumes: repositories, `CartMath`, stores.
- Produces:
```kotlin
enum class SortOrder { PRICE_ASC, PRICE_DESC }
data class ShopUiState(val balanceCents: Long, val spentCents: Long, val percentSpent: Double,
    val items: List<ItemUi>, val query: String, val category: Category?, val sort: SortOrder, val loading: Boolean)
data class ItemUi(val item: Item, val quantity: Long, val maxQuantity: Long)
class ShopViewModel(...) { fun setQuery(q: String); fun setCategory(c: Category?); fun setSort(s: SortOrder)
    fun setQuantity(itemId: String, qty: Long); fun buyMax(itemId: String) }
```

- [ ] **Step 1: Failing ViewModel tests** (Turbine + fakes): sort ascending/descending by price; category filter; case-insensitive search over the active language and `en`; `setQuantity` over max is clamped; `buyMax` spends exactly the affordable maximum; state `percentSpent` reflects cart. **Step 2: Run** → FAIL. **Step 3: Implement** VM and screen (LazyColumn, stable keys, sticky balance bar). **Step 4: Run** → PASS; install on emulator and screenshot to verify layout. **Step 5: Commit** `feat(shop): shop screen with sort, filter and search`.

### Task 6: Cart / receipt screen and share image

**Files:**
- Create: `ui/cart/CartViewModel.kt`, `ui/cart/CartScreen.kt`, `platform/ShareReceipt.kt`, `res/xml/file_paths.xml`; Modify: manifest (FileProvider)
- Test: `ui/cart/CartViewModelTest.kt`

**Interfaces:**
- Produces: `CartUiState(lines: List<CartLineUi>, totalCents, remainingCents, percentSpent)`; `ShareReceipt.share(context, state, formatter)` renders a bitmap (Canvas) into `cache/receipts/` and fires `ACTION_SEND` with a FileProvider URI.

- [ ] **Steps:** failing VM tests (totals, remaining, clear cart) → implement → run → emulator screenshot of share → commit `feat(cart): receipt screen and share image`.

### Task 7: Onboarding, settings, locale, currency

**Files:**
- Create: `ui/onboarding/OnboardingScreen.kt`, `ui/onboarding/OnboardingViewModel.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`, `values-vi/strings.xml`; Modify: `locales_config.xml`, `MainActivity.kt` (apply per-app locale)
- Test: `ui/onboarding/OnboardingViewModelTest.kt`, `ui/settings/SettingsViewModelTest.kt`

**Interfaces:** `OnboardingViewModel.finish()` writes `Settings(onboarded=true,…)`; `SettingsViewModel.changePerson(id)` clears the cart when `cartTotal > newBalance` only after the UI confirms (`confirmPersonChange`).

- [ ] **Steps:** failing tests (onboarding requires person + currency; changing to a poorer person with an oversized cart requires confirmation and then clears cart; unknown currency falls back to USD) → implement → run → emulator check in both languages → commit `feat: onboarding, settings, locale and currency`.

### Task 8: Content — full catalog, people, avatars

**Files:** Modify `assets/catalog.json` (300–500 items), `assets/people.json` (10–15 people), `assets/fx.json`; add avatar rendering in `ui/components/Avatar.kt`.

- [ ] Draft items by category with sourced estimates; keep `BundledDataValidationTest` green; sanity-check price ordering with a spot test (`max price ≥ largest net worth × 0.001` so the top tier is meaningful); commit `content: full catalog and people list`.

### Task 9: Firebase (optional) and Remote Config overlay

**Files:**
- Create: `platform/RemoteConfigSource.kt`, `data/OverlayPeopleRepository.kt`
- Test: `data/OverlayPeopleRepositoryTest.kt`
- Modify: `app/build.gradle.kts` (conditional plugins/deps), `AppContainer.kt`

**Interfaces:** `interface NetWorthOverrides { suspend fun fetch(): Map<String, Override> }`, `data class Override(val usd: Long, val source: String, val asOf: String)`; `OverlayPeopleRepository(base, overrides)` applies only valid overrides (`usd > 0`, known id, parseable).

- [ ] **Steps:** failing tests (malformed → base; zero/negative → base; unknown id ignored; valid applied) → implement → run → commit `feat(firebase): optional Firebase and Remote Config overlay`.

### Task 10: Polish, accessibility, R8, QA

- [ ] Font scale 200% and long Vietnamese names screenshot checks; TalkBack content descriptions; dark mode; `assembleRelease` with R8 installed and smoke-tested on the emulator; add Compose UI test for onboard → add item → cart; update README; commit `chore: polish, a11y and release build verification`.

### Task 11: Release preparation (docs only, no store actions)

**Files:** Create `docs/privacy-policy.md`, `docs/store-listing.md`, `docs/release-checklist.md`; document GitHub Pages enablement and keystore creation steps (the keystore itself is created by the owner, never committed).

- [ ] Write documents; commit `docs: privacy policy, store listing and release checklist`.

---

## Self-review (spec coverage)

Spec §2 reqs 1–9 → T2,T5,T6,T7,T8,T9; §5 money → T1,T3; §6 data/validation → T2; §6 Remote Config → T9; §7 screens → T5–T7; §8 localization → T7; §9 art/legal → T7,T8; §10 CI/Firebase-optional → T0,T9; §11 M0–M8 → T0–T11; M9 AdMob intentionally deferred (stub `AdsGateway` in T4). Type names are consistent across tasks (`Item`, `Person`, `Cart`, `CartMath`, `Settings`, `SortOrder`, `ItemUi`).
