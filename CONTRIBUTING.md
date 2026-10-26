# Contributing

The most useful contribution is a **device report**: the vendor settings table can only be verified on
real phones.

## Report a device

1. Build and install the demo app (`./gradlew :sample:installDebug`, with a JDK 17 in `JAVA_HOME`).
2. Tap each screen under *Try each vendor screen* and note which ones open.
3. Tap **Copy report** and open a [vendor screen report](../../issues/new?template=vendor-report.yml).

## Change the vendor table

Entries live in [`sbriga-core/src/main/assets/sbriga-vendors.json`](sbriga-core/src/main/assets/sbriga-vendors.json).

- Order matters: candidates are tried from top to bottom, so put the most reliable first.
- Set `"verified": true` only for a component you confirmed on a real device, and say which device and
  OS version in the pull request.
- Remove or demote entries that no longer open.
- Keep changes to this file in their own pull request.

## Code changes

- Run `./gradlew :sbriga-core:testDebugUnitTest` before opening a pull request.
- The diagnosis logic in `Diagnoser` is pure on purpose: add a unit test for every rule you change.
- The public API uses Kotlin explicit API mode: anything public must be intentional.

By contributing you agree that your work is released under the Apache License 2.0.
