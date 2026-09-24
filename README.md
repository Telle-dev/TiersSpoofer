# TierSpoofer

Fabric mod for 1.21.11. Shows tier tags (MCTiers / PvPTiers / SubTiers) next to names and lets you set your own tier, name, name color and skin for any player. Everything is client-side, so only you see it.

## Building

On Windows just run `Build.bat`. It grabs Java 21 if you don't have it, builds the mod and drops the jar in `output/`.

Otherwise, with Java 21 installed:

```
./gradlew build
```

The jar ends up in `build/libs/`. Pushing to GitHub also builds it (check the Actions tab).

## Usage

Press `=` in game (or open it from Mod Menu).

- **Player Name / tier / mode / list**: who to tag and with what. Hit Add.
- **Fake Name**: shows instead of their real name in tab, nametag, chat and death messages. `&` codes and `&#RRGGBB` work. Also used for the skin.
- **Name Color**: `#FF5555`, a gradient like `#FF0000-#0000FF`, or `rainbow`.
- **Real**: pull real tiers from a tier list for everyone you haven't tagged.
- **Own Tag**: show your own nametag in F5.

Needs Fabric API. Mod Menu is optional.

## Credits

MCTiers/SubTiers icons from [TierTagger](https://github.com/mctiers-dev/TierTagger) (MPL-2.0), PvPTiers icons and colors from [PvPTiers/Tiers](https://github.com/PvPTiers/Tiers) (GPL-3.0). See `THIRD_PARTY_NOTICES.md`.
