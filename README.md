# Spend Like a Millionaire

An offline Android app (Kotlin + Jetpack Compose) that lets you spend a celebrity-sized fortune
on everything from a coffee to a football stadium, to feel how big "billionaire money" really is.

> Working title. Status: **requirements / design phase** — no app code yet.

## Planned features

- Pick whose fortune to spend (a short list of public billionaires), with the source of the net worth shown.
- Huge catalog of items, from a few dollars to multi-billion-dollar assets; sort and filter by price.
- Cart with quantities, running balance and "% of fortune spent".
- Choose display currency, language and person during a short onboarding.
- Shareable receipt image.
- Fully offline; small values (e.g. net worths) updatable via Firebase Remote Config.

## Disclaimer

This is a parody / entertainment app. It is not affiliated with, endorsed by, or sponsored by any
person, company, team or band mentioned. Net worths and item prices are rough public estimates.
All artwork is original or openly licensed; no real people's likenesses or third-party logos are used.

## Tech

Kotlin, Jetpack Compose (Material 3), Gradle. Build: `./gradlew assembleDebug`.
