# FlightLink

A tiny Android share-target that converts links between **Flightradar24** and **Skycards** and opens the matching app.

No accounts, no network requests, no analytics — the app converts the flight id between hexadecimal (Flightradar24) and decimal (Skycards).

**[Русская версия](README.ru.md)**

## How it works

Flightradar24 uses the flight id in hexadecimal; Skycards uses the same id in decimal. For example, `0x42693275` is `1114190453`:

```text
https://fr24.com/SDM6325/42693275        (hex id 42693275)
https://skycards.app/flight/-/1114190453 (decimal id 1114190453)
```

FlightLink recognises either form and converts the id:

| Shared link | Converted link |
| --- | --- |
| `https://fr24.com/SDM6325/42693275` | `https://skycards.app/flight/-/1114190453` |
| `https://fr24.com/42693275` | `https://skycards.app/flight/-/1114190453` |
| `https://skycards.app/flight/SU95/1114190453` | `https://fr24.com/42693275` |
| `https://skycards.app/flight/-/1114190453` | `https://fr24.com/42693275` |

The optional aircraft type segment is dropped (`-` is accepted by Skycards).

## Install

Download the latest APK from the [Releases](../../releases) page and install it.
The release APK is debug-signed; Android will ask you to allow installation from
your browser or file manager.

Requires Android 8.0 (API 26) or newer. Install both Flightradar24 and Skycards
for the one-tap flow.

## Usage

1. In Flightradar24 or Skycards, open a flight and tap **Share**.
2. Pick **FlightLink** from the share sheet.
3. FlightLink converts the link and opens the other app.

You can also open FlightLink directly, paste a link, and use **Convert** →
**Open in …**. If the target app is not installed, FlightLink opens its Play
Store page. The converted link can always be copied to the clipboard.

For links shared from Skycards that include `?reg=…`, the manual screen also
offers **Open aircraft page** — an additional Flightradar24 fallback.

## Settings

The gear icon on the main screen configures what happens when a link is shared
into FlightLink. The action is set separately for each direction:

- **When sharing from Flightradar24:** Open Skycards / Copy / Ask.
- **When sharing from Skycards:** Open Flightradar24 / Copy / Ask.

The default is **Open**. **Ask** shows a small dialog with Open / Copy / Cancel.

**Copy both links** (global option) makes the Copy action put both links on the
clipboard:

```text
Skycards: https://skycards.app/flight/-/1114190453
Flightradar24: https://fr24.com/42693275
```

When disabled, only the converted link is copied. The manual screen is not
affected by these settings.

## Build

### Docker (recommended)

```bash
./scripts/build.sh
```

The APK is written to `dist/flightlink-debug.apk`. The script builds a small
toolchain image (JDK 17 + Android SDK 34 + Gradle 8.7) and runs the Gradle build
inside it.

The image is native to the host architecture. On ARM64 the Dockerfile installs
x86_64 runtime libraries and a pinned `qemu-user` 7.2, then runs the Android
SDK's x86_64 `aapt2` through it. No binfmt_misc registration is required. QEMU
is pinned to 7.2 because newer releases break `aapt2`'s argument parsing.

### Local build

Requirements on x86_64: JDK 17, Android SDK with platform 34 and build-tools
34.0.0.

```bash
./gradlew assembleDebug
```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`. On ARM64 use the
Docker build above; a plain `./gradlew` would need the same aapt2 wrapper.

The converter rules are covered by unit tests:

```bash
./gradlew testDebugUnitTest
```

## GitHub Actions

`.github/workflows/build.yml` builds the debug APK on every push and pull
request, uploads it as an artifact, and attaches it to a GitHub Release when a
tag such as `v1.0.0` is pushed.

## Project layout

```text
app/src/main/java/dev/flightlink/
  LinkConverter.kt   link parsing, hex/decimal conversion
  MainActivity.kt    paste/convert screen
  ShareActivity.kt   invisible share target
  AppLauncher.kt     opens the target app, browser, or Play Store
  Settings.kt        per-direction share actions
  SettingsActivity.kt  settings screen

app/src/test/java/dev/flightlink/
  LinkConverterTest.kt  unit tests for the conversion rules
```

## Notes

- The utility is unofficial and is not affiliated with Flightradar24 AB or
  Skycards.
- FlightLink does not validate the flight id against a server. If an app does
  not recognise an id, it shows its own error page.
- The app declares no permissions and makes no network requests.

## License

MIT
