# TierSpoofer 1.21.11 — Build Instructions

> **Easiest (Windows):** double-click **`Build.bat`**. It finds Java 21 (or downloads a
> portable copy into `.jdk\`, no admin needed), builds the mod, puts the jar in `output\`
> and offers to copy it into `%APPDATA%\.minecraft\mods`. The first run takes about 10 minutes.
>
> **Or:** push this repo to GitHub. The workflow in
> `.github/workflows/build-tierspoofer.yml` builds the jar. Open the run under the
> **Actions** tab and download the `tierspoofer-jar` artifact.

## Requirements
- **Java 21** (must be exactly 21, not higher for best compatibility)
- **Git** (optional but recommended)
- Internet connection (Gradle downloads dependencies on first build)

## Step 1: Java 21

The Gradle wrapper (`gradlew`, `gradle/wrapper/gradle-wrapper.jar`, Gradle 9.1.0)
is included now, so all you need is **Java 21** (for example Temurin 21).

## Step 2 — Build

**Windows:**
```
gradlew.bat build
```

**Linux / macOS:**
```
chmod +x gradlew
./gradlew build
```

First build takes 5–10 minutes (downloads MC, mappings, dependencies).
Subsequent builds are fast.

## Step 3 — Find the output JAR

```
build/libs/tierspoofer-1.0.0+1.21.11.jar
```

Drop this into your `.minecraft/mods/` folder alongside:
- Fabric Loader 0.18.1+
- Fabric API 0.141.1+1.21.11
- ModMenu 17.0.0 (optional)

## Fake names & name colors

Each entry in the config screen (`=` key) can change a player's name everywhere
it shows up: **tab list, nametag, chat and death messages**.

- **Fake Name**: the name to show instead. Supports `&` codes (`&c`, `&l`, ...) and `&#RRGGBB`.
  Leave it empty to keep the real name (for example, to only recolor it).
- **Name Color**: `#RRGGBB` (or `#RGB`), a gradient like `#FF0000-#0000FF`
  (2+ stops), or `rainbow`. Click a swatch to fill it in. The field turns red if
  the value isn't valid. Colors from `&` codes in the fake name take priority.
- The **Preview** line shows exactly how the tag will look.

Only the username itself is replaced, so server rank prefixes and team colors
in the tab list stay the same (`[VIP] RealName` becomes `[VIP] FakeName`). If you add
a player who isn't online, the entry is matched by name when they join and
switches to their real UUID automatically.

## Tier lists & icons

Icons for all three tier lists ship inside the mod now (no TierTagger or
resource pack needed). `assets/minecraft/font/default.json` maps them to:

| Code points | Tier list |
|---|---|
| `\uE701`–`\uE708` | MCTiers (same as TierTagger) |
| `\uE801`–`\uE812` | SubTiers (same as TierTagger) |
| `\uEA01`–`\uEA08` | PvPTiers |

In the config screen (`=` key):

- **MCTiers / PvPTiers / SubTiers** button (next to "All"): which tier list a
  spoofed entry uses. It changes the gamemode dropdown, the icons and the tier
  colors. Older configs without this setting load as MCTiers.
- **Real: OFF / MCTiers / PvPTiers / SubTiers**: TierTagger-style lookups.
  Every player you haven't spoofed gets their real tier from that list's API,
  shown in the nametag and tab list. A spoofed entry always overrides the real tier.
- **Best / <gamemode>**: show each player's highest real tier, or their tier
  in one gamemode.

APIs used: `mctiers.com/api/v2/profile/<uuid>`, `pvptiers.com/api/profile/<uuid>`,
`subtiers.net/api/v2/profile/<uuid>`. Results are cached for 10 minutes;
errors and rate limits are retried after 1 minute.

See `THIRD_PARTY_NOTICES.md` for where the icons come from and their licenses.

## Troubleshooting

**"Could not resolve net.fabricmc:yarn"**
→ Check your internet connection. The FabricMC Maven server may be temporarily down.

**"Unsupported class file major version"**
→ You're using the wrong Java version. Must be Java 21.

**"Invalid mixin config tierspoofer.mixins.json"**
→ The mixins.json is in the wrong folder. It must be at `src/client/resources/tierspoofer.mixins.json`.

**Compile error in MixinPlayerListEntrySkin.java**
→ This file uses 1.21.11's skin API (`SkinTextures.create`, `SkinTextures.SkinOverride.create`,
  `AssetInfo.TextureAssetInfo(Identifier id, Identifier texturePath)`). It was checked
  against the Yarn 1.21.11 mappings but never compiled against the real jar. If a
  signature is off, your IDE will show the correct one.
