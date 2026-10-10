# UIcore — Build menus inside the Minecraft world

UIcore is a world-space UI framework for Minecraft servers. It places a menu around the player's camera, so labels, buttons, item previews, and input fields appear as part of the game world instead of inside a chest or inventory screen. Players navigate with a visual cursor, click elements, move between screens, and enter text through chat.

Create menus in YAML, then extend them with the public Java API. Use UIcore for server hubs, profiles, settings, shops, selection screens, and other custom interfaces. The bundled examples work with vanilla items and need no resource pack.

## What you can build

- **Interactive screens:** text labels and buttons with hover feedback, cursor styles, tooltips, click animations, and cooldowns.
- **Inputs and state:** chat input fields, toggles, permission checks, click requirements, and per-player element states that can change text, material, or visibility.
- **Item galleries:** item displays, player avatars, reusable templates, and slots with selectable contents.
- **World previews:** show up to four player-specific Sponge `.schem` v2 or v3 previews in a menu. They are sent to that player only and do not change the real world.
- **Player privacy:** choose whether nearby players can see a menu's displays, hide the player who opened it, hide other players from that player, and allow or block damage from other players while the menu is open.
- **Extensions:** use the Java API to register actions, data providers, conditions, and custom components.
- **Integrations:** Vault and LuckPerms support is built in and optional. PlaceholderAPI, FancyNpcs, SkinsRestorer, and CraftEngine are also optional integrations.

## Requirements and compatibility

- Minecraft **1.21.1 or newer** on Paper or a compatible server. UIcore declares support for Folia; on Folia, keep each menu scene within the camera's region.
- **PacketEvents 2.14.0** is required.
- Run the server with the Java version required by that Minecraft and server build. UIcore's plugin and API use Java 17 bytecode.
- Vault, LuckPerms, PlaceholderAPI, FancyNpcs, SkinsRestorer, and CraftEngine are optional. UIcore starts without them; their integration features need the corresponding plugin.

## Get started

Install UIcore and PacketEvents in the server's `plugins/` directory and start the server. UIcore creates its configuration and three example menus. Run `/uicore run demo_main` to open the main example; `/uicore stop` closes the menu. Edit or add YAML files in `plugins/UIcore/menu/`, then run `/uicore reload` to load your changes.

Read the [installation guide](https://github.com/kekaop/UIcore/blob/orven/normal-source-build/release/INSTALL.md) to set up the server, then follow the [menu authoring guide](https://github.com/kekaop/UIcore/blob/orven/normal-source-build/docs/MENU_AUTHORING_GUIDE.md) to build your first screen. The [compatibility matrix](https://github.com/kekaop/UIcore/blob/orven/normal-source-build/docs/COMPATIBILITY.md) lists tested server builds. Watch the [bundled menu walkthrough](https://github.com/user-attachments/assets/60c09f38-9699-4fa7-9ec8-5ccf51af7e2b) or see a [custom server menu](https://github.com/user-attachments/assets/50bd8035-eb6a-4ecd-9941-b3f17b2cd196).

## Current limits

Schematic previews show block states, but not container or other block-entity contents, entities, or biomes. Console commands in menus are disabled by default and require an explicit allowlist. NoChatReports may interfere with signed chat and menu text input on some client setups; see the compatibility guide for the observed behavior and workaround.

**Source and documentation:** [github.com/kekaop/UIcore](https://github.com/kekaop/UIcore)  
**License:** [AGPL-3.0](https://github.com/kekaop/UIcore/blob/orven/normal-source-build/LICENSE)

Created by **Orven**. UIcore is based on [CustomScreenMenu](https://builtbybit.com/resources/customscreenmenu.108929/) by **wuhaoxian123** and is published with permission from the original author.
