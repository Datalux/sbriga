# sbriga

Find out whether Android is limiting your app's background work, and why.

Many phone makers (Xiaomi, Huawei, Oppo, Vivo and others) add their own battery managers on top of
Android's Doze. They can kill your app or block its autostart in ways the standard APIs don't report,
and the settings screens that fix it are undocumented and change between OS versions.

Most helpers only open one of those screens. **sbriga also tells you whether you need to**: it reads
what the OS exposes about your app's background standing and gives you a diagnosis you can act on.

## What it does

- Reads past process exits (`ApplicationExitInfo`, Android 11+), the app standby bucket, battery
  optimization state and the "restricted" background setting.
- Separates crashes and ANRs (your bug), low-memory kills, user force-stops and kills that look like
  vendor battery managers.
- Suggests what to ask the user: remove the restriction, exempt the app from battery optimization, or
  allow autostart on their phone.
- Opens the right settings screen: the vendor's one if it exists, then the standard battery
  optimization list, then the app details page.

## Usage

```kotlin
val sbriga = Sbriga(context)
val diagnosis = sbriga.diagnose()

if (diagnosis.has(FindingCode.SUSPECTED_VENDOR_KILLS)) {
    // Show your own explanation, then:
    sbriga.settingsLauncher().open(SettingsTarget.AUTOSTART)
}
```

`diagnose()` is cheap and never throws. Findings are codes, not strings, so you write the user-facing
text and translations yourself.

## Be aware of the limits

- **The diagnosis is probabilistic.** Android never says which vendor component killed a process, so a
  "suspected vendor kill" is an inference from repeated unexplained exits, reported with a confidence
  level. `Diagnosis.limitations` lists what could not be assessed.
- **Below Android 11 there is no exit history.** Only the current state is checked.
- **The vendor settings table is not verified.** The screens in
  [`sbriga-vendors.json`](sbriga-core/src/main/assets/sbriga-vendors.json) come from community lists
  and are all marked `"verified": false`. They are tried one by one and fall back to standard screens
  when they don't open.
- **Not tested on real devices yet.** Treat the current state as an early preview.

## Help us build the vendor table

The vendor table is the part that only the community can get right, and it decays with every firmware
update. If you have a physical device, you can improve sbriga a lot with a few minutes:

1. Find the settings screen that controls autostart or background battery use on your phone.
2. Check which component opens it (for example with `adb shell dumpsys activity activities` while the
   screen is in front, or `adb shell dumpsys window | grep mCurrentFocus`).
3. Open an issue or a pull request with:
   - manufacturer, brand and model (`Build.MANUFACTURER`, `Build.BRAND`, `Build.MODEL`);
   - the OS name and version (MIUI / HyperOS / EMUI / ColorOS / OriginOS / One UI ...) and Android version;
   - the package and activity class that opened the screen;
   - whether it worked when started from an app, or crashed or did nothing.

Reports of entries that **no longer work** are just as valuable. Pull requests that change only
`sbriga-vendors.json` are welcome.

## Build

Requires a JDK 17 (point `JAVA_HOME` at it) and the Android SDK.

```
./gradlew :sbriga-core:testDebugUnitTest
```

## License

Apache License 2.0. See [LICENSE](LICENSE).
