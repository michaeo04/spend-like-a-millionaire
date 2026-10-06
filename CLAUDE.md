# CLAUDE.md

Offline Android app "Spend Like a Millionaire" (Kotlin + Jetpack Compose, single module `app`).
**Current state and the remaining launch checklist live in `docs/PROJECT_STATUS.md` - read it first.**

## Where things are
- Code: `app/src/main/java/com/michaeo04/spendlikeamillionaire/{domain,data,ui,platform}`; manual DI in `AppContainer.kt`.
- Data (bundled, read-only): `app/src/main/assets/{catalog,people,fx,image_credits}.json` and `images/`. Do not hand-edit
  `catalog.json`, `people.json`, `image_credits.json`; they are generated:
  `tools/catalog-source.tsv` -> `python tools/build_catalog.py`, `tools/people-source.tsv` -> `python tools/build_people.py`,
  photos via `python tools/fetch_images.py` (overrides in `tools/image-overrides.json`).
- User data on device: DataStore (settings + cart). Remote Config only overlays net worths (`net_worth_overrides`).
- Firebase is optional: `app/google-services.json` is git-ignored; without it `src/nofirebase` is compiled.

## Commands
```
./gradlew testDebugUnitTest assembleDebug lintDebug
./gradlew connectedDebugAndroidTest      # needs an emulator; run `adb uninstall com.michaeo04.spendlikeamillionaire` first
./gradlew bundleRelease                  # needs signing config (not wired yet, see PROJECT_STATUS.md)
```

## Conventions
- Money is `Long` USD cents everywhere; convert/format only at the UI edge (`domain/Formatting.kt`). Quantities are clamped to what the balance affords.
- TDD: write the failing test first. ViewModels are tested with fakes in `app/src/test/.../testing/Fakes.kt`.
- UI strings in both `values/strings.xml` and `values-vi/strings.xml`.
- Never commit secrets: keystores, `keystore.properties`, `google-services.json`, `local.properties`, `.env`.
- Work on a branch, open a PR, wait for CI (`.github/workflows/ci.yml`), then merge.

## Windows gotchas
- Write files as UTF-8 **without BOM** (PowerShell 5.1 `Set-Content -Encoding utf8` adds one and breaks Python JSON and commit messages).
- Real DataStore cannot overwrite files on Windows JVM tests; stores are tested with an in-memory fake.
- The emulator can die when Gradle and heavy downloads run together; restart with `-memory 2048`.
- Keep `gradlew` executable in git (`git update-index --chmod=+x gradlew`) and LF line endings (`.gitattributes`).

## Open decisions / do not do
- AdMob is intentionally not built yet (milestone after launch); only the empty `AdsGateway` exists.
- Remote Config is for display conditions (person groups, ads), NOT for the catalog.
- Real brand names and real portraits are shipped on purpose; see `docs/release-checklist.md` 3b/3c before changing the store listing.
