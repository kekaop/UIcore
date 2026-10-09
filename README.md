# UIcore

### A world-space UI framework for Minecraft servers

UIcore turns the game world into a stage for menus. Text, items, buttons, input fields, stateful elements, animated slots, NPC mirrors, and per-player previews live in the same scene as the player instead of inside a flat inventory window.

This is the first public release of UIcore: a focused, extensible continuation of CustomScreenMenu with a proper Java API, YAML authoring, Paper/Folia support, and built-in integrations for Vault and LuckPerms.

The project exists thanks to **wuhaoxian123**, the original CustomScreenMenu author. UIcore is based on the paid 1.4.8 build, with permission to publish the complete source under AGPL-3.0. See the [original plugin page](https://builtbybit.com/resources/customscreenmenu.108929/), [LICENSE](LICENSE), and [provenance](PROVENANCE.md).

## What is included

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

- Minecraft 1.20.1–26.2 on Paper, Folia, Purpur, or a compatible fork.
- Java 17 bytecode. Use the Java runtime required by the selected server build.
- PacketEvents 2.14.0 is required.
- PlaceholderAPI, FancyNpcs, SkinsRestorer, CraftEngine, Vault, and LuckPerms are optional.

## Documentation

- [Menu authoring guide](docs/MENU_AUTHORING_GUIDE_RU.md) · [English extension API](docs/EXTENSION_API_EN.md)
- [Русская документация API](docs/EXTENSION_API_RU.md) · [Built-in integrations](docs/BUILTIN_INTEGRATIONS_EN.md)
- [Встроенные интеграции Vault и LuckPerms](docs/BUILTIN_INTEGRATIONS_RU.md)
- [Runtime contracts](docs/RUNTIME_CONTRACTS.md) · [Compatibility matrix](docs/COMPATIBILITY_RU.md)

## Showcase videos

The README is ready for the final recordings. Replace each `VIDEO_*_URL` marker with the public video link after uploading it.

| Video | Link |
| --- | --- |
| Full UIcore tour | `VIDEO_FULL_TOUR_URL` |
| Interactions: hover, click, states, and input | `VIDEO_INTERACTIONS_URL` |
| Dragging: groups and individual elements | `VIDEO_DRAGGING_URL` |
| Vault and LuckPerms integrations | `VIDEO_INTEGRATIONS_URL` |
| Slots and schematic previews | `VIDEO_SLOTS_SCHEMATICS_URL` |

The matching showcase menus are in [`showcase/menus`](showcase/menus), with recording notes in [`showcase/videos`](showcase/videos). They stay outside the Maven resource tree and are not included in the plugin JAR.

The repository contains source and documentation only. The plugin JAR is created by the Maven build and is not committed to Git.
