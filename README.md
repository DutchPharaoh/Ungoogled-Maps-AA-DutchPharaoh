# GoogleMap-Morphe: two-wheeler navigation with Google Maps on Android Auto

**English** | [中文](README.zh-TW.md)

Makes **Google Maps navigate on an Android Auto head unit with its native car interface, including two-wheeler (motorcycle) routes**. Intended for an Android Auto screen mounted on a motorcycle: stock Google Maps only allows driving routes on Android Auto and refuses two-wheeler navigation.

Patched with [Morphe](https://morphe.software). The patched Maps installs under a different package, **alongside stock Google Maps**: keep stock Maps for driving, use the patched copy on the motorcycle.

> Experimental. Only verified on a Galaxy S25 (Android 16) with the headunit head-unit emulator (wired USB). Test while parked before riding. Whether a route is legal (e.g. 125cc bikes are banned from Taiwan's national freeways) depends on local road signs, not on the app.

## Features

- Native Google Maps map and turn cards on the head unit
- Two-wheeler mode gives a two-wheeler route and driving mode gives a driving route; the head unit no longer shows "To navigate on Android Auto, switch to ... directions"
- Search destinations on the head unit and navigate to several destinations in a row
- Google sign-in (through MicroG-RE) and location work

## Supported version

**Google Maps `26.44.06.1006895251` only (versionCode `1068856906`, arm64-v8a).** Every change is pinned to this build's obfuscated names; other versions fail at patch time (they do not produce a broken app). **Do not update** the patched app.

This project does not distribute Google Maps. Get the original APKM of the version above yourself (e.g. from APKMirror).

## What you need

| Item | Download |
|---|---|
| Morphe Manager | <https://morphe.software> |
| **This version of MicroG-RE** (required) | [shuili1013/MicroG-RE releases](https://github.com/shuili1013/MicroG-RE/releases/tag/v7.3.0-maps-fix) → `microg-re-maps-fix-7.3.0.apk` |
| Fixed Dutch App Patches bundle | [release `dutch-1.53.9-aa-fix2` in this repo](https://github.com/shuili1013/GoogleMap-Morphe/releases/tag/dutch-1.53.9-aa-fix2) → `patches-1.53.9-aa-fix2.mpp` |
| This project's patches | Add the remote source `github.com/shuili1013/GoogleMap-Morphe` in Morphe |

### Why this version of MicroG-RE is required

Official MicroG-RE has two problems with a patched Google Maps, both fixed in this version of MicroG-RE:

1. **No sign-in, map and search fail**: MicroG-RE only gives account access to Google apps on its allow list, and Google Maps is not on it (log: `GmsAuthProvider: Not granting extended access`).
2. **No location**: Maps connects to the location service with the original `com.google.android.location.internal.GoogleLocationManagerService.START` action, while the official build only accepts the `app.revanced`-prefixed name (log: `Unable to start service ... not found`).

If official MicroG-RE is already installed, **uninstall it first**, then install this version of MicroG-RE (different signing key, Android cannot update over it). Afterwards open MicroG settings and run Self-Check.

## Installation

1. **Install MicroG-RE** (link above) and run its Self-Check.
2. **Morphe settings → Patching → enable Expert mode.**
3. **Add the patch sources:**
   - Sources → **+** → **Local** → pick the downloaded `patches-1.53.9-aa-fix2.mpp`
   - Sources → **+** → **Remote** → enter `github.com/shuili1013/GoogleMap-Morphe`
   - If you already added the official Dutch App Patches, **disable** it so the two sources with the same name do not clash.
4. **Patch Google Maps** (the 26.44 APKM) with this selection:

   | Source | Selection |
   |---|---|
   | Dutch App Patches (local fixed bundle) | Tap **Enable recommended patches**, then also tick **`Add microG-RE support`** |
   | Maps two-wheeler experiment | ✅ `Allow any travel mode on Android Auto`<br>✅ `Always two-wheeler on Android Auto`<br>✅ `Skip private Android Auto car connection`<br>❌ `Two-wheeler instead of driving` (optional: turns driving routes into two-wheeler routes too)<br>❌ `Experimental projected CAR two-wheeler routing` (old; edits the same method as the previous one, never select both) |

   Do **not** select `Clone app` from the official Morphe Patches (Dutch's `Change package name` already renames the app).
5. **Install.** The patched app installs as `org.ungoogled.android.apps.maps.microg`, named **Ungoogled Maps**.
6. **Allow unknown sources in Android Auto:** Android Auto settings on the phone → tap "Version" about 10 times to enable developer mode → top-right menu "Developer settings" → enable **Unknown sources**.
7. Connect to the head unit and pick **Ungoogled Maps** (the head unit shows two Maps icons; stock Maps cannot be hidden, so pick the right one).

## Known limitations

- The "next turn" info in Android Auto's side rail is not shown; the map and turn cards on the main car screen work.
- Real head units, wireless Android Auto, rerouting and long real-road trips are not fully verified.
- Navigation started on the phone and handed to the head unit is less tested than navigation started from a search on the head unit.

## What this project's patches do

| Patch | Description |
|---|---|
| `Skip private Android Auto car connection` | Maps' `CarConnectionHandler.onCarConnected` calls a private Android Auto service, which always rejects a re-signed app (`Wrong signature - go/gearhead-retail-device`) and crashes it. The handler is skipped. |
| `Always two-wheeler on Android Auto` | Android Auto's "supported mode" normally allows two-wheeler only when the head unit reports a motorcycle; it now returns two-wheeler, so destinations searched on the head unit default to two-wheeler routes. |
| `Allow any travel mode on Android Auto` | Removes the two checks that refuse or end car navigation when the travel mode differs. |
| `Two-wheeler instead of driving` (optional) | Rewrites every driving route request as two-wheeler. |

Source: `patch/patches/src/main/kotlin/tw/maps/`. The full debugging history, logcat evidence and 26.44 name mappings are in [findings.md](findings.md) (Chinese).

## Original sources and credits

This project builds on:

- **[Morphe](https://github.com/MorpheApp)**: patcher, Manager and the [patches template](https://github.com/MorpheApp/morphe-patches-template).
- **[Dutch App Patches](https://codeberg.org/JohninNL/nl-morphe-patches)** (JohninNL, GPLv3; [GitLab mirror](https://gitlab.com/johninnld/nl-morphe-patches)): the native Android Auto navigation adapter, `Change package name`, `Bypass Play Services checks`, `Add microG-RE support` and more. This repo's fix patches and notes are in [`third_party/dutch-app-patches/`](third_party/dutch-app-patches/).
- **[bearinmind patches](https://github.com/bearinmindcat/morphe-patches)** (bearinmindcat): the original source of the Maps features in Dutch App Patches.
- **[MicroG-RE](https://github.com/MorpheApp/MicroG-RE)** (MorpheApp) and **[microG](https://github.com/microg/GmsCore)** (microG Project, Apache-2.0): this version of MicroG-RE ([shuili1013/MicroG-RE](https://github.com/shuili1013/MicroG-RE)) is based on them.

## License

- Code in this repo: see [LICENSE](LICENSE) (Apache-2.0).
- The patches in `third_party/dutch-app-patches/` and the fixed Dutch App Patches bundle in the releases: GPLv3, as upstream.
- This version of MicroG-RE: Apache-2.0, as upstream.
- This project does not distribute any Google APK.

## Research notes

The original reverse-engineering work (DEX disassembly scripts, offline tests, handoff notes) is kept in [handoff.md](handoff.md), [findings.md](findings.md), `scripts/`, `tests/` and [patch/README.md](patch/README.md) (mostly Chinese).
