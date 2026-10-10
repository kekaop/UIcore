# UIcore

> **Language:** English | [Русская версия](README_RU.md)

### A world-space UI framework for Minecraft servers

UIcore turns the game world into a stage for menus. Text, items, buttons, input fields, stateful elements, animated slots, NPC mirrors, and per-player previews live in the same scene as the player instead of inside a flat inventory window.

This is the first public release of UIcore: a focused, extensible continuation of CustomScreenMenu with a proper Java API, YAML authoring, Paper/Folia support, and built-in integrations for Vault and LuckPerms.

The project exists thanks to **wuhaoxian123**, the original CustomScreenMenu author. UIcore is based on the paid 1.4.8 build, with permission to publish the complete source under AGPL-3.0. See the [original plugin page](https://builtbybit.com/resources/customscreenmenu.108929/) and [LICENSE](LICENSE).

## What is included

- Per-menu privacy for displays and players, with optional protection from player damage.
- World-space menus rendered around the player camera.
- Text, item, button, input, state, slot, and schematic-preview systems.
- A public API for menus, elements, input fields, slots, state, actions, data providers, conditions, and components.
- Optional integrations built into the UIcore JAR: Vault and LuckPerms.
- Paper and Folia-aware scheduling with Java 17 bytecode.
- Russian and English guides, including a full menu authoring reference.

## Build UIcore

Run:

```bash
mvn clean package
```

The build produces:

- `uicore-api/target/uicore-api-1.0.0.jar`
- `uicore-plugin/target/UIcore-1.0.0.jar`
- `uicore-example-extension/target/uicore-example-extension-1.0.0.jar`

## Add the API to a plugin

```xml
<dependency>
  <groupId>org.orvencraft.uicore</groupId>
  <artifactId>uicore-api</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Runtime requirements

- Minecraft 1.21.1 and newer on Paper, Folia, Purpur, or a compatible fork.
- Java 17 bytecode. Use the Java runtime required by the selected server build.
- PacketEvents 2.14.0 is required.
- PlaceholderAPI, FancyNpcs, SkinsRestorer, CraftEngine, Vault, and LuckPerms are optional.

**Client compatibility:** NoChatReports may cause chat input to fail, including after a menu closes. See the [symptoms, cause, and workaround](docs/COMPATIBILITY.md#nochatreports-can-break-chat-input).

## Documentation

English is the primary documentation language. Each translated page links to its Russian counterpart.

- [Documentation index](docs/README.md)
- [Menu authoring guide](docs/MENU_AUTHORING_GUIDE.md) · [Русская версия](docs/MENU_AUTHORING_GUIDE_RU.md)
- [Extension API](docs/EXTENSION_API.md) · [Русская версия](docs/EXTENSION_API_RU.md)
- [Built-in integrations](docs/BUILTIN_INTEGRATIONS.md) · [Русская версия](docs/BUILTIN_INTEGRATIONS_RU.md)
- [Compatibility matrix](docs/COMPATIBILITY.md) · [Русская версия](docs/COMPATIBILITY_RU.md)
- [Installation guide](release/INSTALL.md) · [Русская версия](release/INSTALL_RU.md)
- [Runtime contracts](docs/RUNTIME_CONTRACTS.md) · [Smoke checklist](docs/SMOKE_CHECKLIST.md)

## Try the demo menus

Open `/uicore run demo_main` to explore the three bundled examples. They use vanilla Minecraft items and work without a resource pack or optional plugins.

| Menu                                                                   | Features                                                                       |
| ---------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| [demo_main](uicore-plugin/src/main/resources/menu/demo_main.yml)       | Hover effects, cursor changes, chat input, toggles, and click cooldowns        |
| [demo_gallery](uicore-plugin/src/main/resources/menu/demo_gallery.yml) | Item displays, your avatar, and a template slot with three selectable previews |
| [demo_states](uicore-plugin/src/main/resources/menu/demo_states.yml)   | Text, material, visibility, permission styles, and click requirements          |

The gallery selectors require `uicore.slot` (granted to OP players by default). See the [demo walkthrough](showcase/README.md) · [Русская версия](showcase/README_RU.md).

## Showcase videos

### Custom server menu

An example of a server interface built with UIcore: a player profile, world selection, friends, and settings, connected through menu navigation.

https://github.com/user-attachments/assets/50bd8035-eb6a-4ecd-9941-b3f17b2cd196

### Bundled demo menus

A walkthrough of `demo_main`, `demo_states`, and `demo_gallery`: hover feedback, chat input, toggles, element states, item displays, avatars, and template selection.

https://github.com/user-attachments/assets/60c09f38-9699-4fa7-9ec8-5ccf51af7e2b

[Recording notes](showcase/videos/README.md) stay outside the plugin JAR. The three demo menus are also the plugin's default examples and are installed on a fresh server.

The repository contains source and documentation only. The plugin JAR is created by the Maven build and is not committed to Git.
