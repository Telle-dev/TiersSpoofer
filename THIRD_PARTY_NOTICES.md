# Third-party assets

TierSpoofer's own code is MIT (see `LICENSE`). The gamemode icons bundled under
`src/client/resources/assets/tierspoofer/textures/font/` come from other
projects and keep their original licenses:

| Folder | Source | License |
|---|---|---|
| `mctiers/`, `subtiers/` | [mctiers-dev/TierTagger](https://github.com/mctiers-dev/TierTagger) @ `6f49034` (`common/src/main/resources/assets/tiertagger/textures/`) | MPL-2.0 — `licenses/TierTagger-LICENSE.txt` |
| `pvptiers/` | [PvPTiers/Tiers](https://github.com/PvPTiers/Tiers) @ `c3520c4` (`resourcepacks/tiers-resources/.../font/gamemodes/pvptiers/`) | GPL-3.0 — `licenses/Tiers-GPL-3.0.txt` |

The PvPTiers tier colors in `TierList.java` are taken from that project's
`colors/pvptiers.json`.

**Heads-up:** the PvPTiers icons are GPL-3.0. If you publish a jar that
includes them, the GPL expects the whole mod to be distributed under GPL-3.0
(with source). Either relicense TierSpoofer to GPL-3.0 before publishing, or
delete the `pvptiers/` folder and its entries in `assets/minecraft/font/default.json`.
