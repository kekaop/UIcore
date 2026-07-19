# CustomScreenMenu local patch notes for upstream

This is a short list of the local changes made in this test workspace.

The workspace is based on a decompiled/test copy, so the best path is to port
the behavior into the original source tree instead of copying the whole folder
as-is.

## Version used for local testing

```text
1.4.8-dev20
```

## Main fixes

- Fixed menu transitions where `next-menu` could break hover/click state.
- Fixed cursor movement after switching to another menu.
- Reduced transition flicker by keeping the camera path alive where possible.
- Fixed toggle text updates for all matching TextDisplays.
- Fixed YAML `on` / `off` parsing for `toggle-content`.
  Bukkit/YAML may parse unquoted `on` and `off` as boolean keys, so examples now
  use quoted `'on'` and `'off'`.
- Removed temporary toggle debug spam from the local test build.
- Translated the default `config.yml` comments/messages to English.

## New menu config features

### Hover and pressed styles

- `hover-style`
  - color
  - bold / italic / underlined / strikethrough / obfuscated
  - smooth color transition metadata
- `pressed-style`
  - temporary click feedback
  - optional replacement text through `pressed-style.text`
  - duration in ticks
- `hover-cursor`
  - temporary cursor item override
  - supports `custom-model-data`
  - optional scale override
- `hover-tooltip`
  - actionbar/message style hover hint

### Permission-aware style and denied actions

- `permission-style.allowed`
- `permission-style.denied`
- `permission-denied.message`
- `permission-denied.actionbar`
- `permission-denied.commands`

This is separate from click-time checks. It only changes how the element looks
and what happens when the player clicks without the required permission.

### Click requirements

Added click-time requirements:

- permission checks
- placeholder checks
- denied message/actionbar/sound/commands

Example:

```yaml
click-requirements:
  permissions:
    - customscreenmenu.demo.click
  placeholders:
    - placeholder: "%player_world%"
      operator: "=="
      value: "world"
  denied:
    actionbar: "<red>Click denied"
    commands:
      - "[message] <gray>Denied command example"
```

### Toggle content

Added per-player local toggle display state:

```yaml
toggle-content:
  enabled: true
  'off': "[ ] Enabled"
  'on': "[x] Enabled"
  on-commands:
    - "[actionbar] <green>Enabled"
  off-commands:
    - "[actionbar] <red>Disabled"
```

Added `toggle-style`:

```yaml
toggle-style:
  'off':
    color: "#ff6b6b"
    strikethrough: true
  'on':
    color: "#62e67a"
    bold: true
```

### Input fields

Input fields can now be used in any menu, not only password/login menus.

Supported placeholders:

```text
%cursormenu_input_field%
%input_field%
{input.field}
{menu.input.field}
```

Menu elements can either use `type: input-field` or attach `input-field` to a
normal clickable element.

### Visible conditions

Added `visible-if` support for:

- permissions
- placeholders
- local API state
- `all`, `any`, `not`

Example:

```yaml
visible-if:
  placeholder: "%player_world%"
  equals: world
```

Example with API state:

```yaml
visible-if:
  state.page: 2
```

### Animation aliases

Added config aliases around existing floating animation behavior:

- `fade`
- `move`
- `rotate`
- `scale`
- `bounce`

### Per-layout cooldown

Added click cooldown:

```yaml
cooldown:
  key: reward_button
  time: 5s
  denied:
    actionbar: "<red>Wait {cooldown.left}s"
```

Supported time formats include `500ms`, `5s`, `2m`, `20ticks`, `ticks`, and
`seconds`.

### Text progress bars

Added `type: bar-text`.

```yaml
type: bar-text
value: "%player_health%"
max: "20"
segments: 20
filled: "<green>|"
empty: "<dark_gray>|"
prefix: "<red>Health "
suffix: " <gray>{value}/{max} ({percent}%)"
```

### ItemDisplay in normal menus

Text and ItemDisplay elements can live in the same menu.

Item menu elements support:

- `material`
- `custom-model-data`
- `scale`
- `glow`
- `rotate`
- `rotate-speed`
- `billboard`
- custom hitbox for click/hover routing

## MiniMessage / chat messages

Local command/message utilities were adjusted so menu messages can use
MiniMessage-like formatting through the existing Adventure parser path.

Link messages are still supported through `[link]`.

## Runtime API

Added/extended API surface:

- open menu
- close menu
- add element
- remove element
- edit element
- player-scoped element overrides
- input field focus/read/write
- per-player menu state
- menu refresh

Important behavior:

- `api.addElement(menuId, element)` is global.
- `api.addElement(player, menuId, element)` is only for that player.
- Same rule applies to remove/edit calls with a `Player` argument.

## Event API

Added event classes:

- `MenuOpenEvent`
- `MenuCloseEvent`
- `MenuSwitchEvent`
- `MenuHoverEvent`
- `MenuClickEvent`
- `MenuInputEvent`

## English demo menus

Added clean English demo menus:

```text
demo_main.yml
demo_styles.yml
demo_toggle_input.yml
demo_items_api.yml
demo_conditions.yml
```

Open the hub:

```text
/cmenu run demo_main
```

Each demo uses:

```yaml
camera-position:
  distance: 1.5

layout:
  some_element:
    z: 3.5
```

## Files worth reviewing

Core behavior:

```text
src-decompiled/com/cmenu/ui/CursorMenuPlugin.java
src-decompiled/com/cmenu/ui/HoverEnlargeManager.java
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
src-decompiled/com/cmenu/ui/TextDisplayHitBox.java
src-decompiled/com/cmenu/ui/util/CommandUtils.java
src-decompiled/com/cmenu/ui/MenuListener.java
src-decompiled/com/cmenu/ui/Commands.java
src-decompiled/com/cmenu/ui/CursorMenuPlaceholder.java
src-decompiled/com/cmenu/ui/section/MenuConfigLoader.java
src-decompiled/com/cmenu/ui/section/Section.java
```

API:

```text
src-decompiled/com/cmenu/ui/api/
```

Configs and docs:

```text
src-decompiled/config.yml
docs/API_USAGE.md
docs/DEV12_CHANGE_LIST.md
docs/DEV18_CHANGE_LIST.md
docs/CURSOR_RANGE_NOTES.md
docs/CustomScreenMenu_GitBook.html
```

Demo menus:

```text
src-decompiled/menu/demo_main.yml
src-decompiled/menu/demo_styles.yml
src-decompiled/menu/demo_toggle_input.yml
src-decompiled/menu/demo_items_api.yml
src-decompiled/menu/demo_conditions.yml
```

## Known risks / porting notes

- This is not a clean upstream source checkout.
- Some older files in the workspace still contain legacy mojibake text.
- Auth/password support was kept intentionally.
- `[op]` and `[console]` command behavior was not redesigned in this pass.
- PacketEvents and Paper 26.1.2 behavior should be smoke-tested on a real
  server after porting.
- Menu transition/camera logic is sensitive. Port those changes carefully and
  test `next-menu` with hover/click/cursor movement after the switch.

## Local smoke test checklist

```text
/cmenu reload
/cmenu run demo_main
```

Check:

- hover enlarges text;
- hover style changes color/underline;
- hover-cursor changes item;
- pressed-style text appears and returns;
- toggle display state persists;
- input field updates from chat;
- cooldown denies repeated clicks;
- item preview receives hover/click;
- next-menu transitions keep cursor movement working.
