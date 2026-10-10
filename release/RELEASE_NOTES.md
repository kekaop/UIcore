# UIcore 1.0.0

> **Language:** English | [Русская версия](RELEASE_NOTES_RU.md)

This is the first public UIcore release based on CustomScreenMenu 1.4.8. It targets Minecraft 1.21.1 and newer and has startup, reload, and shutdown checks on available Paper, Folia, Purpur, and Leaf builds. The [compatibility matrix](../docs/COMPATIBILITY.md) records the exact scope.

This release includes element states (`initial-state`, `states`, `set-state`, and `set-states`) and player-specific Sponge `.schem` v2/v3 previews. It keeps permission conditions and improves player restoration after a menu closes, entity and task lifecycle handling, configuration validation, click and input processing, and Folia scheduler support. The JAR is built against the Paper 1.21.1 API with Java 17 bytecode.

Known limits: schematic previews do not show block-entity contents, entities, or biomes. Optional plugin integrations must be checked on the target server build.
