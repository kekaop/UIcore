# Planned UIcore work

> **Language:** English | [Русская версия](PLANNED_TASKS_RU.md)

This page records the current direction for UIcore. It is not a release guarantee.

1. **Drag controls.** A menu may move a named group of elements and rotate one target element through separate controls. The two gestures can coexist in the same menu. Final behavior needs an in-game check on supported server versions.
2. **Element states.** `initial-state`, `states`, `set-state`, and `set-states` are implemented. State is per player and is cleared when the menu closes. See `demo_states.yml`.
3. **Server compatibility.** UIcore targets Minecraft 1.21.1 and newer with Java 17 bytecode. Connected-player and optional-integration testing remains. See the [compatibility matrix](COMPATIBILITY.md) and [Folia port status](PORT_26_2.md).
4. **Schematic previews.** The base implementation reads Sponge `.schem` v2/v3, binds up to four previews to a menu, sends viewer-only block changes, and restores live blocks when a menu closes or changes. See the [menu authoring guide](MENU_AUTHORING_GUIDE.md) for format and limits.

Future input ideas include mouse-wheel navigation by cycling an invisible hotbar item. It remains an idea and is not part of the public API or a release promise.
