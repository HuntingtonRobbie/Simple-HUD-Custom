# Simple HUD Enhanced (custom fork)

This is a personal fork of **[Simple HUD Enhanced](https://github.com/soradgaming/Simple-HUD-Enhanced)** by **SoRadGaming**, with contributions from yutotakano, alerouzic, johnvictorfs, A5b84, mooziii and the translators listed in `fabric.mod.json`. Simple HUD Enhanced started as a fork of [Simple Utilities Mod](https://github.com/johnvictorfs/simple-utilities-mod) by johnvictorfs. All credit for the original mod goes to them.

The fork keeps the original **MIT License** (see `LICENSE`). It was made for personal use and is not an official release.

## Branches
- `26.3`: Minecraft 26.3, built on upstream's `26.2` branch.
- `1.21.11`: Minecraft 1.21.11, built on upstream's `main`.

## What this fork changes
- **Minimal effects display:** shows just the effect icon and a timer instead of the vanilla boxes. It has a layout, a timer side, scale and a low-time warning, plus a Roman numeral for effects at level II and up.
- **Edit HUD screen:** click, drag or nudge (arrow keys) any Simple HUD element into place.
- **Apply button** in the settings screen.
- **Hide Scoreboard Sidebar** option.
- `26.3` only: ported to Minecraft 26.3. Show HUD changes only in the settings screen: the toggle key was removed and F3 no longer rewrites the setting.

---

*The original README follows.*
## Mod Pages
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/simple-hud-enhanced)

[Modrinth](https://modrinth.com/mod/simple-hud-enhanced)
## Description
A Minecraft mod that enhances the game's Heads-Up Display (HUD) by introducing 
customizable elements and features to display information.
The mod is designed to be lightweight and easy to use, allows users to adjust the position, visibility,
and other settings of these elements, providing a personalized HUD experience 
tailored to individual preferences though the [Mod Menu mod](https://www.curseforge.com/minecraft/mc-mods/modmenu).

## Installation
1. Download the latest version of the mod from the [releases page]()
2. Download and install the [Fabric Loader](https://fabricmc.net/use/).
3. Download and install the [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api).
4. Download and install the [Mod Menu mod](https://www.curseforge.com/minecraft/mc-mods/modmenu).
5. Place the downloaded mod in the `mods` folder in your Minecraft directory.
6. Launch the game using the Fabric profile.
7. Enjoy!

## Code Contributions
This mod has been built from the ground up to be modular and faster to update, 
originally as a fork of Simple Utilities Mod by [johnvictorfs](https://github.com/johnvictorfs/simple-utilities-mod).

This mod has also been built with the help of the following mods:
- For Status Effect Rings:
[A5b84](https://github.com/A5b84/status-effect-bars)
- For TPS Tracking Client Side:
[mooziii](https://github.com/mooziii/tpshud-fabric)
