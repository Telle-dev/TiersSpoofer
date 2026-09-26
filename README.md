# TierSpoofer

Fabric mod for Minecraft 1.21.11. Shows MCTiers, PvPTiers and SubTiers tags next to player names, and lets you give any player your own tier, name, name color and skin.

Everything happens on your side only. Nobody else sees the changes and nothing is sent to the server.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for 1.21.11
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods` folder
3. Download the latest `tierspoofer-*.jar` from [Releases](https://github.com/Telle-dev/TiersSpoofer/releases) and put it in `mods` too
4. [Mod Menu](https://modrinth.com/mod/modmenu) is optional, but lets you open the settings from the mods list

## How to use

Press **=** in game to open the menu.

- **Player Name, tier, mode, list**: pick a player and the tier you want them to have, then hit **Add**
- **Fake Name**: shown instead of their real name in tab, nametags, chat and death messages. `&` color codes and `&#RRGGBB` work. Their skin changes to that name's skin too
- **Name Color**: a hex color like `#FF5555`, a gradient like `#FF0000-#0000FF`, or `rainbow`
- **Real**: shows real tiers from a tier list for everyone you didn't add yourself
- **Own Tag**: shows your own nametag in F5
- **Cmds** (off by default): fake names show up when you tab-complete commands, and get turned back into the real name when you send it. So `/tpa k1rbe` still reaches the server as `/tpa Steve`

Works together with TierTagger and the PvPTiers Tiers mod. If TierSpoofer shows a tier for someone, the other mod's tag for that player is hidden, so you don't see two.

## Building it yourself

On Windows, double-click `Build.bat`. It downloads Java 21 if you don't have it and puts the jar in `output/`.

Anywhere else, with Java 21:

```
./gradlew build
```

The jar ends up in `build/libs/`.

## License

GPL-3.0, see [LICENSE](LICENSE). The tier icons come from [TierTagger](https://github.com/mctiers-dev/TierTagger) (MPL-2.0) and [PvPTiers/Tiers](https://github.com/PvPTiers/Tiers) (GPL-3.0), details in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
