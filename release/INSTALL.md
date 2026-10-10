# Install UIcore 1.0.0

> **Language:** English | [Русская версия](INSTALL_RU.md)

UIcore creates menus from world-space elements: text, items, buttons, element states, and player-specific schematic previews. Menus are configured in YAML and controlled by plugin commands.

## Install the plugin

1. Stop the server and back up the world and `plugins/UIcore/` if this is an update.
2. Put `UIcore-1.0.0.jar` and PacketEvents **2.14.0** in `plugins/`.
3. Start the server. UIcore creates its configuration and example menus in `plugins/UIcore/menu/`.
4. Stand in an open area and run `/uicore run demo_main`. Close the menu with `/uicore stop`.

UIcore supports Minecraft 1.21.1 and newer. It uses Java 17 bytecode; run the server with the Java runtime required by its build. See the [compatibility matrix](../docs/COMPATIBILITY.md) for tested server combinations. PacketEvents is required; PlaceholderAPI, FancyNpcs, SkinsRestorer, CraftEngine, Vault, and LuckPerms are optional.

## Create your menu

Create a `.yml` file in `plugins/UIcore/menu/`. The `demo_*` files show the syntax and do not modify existing menus. Begin with `camera-position.type: player`, then run `/uicore reload` after an edit and check the log for YAML errors. See the [menu authoring guide](../docs/MENU_AUTHORING_GUIDE.md) for the complete syntax.

Console actions are disabled by default. If you need them, allow only commands you trust through `security.allow-console-commands` and `security.console-command-allowlist` in `config.yml`.

For schematic previews, put Sponge `.schem` v2 or v3 files in `plugins/UIcore/schematics/`, list the file under the menu's `schematics:` section, and run `/uicore reload`. The preview is visible only to the player and does not modify world blocks. Container contents and other block entities, entities, and biomes are not shown.

On Folia, keep all elements in one scene inside the camera region. If a large menu crosses a region boundary, choose a different camera position or reduce the distance to its elements.

## Update an existing server

Stop the server before replacing the JAR. Keep your YAML files and schematics, then replace only UIcore in `plugins/`. After startup, run the main menus, clicks, scene changes, and `/uicore stop`, then check the log. Do not overwrite your configuration with archive examples: UIcore adds an example only when a file with that name does not exist.
