# UIcore menu authoring guide

> **Language:** English | [Русская версия](MENU_AUTHORING_GUIDE_RU.md)

Use this guide to create a YAML menu, position it around the camera, add interactive elements, and verify the finished scene. UIcore installs `demo_main`, `demo_gallery`, and `demo_states` in `plugins/UIcore/menu/`; copy an example before changing it. See the [demo walkthrough](../showcase/README.md).

## Create and reload a menu

Create a `.yml` file in `plugins/UIcore/menu/`, then reload it:

```text
/uicore reload
/uicore run <menu-id>
```

Start with a player-relative camera. It keeps the first scene near the player while you design it.

```yaml
welcome:
  camera-position:
    type: player
    distance: 1.5
  layout:
    title:
      type: text
      name: "<gold>Welcome"
      x: 0
      y: 0.45
      z: 3.5
      text-size: 0.45
```

`x`, `y`, and `z` are positions relative to the menu camera. Positive `x` moves right, positive `y` moves up, and positive `z` moves forward from the camera. Keep interactive elements close enough to the cursor range configured in `config.yml`.

The source config uses `cursor-item.z-offset: 1.0` to offset the visual cursor along the camera direction. Updating the plugin preserves an existing server config.

## Set menu privacy and player damage

Put these settings directly under the menu ID, alongside `camera-position` and `layout`. The values below are the defaults, including for existing menus that omit the settings.

```yaml
welcome:
  hide-menu-for-other-player: true
  hide-player-for-other-player: true
  hide-other-players: false
  can-damaged: false
  camera-position: { type: player, distance: 1.5 }
  layout:
    title:
      name: "<gold>Welcome"
      x: 0
      y: 0.45
      z: 3.5
```

| Setting                        | Default | Behavior                                                                                                                                   |
| ------------------------------ | ------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `hide-menu-for-other-player`   | `true`  | Shows menu text, items, the cursor, and temporary menu text only to the menu owner. Set `false` to let nearby players see those displays.  |
| `hide-player-for-other-player` | `true`  | Hides the menu owner from other players using Bukkit player visibility, including the tab list. Set `false` to retain ordinary visibility. |
| `hide-other-players`           | `false` | Set `true` to hide other players from the menu owner.                                                                                      |
| `can-damaged`                  | `false` | Blocks incoming damage caused by other players, including their projectiles. Set `true` to let the server's usual damage rules apply.      |

The visibility settings are independent. If one player hides themselves and another hides other players, both rules apply. Closing one menu removes only that visit's rule. Settings change when switching menus and are reapplied for new observers and world changes. Closing, death, or disconnect ends the visit's damage protection and restores ordinary player visibility; another plugin's hide requests remain in effect. Existing invisibility isn't removed when a menu opens.

Public displays show the owner's rendered content, including placeholders and input values, to nearby players. They remain interactive only for the owner. Password fields still use their configured masking. Personal NPC mirrors and client-only schematic previews remain personal.

`can-damaged` controls incoming damage from other players only. It doesn't grant permission to attack, override PvP restrictions, or prevent damage from mobs or the environment. Use actual YAML booleans (`true` or `false`); invalid values reject the menu during loading.

## Choose an element type

Elements live under `layout:`. Text is the default when `type` is omitted.

| Type            | Use it for                                                                      |
| --------------- | ------------------------------------------------------------------------------- |
| `text`          | Labels, buttons, tooltips, and formatted MiniMessage text.                      |
| `item`          | An `ItemDisplay` with a material, model data, transform, and optional rotation. |
| `player-avatar` | A player's face or full head display.                                           |
| `input-field`   | A chat-backed field that accepts text from the player.                          |
| `bar-text`      | A text progress bar.                                                            |

Common fields include `name`, `x`, `y`, `z`, `scale`, `billboard`, `tilt`, `hitbox`, `hover-style`, `pressed-style`, `visible-if`, and `permission`.

