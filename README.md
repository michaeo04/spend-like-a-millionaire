# Spend Like a Millionaire

An offline Android app (Kotlin + Jetpack Compose) that lets you spend a billionaire's fortune on
everything from a candy bar to a space station, to feel how big "billionaire money" really is.

> Status: v0.1 feature-complete (AdMob is intentionally built last, just before launch).

## Features

- Pick whose fortune to spend (15 public billionaires), with the source and month of the net worth.
- 300+ items from about $1.80 (a Snickers bar) to $500B (a brand-new city), with real product names
  (Big Mac, PlayStation 5, Tesla Model 3, Rolex Submariner ...) and open-licensed photos; sortable by
  price, filterable by category, searchable (accent-insensitive, so "ca phe" finds "Cà phê").
- Photo grid with a +/- stepper; tap the number to type a quantity or use **MAX**; cart with running
  total and **% of the fortune spent**.
- Shareable receipt image.
- Animated onboarding (welcome, language, currency, person with a "= N Big Macs" fun fact);
  English and Vietnamese; 17 display currencies.
- Fully offline. Net worths can be refreshed through Firebase Remote Config without an app update.

## Disclaimer

This is a parody / entertainment app. It is not affiliated with, endorsed by, or sponsored by any
person, company, team or band mentioned. Net worths and item prices are rough public estimates.
Avatars and icons are original or emoji; no real people's likenesses or third-party logos are used.

## Build and test

Requirements: JDK 17 and the Android SDK (platform 37, build-tools 36). Set `ANDROID_HOME` or
create `local.properties` with `sdk.dir=...`.

```
./gradlew assembleDebug            # debug APK
./gradlew testDebugUnitTest        # unit tests (JVM)
./gradlew lintDebug                # lint
./gradlew bundleRelease            # release AAB (R8); needs signing config, see docs/release-checklist.md
```

End-to-end test (needs an emulator or device; **uninstall the app first** so it starts fresh):

```
adb uninstall com.michaeo04.spendlikeamillionaire
./gradlew connectedDebugAndroidTest
```

## Photos and credits

Item photos are downloaded from **Wikimedia Commons** and only if they are CC0, public domain, CC BY or
CC BY-SA. Author, license and source page are stored in `tools/images-manifest.json` and shown in the
app under Settings > Image credits (required by CC BY).

```
python -m pip install pillow
python tools/fetch_images.py                # fetch photos for items that have none (resumable)
python tools/fetch_images.py --only a,b     # refetch specific items
python tools/contact_sheet.py               # labelled thumbnail sheets to review matches by eye
python tools/build_catalog.py               # regenerate catalog.json + image_credits.json
```

Wrong photo? Add `{"item_id": {"query": "better words"}}` (or `{"file": "File:Exact name.jpg"}`, or
`{"skip": true}` to keep the emoji) to `tools/image-overrides.json` and refetch that item.

## Editing the catalog

The source of truth is `tools/catalog-source.tsv`. After editing, regenerate the JSON the app reads:

```
python tools/build_catalog.py
```

The build validates ids, categories, prices and the "estimate" flag, and a unit test
(`BundledDataValidationTest`) re-checks the generated assets on every test run.
People and exchange rates live directly in `app/src/main/assets/people.json` and `fx.json`.

## Firebase (optional)

`app/google-services.json` is git-ignored. Without it the app builds and runs with a no-op bridge
(no Crashlytics/Analytics/Remote Config). With it, Crashlytics, Analytics and Remote Config are
enabled automatically. Remote Config key `net_worth_overrides`:

```json
{ "p_musk": { "usd": 981000000000, "source": "Forbes", "asOf": "2026-10" } }
```

Invalid entries (non-positive or absurd values, unknown ids, malformed dates) are ignored and the
bundled value is used. Fetched values apply from the next launch.

## Docs

- Design: `docs/superpowers/specs/2026-10-05-spend-like-a-millionaire-design.md`
- Implementation plan: `docs/superpowers/plans/2026-10-05-spend-like-a-millionaire.md`
- Release: `docs/release-checklist.md`, `docs/store-listing.md`, `docs/privacy-policy.md`

## License

MIT, see `LICENSE`.
