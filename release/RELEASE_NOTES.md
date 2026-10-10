# UIcore 1.0.0

> **Language:** English | [Русская версия](RELEASE_NOTES_RU.md)

This is the first public UIcore release based on CustomScreenMenu 1.4.8. It targets Minecraft 1.21.1 and newer and has startup, reload, and shutdown checks on available Paper, Folia, Purpur, and Leaf builds. The [compatibility matrix](../docs/COMPATIBILITY.md) records the exact scope.

This release includes element states (`initial-state`, `states`, `set-state`, and `set-states`) and player-specific Sponge `.schem` v2/v3 previews. It keeps permission conditions and improves player restoration after a menu closes, entity and task lifecycle handling, configuration validation, click and input processing, and Folia scheduler support. The JAR is built against the Paper 1.21.1 API with Java 17 bytecode.

Known limits: schematic previews do not show block-entity contents, entities, or biomes. Optional plugin integrations must be checked on the target server build.

## Corrections before release

- Legacy blockcache recovery runs each block operation on its Folia region, then persists the journal after all results arrive. Failed or conflicting entries remain available for retry.
- Global API element mutations are serialized per menu; renderers use immutable definition snapshots.
- CraftEngine image/shift tags use the Adventure tag resolver builder alongside standard tags.
- The subprocess test chooses `java` on Linux and `java.exe` on Windows.
- Each menu has independent settings for public displays, player visibility in both directions, and incoming player damage. See the [menu privacy settings](../docs/MENU_AUTHORING_GUIDE.md#set-menu-privacy-and-player-damage).

- Removed the remaining experimental drag capture, group movement, and interactive rotation code and its obsolete configuration examples.
- Internal teleport markers match the requested destination and cannot be cleared by completion of an older request.
- Commands queued across entity/global schedulers recheck their originating menu session before execution.
- Configuration rejects non-string permissions and non-map click requirements.
- Empty or failed extension data results respect the provider refresh interval.
- Build instructions specify JDK 21 or newer; example extension build outputs are excluded from source control.

Automated tests cover these paths. Multiplayer rendering and optional integrations still need checks on the target server. The subprocess test ran on Windows; an actual Linux run remains pending.
