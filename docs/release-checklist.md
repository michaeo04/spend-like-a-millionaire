# Release checklist

Everything here is done by the owner; none of it is automated and no secret may enter the repo.

## 0. One-time accounts

- [ ] Google Play Console developer account (one-time registration fee) and identity verification.
- [ ] Firebase project with an Android app whose package name is `com.michaeo04.spendlikeamillionaire`.
- [ ] If the account is a new personal account: closed testing with at least 12 testers for 14 days
      before production access (verify the current requirement in the Play Console).

## 1. Firebase

- [ ] Download `google-services.json` into `app/` (it is git-ignored; never commit it).
- [ ] Remote Config: add parameter `net_worth_overrides` (JSON string, default `{}`), publish.
- [ ] Rebuild and confirm Crashlytics and Remote Config work on a device.

## 2. Signing

Create an upload keystore **outside the repo** and back it up (losing it blocks future updates):

```
keytool -genkeypair -v -keystore D:\secrets\upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Create `keystore.properties` in the repo root (git-ignored):

```
storeFile=D:/secrets/upload-keystore.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

Then wire a `signingConfigs.release` block to read it (it is not committed because it needs your
secrets) and run `./gradlew bundleRelease`. Enable Play App Signing in the Play Console.

## 3. Privacy policy hosting

- [ ] GitHub repo > Settings > Pages > Deploy from branch `main`, folder `/docs`.
- [ ] Confirm https://michaeo04.github.io/spend-like-a-millionaire/privacy-policy loads.
      (The Settings screen links to this URL.)

## 3b. Brand names and photos (review before publishing)

- [ ] The catalog uses real product/brand names and photos of branded products. Names are plain
      text and photos are open-licensed with in-app credits, but Play can still flag trademark use.
- [ ] Do not put brand names in the app title, short description or tags; keep the parody disclaimer.
- [ ] Store screenshots: prefer screens with generic items (onboarding, cart, a category grid) to avoid
      showcasing third-party logos. If a reviewer objects, switch an item with `{"skip": true}`
      in `tools/image-overrides.json` or rename it in `tools/catalog-source.tsv`.

## 3c. Photos of real people (right of publicity)

- [ ] The app shows Commons portraits of 30 real people (billionaires and celebrities). The licenses
      cover copyright, NOT personality/publicity rights, and some files carry a "personality rights"
      note on Commons (`"personality": true` in `tools/images-manifest.json`). Using a person's photo in a
      monetized app can still need permission in some countries.
- [ ] Safer fallback for release: set `{"skip": true}` for any person in `tools/image-overrides.json`
      (they keep the colored initial avatar), or remove all portraits and rebuild with
      `python tools/build_people.py`. Do not use people's names or photos in store graphics.

## 4. Store listing

- [ ] Fill in `docs/store-listing.md` content, upload icon, feature graphic and screenshots.
- [ ] Complete the content rating questionnaire and the Data safety form.
- [ ] Choose countries, pricing (free), and confirm the target API level is 36 or higher.

## 5. Test and roll out

- [ ] Run unit tests, lint and the end-to-end test; smoke-test the release build on a real device.
- [ ] Upload the AAB to an internal/closed testing track, recruit testers.
- [ ] Production: staged rollout (for example 20% then 100%), watch Crashlytics.

## 6. After launch: AdMob (milestone M9)

- [ ] Create an AdMob account/app, use test ad unit IDs while developing.
- [ ] Implement `AdsGateway` and the UMP consent flow; update privacy policy, Data safety and
      content rating before publishing the update.