```yaml
layout:
  play:
    name: "<green>Play"
    x: 0
    y: 0
    z: 3.5
    hitbox: { width: 1.4, height: 0.42, depth: 0.25 }
    hover-style:
      color: "#FFFF55"
    command:
      - "[player] warp spawn"
```

Hitboxes extend around an element's center. Use a positive `height`, and make the box cover the visible text or item without overlapping another interactive element.

## Use text, items, and avatars

Text accepts MiniMessage. Use quoted YAML strings when a value includes MiniMessage, a formula, `on`, `off`, or a number that must stay text.

```yaml
layout:
  line:
    name:
      - "<aqua>First line"
      - "<gray>Second line"
    x: 0
    y: 0.25
    z: 3.5
    alignment: center
```

Items use a Minecraft material and can use a GUI-like transform. `rotate` applies continuous item rotation; `rotate-speed` is measured in degrees per tick.

```yaml
layout:
  reward:
    type: item
    material: DIAMOND_SWORD
    transform: gui
    scale: 0.7
    x: 0
    y: 0
    z: 3.5
    rotate: true
    rotate-speed: 2
```

A `player-avatar` can render a named player. Set `player: self` for the viewer and `face-only: true` for a face display. Give it a separate hitbox if it is clickable.

## Control visibility, permissions, and clicks

These settings answer different questions:

- `permission` controls whether the player may use an element or menu.
- `visible-if` controls whether the element is rendered.
- `click-requirements` controls whether a specific click may run its actions.

```yaml
layout:
  rank_button:
    name: "<gold>Rank reward"
    x: 0
    y: 0
    z: 3.5
    permission: "server.rank.reward"
    visible-if:
      placeholder: "%player_level%"
      operator: ">="
      value: "10"
    click-requirements:
      placeholders:
        - { placeholder: "%player_level%", operator: ">=", value: "20" }
      denied:
        actionbar: "<red>You need level 20."
    command:
      - "[player] rewards claim"
```

Player commands use `[player]`. Console commands are disabled by default. Allow them only after configuring `security.allow-console-commands` and a narrow `security.console-command-allowlist` in `config.yml`. `[op]` never grants temporary operator status.

## Use states and navigation

`states` changes one element's configured appearance or visibility. `toggle-content` is for a button that toggles its own on/off content. Both are separate from menu navigation.

```yaml
layout:
  mode:
    name: "<gray>Mode: off"
    x: 0
    y: 0.2
    z: 3.5
    initial-state: idle
    states:
      idle: { text: "<gray>Mode: off" }
      active: { text: "<green>Mode: on" }
  enable:
    name: "<green>Enable"
    x: 0
    y: -0.2
    z: 3.5
    set-state:
      element: mode
      state: active
```

Use `next-menu.menu` to change scenes and `stop-menu.enabled: true` to close the current menu. Give each button a `cooldown` when an action should not run repeatedly.

## Collect chat input

An input element opens a chat capture for the player. Configure field validation and password handling in `input.yml`.

```yaml
layout:
  nickname:
    type: input-field
    name: "<yellow>Enter a nickname"
    field: nickname
    x: 0
    y: 0
    z: 3.5
    input-field:
      prompt: "<yellow>Type your nickname in chat."
```

Use `{input.nickname}` only in actions that need the value. UIcore inserts input as literal display text, masks password fields, and omits their values from denied-command logs.

## Animate an element or position

Use `entrance-animation` for a one-time appearance and `animation` for a continuous text animation. `move`, `fade`, `rotate`, `scale`, and `bounce` are supported continuous types.

```yaml
layout:
  title:
    name: "<aqua>UIcore"
    x: 0
    y: 0.4
    z: 3.5
    entrance-animation:
      enabled: true
      from: top
      distance: 0.3
      duration: 12
    animation:
      type: move
      axis: y
      speed: 1.2
      amplitude: 0.12
```

