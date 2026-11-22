# UIcore roadmap

[Русская версия](ROADMAP_RU.md)

UIcore 1.1 ships [camera cutscenes](CUTSCENES.md), [drag and drop, and mouse-wheel zoom](INTERACTIONS_1_1.md). The sections below describe the planned scope of 1.2 and 1.3.

## World-space menus (1.2)

- Place interactive menus on walls, floors, ceilings, and arbitrary surfaces. Anchor them to a block or a fixed point in the world independently of the player's camera.
- Configure coordinates, rotation, scale, and interaction range. Account for orientation and scale in hit testing.
- Support shared and per-player menus using existing UIcore components, including buttons, groups, sliders, and drag-and-drop.
- Use cases include maps, noticeboards, terminals, control panels, and puzzles.

## Mouse wheel zoom (1.1)

- Enable zoom per menu or group. Configure the scale range, sensitivity, step size, smoothing, and pivot.
- Scale child elements and hitboxes together while preserving drag and drop behavior.
- Zoom uses held-slot packets while keeping the spectator camera. Wraparound counts as a step and the server slot stays unchanged. An adjacent number key sends the same packet as scrolling. See [zoom settings](INTERACTIONS_1_1.md#mouse-wheel-zoom).
- Use cases include maps, skill trees, large diagrams, and editors.

## Web UI editor (1.3)

- Build menus by dragging elements and edit their nested group hierarchy. Configure geometry, text, colors, materials, hitboxes, drag limits, and drop zones.
- Edit actions, events, animations, transitions, state variables, and data bindings. Preview the menu and try its interactions.
- Import and export UIcore YAML, switch between the visual editor and YAML source, and report validation errors. Handwritten YAML remains fully supported.
- Add a template library, element duplication and reuse, undo and redo, grid snapping, and local project storage. A later option is to connect to a Minecraft server for previews and menu updates.

## 3D model integrations (1.3)

- Use separate optional adapters for MythicMobs, ModelEngine, and BetterModel to show creatures, items, and other models alongside standard UIcore components. Missing plugins must leave UIcore and the other adapters functional.
- Support multiple models per menu, per-player visibility, configurable position, scale and orientation, and manual or automatic rotation. Play available animations, including idle, and switch animations through menu actions.
- Create, update, and remove models with the menu lifecycle. Where possible, use visual instances without mob AI or combat logic, and limit active models and update costs.
- Use cases include bestiaries, character selection, and previews of pets, classes, bosses, mounts, and cosmetics.

## MythicMobs skills (1.3)

- Trigger skills from buttons and interface events, including drops and slider changes. Combine skill calls with other UIcore actions.
- Pass skill arguments and player or target context where supported by the MythicMobs API. Show availability and cooldowns when exposed, and handle execution errors.
- Skills that require a successful drop can use YAML `on-drop` actions or the Java `MenuDropEvent`. See [actions after a drop](INTERACTIONS_1_1.md#actions-after-a-drop).

## MythicRPG integration (1.2)

- **Talents.** Show nodes, connections, requirements, costs, and unlock state. Support panning and zooming, and send unlock requests to MythicRPG.
- **Classes and professions.** Show stats, requirements, and features, and allow users to select a class or profession. Provide a 3D preview through ModelEngine or BetterModel.
- **Spells.** Show learned and available abilities, requirements, costs, and cooldowns. Allow dragging abilities between slots where MythicRPG supports it.
- **Character stats.** Bind levels, experience, attributes, and available points to menu elements and update them as character data changes.
- **Maps.** Show locked and unlocked fast-travel points and allow users to choose a destination.
- Keep the integration optional. MythicRPG owns game data, validation, and game logic. UIcore displays the data and forwards input. Feature availability depends on the MythicRPG public API.
