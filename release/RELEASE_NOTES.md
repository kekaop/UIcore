# UIcore 1.1.0

> **Language:** English | [Русская версия](RELEASE_NOTES_RU.md)

Build interactive tours, movable panels, and zoomable menus around the player's camera. UIcore 1.1.0 works with a vanilla Minecraft client.

## Camera cutscenes

- Define routes, turns, pauses, and branching choices in YAML. Configure movement and rotation curves, and keep each player's camera and choices independent.
- Attach a cutscene to a menu, start it through a menu action or the Java API, and save viewpoints in game.
- Try `/uicore run demo_cutscene`. [Cutscene guide](https://github.com/kekaop/UIcore/wiki/Cutscenes)

## Drag and drop, groups, and sliders

- Set `parent` to move child elements with their group. Configure drag bounds, handles, and allowed drop targets.
- Use YAML `on-drop` or Java `MenuDropEvent` to run logic after a successful drop has been applied. Use sliders to write stepped values to menu state.
- Drag uses two clicks by default. `demo_drag` shows the experimental hold-to-drag mode for stationary menus. Minecraft can still draw its first-person crosshair in that mode. [Interaction guide](https://github.com/kekaop/UIcore/wiki/Interactions)

## Mouse-wheel zoom

- Zoom camera-relative elements or one group with a configurable pivot, scale range, and smoothing. Click and drop targets scale with the elements.
- Try `/uicore run demo_zoom`. Zoom uses the two-click drag mode. [Zoom configuration](https://github.com/kekaop/UIcore/wiki/Interactions#mouse-wheel-zoom)

## Fixes and upgrading

- Improved hit testing on transformed elements and kept render and input calculations on the same menu frame.
- Fixed action context for immediate and delayed actions, and kept the active configuration when a reload fails.
- Improved recovery of a player's original state when a menu session ends unexpectedly.
- Install `UIcore-1.1.0.jar` and [PacketEvents 2.14.0 Spigot](https://modrinth.com/plugin/packetevents/version/m78nFxYg) in `plugins/`. Restart the server after replacing the JAR. [Installation guide](https://github.com/kekaop/UIcore/wiki/Installation) · [Release verification](https://github.com/kekaop/UIcore/wiki/Compatibility#tested-builds)