Coordinates may also be formulas. `t` is seconds since the menu session began, `ticks` is the tick count, and `pi` and `e` are constants. Use `position-update` to choose an interval, smoothing, fallback, and clamps.

```yaml
layout:
  moving_label:
    name: "<aqua>Moving"
    x: "0.9 * sin(t * 1.4)"
    y: 0.2
    z: 3.5
    position-update:
      interval: 1
      smoothing: 0.18
      fallback: { x: 0 }
      clamp:
        x: [-1.0, 1.0]
```

Formulas can include PlaceholderAPI values and `{state.key}` only when they resolve to finite numbers.

## Use templates and slots

Templates are complete element definitions under `templates:`. A slot chooses which template to show, while the slot sets its position and optional hitbox.

```yaml
templates:
  bronze:
    type: item
    material: IRON_SWORD
    scale: 0.7
    transform: gui
slots:
  reward:
    x: 0
    y: 0.2
    z: 3.5
    content: bronze
    fallback: bronze
```

Use `/uicore slot set`, `clear`, `reset`, `transition`, or `refresh` to update a player's assignment. Global variants affect every player. See [slots and templates](SLOTS_AND_TEMPLATES.md) for commands and Java API usage.

## Show a schematic preview or a personal NPC

Put Sponge `.schem` v2/v3 files in `plugins/UIcore/schematics/`. A menu may show up to four previews. Previews are sent only to the viewer and do not modify world blocks.

```yaml
schematics:
  - file: frame.schem
    anchor: camera
    x: -2
    y: -1
    z: 4
```

Files are limited to simple `.schem` names with no subdirectories. UIcore restores the real world blocks when the menu closes or changes. It does not reproduce block-entity contents, entities, or biomes.

FancyNpcs powers the optional personal NPC mirror. Configure global defaults in `npc_config.yml` or override them under the menu's `npc:` section. The [animation and display section](#animate-an-element-or-position) explains related display options.

## Move a group or rotate one element

Use separate control elements for these independent gestures. All elements in a moving list share `drag-group`; a move handle names that group. A rotation handle names one target element.

```yaml
layout:
  first:
    name: "First"
    drag-group: entries
    x: -0.5
    y: 0.3
    z: 3.5
  second:
    name: "Second"
    drag-group: entries
    x: -0.5
    y: 0
    z: 3.5
  move_entries:
    name: "Move list"
    x: -0.5
    y: -0.5
    z: 3.5
    drag:
      mode: move-group
      group: entries
      sensitivity: 1.0
  preview:
    type: item
    material: DIAMOND_SWORD
    x: 0.5
    y: 0.2
    z: 3.5
  rotate_preview:
    name: "Rotate item"
    x: 0.5
    y: -0.5
    z: 3.5
    drag:
      mode: rotate-element
      target: preview
      sensitivity: 1.0
```

While the left mouse button is held, the cursor stays in place and mouse movement moves the group or rotates the target. Releasing the button ends the gesture and the cursor resumes from its stored position. `sensitivity` accepts values from `0.01` to `10`. UIcore stores the group position and rotation per player for the menu session.

Drag capture uses a temporary client-only barrier block and survival mode only for the active gesture. UIcore restores the player's original game mode when the gesture or menu ends. Test the feature with a normal client on your target server build before publishing a menu that depends on it.

## Check the finished menu

Before publishing a menu:

- Give each menu a valid `camera-position` and each `next-menu.menu` an existing ID.
- Keep hitboxes meaningful and separate from neighboring controls.
- Check material, transform, billboard, tilt, scale, and resource-pack appearance for every item.
- Check permissions, `visible-if`, click requirements, states, and cooldowns independently.
- Confirm schematic files exist and close the menu to verify block restoration.
- Run `/uicore reload`, read the server log, and complete the [smoke checklist](SMOKE_CHECKLIST.md).

Useful commands include `/uicore run <menu>`, `/uicore stop`, `/uicore reload`, `/uicore slot ...`, `/uicore items <id>`, and `/uicore text open <id>`.
