# plugin.webviewext

Solar2D plugin that fixes native WebView quirks Solar2D's Lua API can't reach. Currently: hides the on-screen +/- zoom buttons Android WebViews show in the bottom-right corner on pinch or scroll-past-edge. iOS / macOS / tvOS variants are no-ops (their webviews never show those buttons).

## Why

Solar2D's Android core (`CoronaWebView.java`) calls `settings.setBuiltInZoomControls(true)` on every `native.newWebView()` but never calls `settings.setDisplayZoomControls(false)`. Android defaults display zoom controls to true, so legacy +/- zoom buttons pop up whenever the user pinches or hits a page edge while scrolling. Solar2D exposes no Lua API to reach `WebSettings`, so the only fix is native.

`hideZoomControls()` walks the activity view hierarchy on the UI thread and calls `setDisplayZoomControls(false)` on every WebView found. Pinch-to-zoom keeps working — only the buttons disappear.

## Install

Add to `build.settings`:

```lua
settings = {
    plugins = {
        ["plugin.webviewext"] = { publisherId = "com.idleveloping" },
    },
}
```

## Usage

Call right after creating a webview:

```lua
local webView = native.newWebView(x, y, width, height)
if system.getInfo("platform") == "android" then
    require("plugin.webviewext").hideZoomControls()
end
```

The call is safe to repeat and affects every WebView alive at the time. Ordering is guaranteed: Solar2D posts the WebView creation to the Android main looper, and `hideZoomControls()` posts its hierarchy walk after it.

## Building from source

Requires:
- JDK 17 (`temurin-17` recommended)
- Solar2D Native installed at `~/Library/Application Support/Corona/Native/` (or symlinked there)

```bash
cd android
JAVA_HOME=/path/to/jdk-17 ./gradlew :plugin:assembleRelease
```

Output AAR: `android/plugin/build/outputs/aar/plugin-release.aar`.

### Local Solar2DPlugins deployment

Drops the built variants into `~/Solar2DPlugins/com.idleveloping/plugin.webviewext/` so the Solar2D simulator picks them up ahead of the online plugin directory:

```bash
./gradlew :plugin:deployAllToLocalSolar2DRepo
```

This deploys:
- `android/data.tgz` — Java AAR
- `iphone/`, `iphone-sim/`, `mac-sim/`, `tvos/`, `tvos-sim/` — Lua no-op variants

Individual platforms can be deployed via `deployToLocalSolar2DRepo` (Android only) or `deployTo_<platform>` for any single variant.

## Repository layout

```
android/
  plugin/                                -- Android Studio module producing the AAR
    src/main/java/plugin/webviewext/
      LuaLoader.java                     -- Lua entry point + hideZoomControls() implementation
    build.gradle                         -- AAR build + deployment tasks
plugins/2026.3729/                       -- Distribution layout consumed by Solar2D Plugin Directory
  android/                               --   AAR + metadata.lua + corona.gradle
  iphone/, iphone-sim/, mac-sim/,        --   plugin/webviewext.lua no-op + metadata.lua
  tvos/, tvos-sim/
.github/workflows/publish.yml            -- Wraps solar2d/directory-plugin-action; publishes release per push
```

## Publishing to the Solar2D Plugin Directory

This repository ships with the layout and GitHub Action that the
[Solar2D Free Plugin Directory](https://plugins.solar2d.com/) expects.

### One-time setup

1. **Repository name must match the directory convention.** The directory's
   [`json_from_repo.py`](https://github.com/solar2d/plugins.solar2d.com/blob/master/json_from_repo.py)
   parses the repo name as `<publisherId>-<plugin.name>`, splitting on the
   first hyphen. This repo is named **`com.idleveloping-plugin.webviewext`**
   to match.
2. **Open a submission issue** at
   [solar2d/plugins.solar2d.com/issues/new](https://github.com/solar2d/plugins.solar2d.com/issues/new)
   describing the plugin and requesting `publisherId = com.idleveloping`,
   `plugin.name = plugin.webviewext`. Wait for maintainer approval.
3. **(Optional) Add the `ISSUE_PAT` secret** to this repository's GitHub
   settings. Personal access token with `repo` scope. Without it the workflow
   still creates a release, but the directory entry won't auto-refresh — you'd
   PR the JSON update manually each time.

### Release flow

Every push to `main` that touches `plugins/**` triggers
`.github/workflows/publish.yml`, which:

1. Tars each `plugins/<build>/<platform>/` directory into
   `<build>-<platform>.tgz`.
2. Creates a GitHub release named `v<run_number>` with those tarballs as
   assets.
3. Dispatches a refresh request to `solar2d/plugins.solar2d.com` (only if
   `ISSUE_PAT` is set) so the directory JSON regenerates against the latest
   release.

### Cutting a new release

After changing Android code:

```bash
cd android
./gradlew :plugin:refreshDirectoryPlugin     # builds AAR + copies into plugins/2026.3729/android/
git add plugins/                              # commit the rebuilt AAR + any lua/metadata changes
git commit -m "Release notes"
git push origin main                          # GitHub Action takes over
```

After changing Lua-only platform code (`plugins/2026.3729/*/plugin/webviewext.lua`):
just commit and push — the workflow tars and releases.

### Supporting additional Solar2D builds

Duplicate `plugins/2026.3729/` to another build folder, rebuild the AAR
against that Solar2D Native version, and update `solar2dBuild` in
`android/plugin/build.gradle` if you want the local-deploy tasks to target
the new build.

## License

MIT — see [LICENSE](LICENSE).
