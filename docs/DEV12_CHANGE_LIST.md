# CustomScreenMenu local dev change list

This is the full local change list prepared on top of the original
CustomScreenMenu 1.4.8 test tree.

Current packaged jar:

```text
dist/CustomScreenMenu-1.4.8-dev12.jar
```

Important note: this workspace is based on decompiled source. Treat these
changes as a reference implementation / patch list for porting into the real
source tree, not as a clean upstream branch.

## Git baseline and existing commits

Baseline commit:

```text
384477d Initial CustomScreenMenu test1 baseline
```

Local commits already present in the repo:

```text
8834fca Add hover styles and MiniMessage chat support
22e9f43 Add permission style demo menu
e87d905 Add bilingual plugin HTML documentation
441cda5 Add permission denied click actions
```

## Runtime features added

### 0. Dev12 event API, conditions, state, and animation

Added Bukkit events:

```text
MenuOpenEvent
MenuCloseEvent
MenuSwitchEvent
MenuHoverEvent
MenuClickEvent
MenuInputEvent
```

`MenuClickEvent` and `MenuInputEvent` are cancellable.

Added player-scoped menu state API:

```java
api.setState(player, "demo_main", "page", "2");
api.getState(player, "demo_main", "page");
api.clearState(player, "demo_main", "page");
```

Added `visible-if` conditions for render/hover/click filtering:

```yaml
visible-if:
  placeholder: "%player_world%"
  equals: world

visible-if:
  state.page: 2
```

Added YAML `animation` alias with `fade`, `move`, `rotate`, `scale`, and
`bounce` types.

Added demo menu file:

```text
src-decompiled/menu/dev12_showcase.yml
server-data-current/menu/dev12_showcase.yml
E:\Minecraft servers\OrvenCraft\test1\plugins\CustomScreenMenu\menu\dev12_showcase.yml
```

Open with:

```text
/cmenu reload
/cmenu run dev12_showcase
```

Included menus:

- `dev12_showcase` - hub with hover, pressed style, permission style, event note.
- `dev12_conditions` - `visible-if` with placeholder, permission, input, and API state examples.
- `dev12_animations` - `fade`, `move`, `rotate`, `scale`, `bounce`, and legacy `floating-animation`.
- `dev12_input_items` - input fields, click requirements, item element, toggle content, MiniMessage link.

Main files:

```text
src-decompiled/com/cmenu/ui/CursorMenuPlugin.java
src-decompiled/com/cmenu/ui/HoverEnlargeManager.java
src-decompiled/com/cmenu/ui/MenuListener.java
src-decompiled/com/cmenu/ui/api/CustomScreenMenuApi.java
src-decompiled/com/cmenu/ui/api/CustomScreenMenuApiImpl.java
src-decompiled/com/cmenu/ui/api/event/*.java
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
```

### 1. Hover and text styles

Added element-level text styling:

```yaml
style:
  color: "#cbd5e1"
  bold: true
  underlined: false
  strikethrough: false

hover-style:
  color: "#ffffff"
  underlined: true
  transition:
    type: smooth
    duration: 6

pressed-style:
  text: "<green>Pressed"
  color: "#62e67a"
  duration: 5
```

Notes:

- Smooth transition is currently color-focused.
- Formatting changes are instant.
- `pressed-style.text` is temporary click feedback and returns after duration.

Main files:

```text
src-decompiled/com/cmenu/ui/style/TextStyleConfig.java
src-decompiled/com/cmenu/ui/HoverEnlargeManager.java
src-decompiled/com/cmenu/ui/TextDisplayManager.java
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
```

### 2. Permission style and permission denied actions

Added visual styles based on element permission:

```yaml
permission-style:
  allowed:
    color: "#62e67a"
  denied:
    color: "#777777"
    strikethrough: true
```

Added denied-click behavior:

```yaml
permission-denied:
  message: "<red>No permission."
  actionbar: "<gray>Missing permission"
  sound: minecraft:entity.villager.no
  commands:
    - "[message] <red>Access denied"
```

When permission is denied, normal element actions are skipped and the denied
actions run instead.

Main files:

```text
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
src-decompiled/com/cmenu/ui/HoverEnlargeManager.java
```

### 3. Hover cursor and hover tooltip

Added temporary cursor item override while hovering an element:

```yaml
hover-cursor:
  enabled: true
  material: EMERALD
  custom-model-data: 0
  scale: 1.15
```

Added tooltip output on hover:

```yaml
hover-tooltip:
  type: actionbar
  text: "<yellow>Open settings"
```

Supported tooltip types:

```text
actionbar
message
chat
```

Main files:

```text
src-decompiled/com/cmenu/ui/HoverEnlargeManager.java
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
src-decompiled/com/cmenu/ui/CursorMenuPlugin.java
```

### 4. MiniMessage chat and actionbar output

Added MiniMessage support for command output helpers:

```yaml
command:
  - "[message] <green>Saved!"
  - "[actionbar] <yellow>Current value: {input.nickname}"
  - "[link]https://example.com|<aqua><underlined>Open website</underlined>"
```

`[link]url|display text` can use formatted display text while preserving the
clickable URL.

Main file:

```text
src-decompiled/com/cmenu/ui/util/CommandUtils.java
```

### 5. Universal input fields

Input fields are no longer only for password/login menus. Any menu element can
focus a chat-backed input field.

```yaml
nickname_field:
  type: input-field
  name: "<gray>Nickname: <white>{input.nickname}"
  field: nickname
  x: 0.0
  y: 0.2
  z: 3.5
  input-field:
    prompt: "<yellow>Type nickname in chat."
```

Also supported on a normal element:

```yaml
input-field: nickname
```

Supported placeholders:

```text
%cursormenu_input_nickname%
%input_nickname%
{input.nickname}
{menu.input.nickname}
```

Password-like fields are masked in menu text, but raw values are used in
commands.

Main files:

```text
src-decompiled/com/cmenu/ui/CursorMenuPlugin.java
src-decompiled/com/cmenu/ui/MenuListener.java
src-decompiled/com/cmenu/ui/Commands.java
src-decompiled/com/cmenu/ui/CursorMenuPlaceholder.java
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
src-decompiled/com/cmenu/ui/util/CommandUtils.java
```

### 6. Toggle content on click

Added per-player local toggle state for elements:

```yaml
toggle-content:
  enabled: true
  'off': "[ ] I accept agreement v1"
  'on': "[x] I accept agreement v1"
  on-commands:
    - "[message] <green>Enabled"
  off-commands:
    - "[message] <red>Disabled"
```

The state survives hover/pressed rendering and menu refreshes for the active
player session.

Main files:

```text
src-decompiled/com/cmenu/ui/CursorMenuPlugin.java
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
```

### 7. Click requirements

Added click-time checks separate from visual `permission-style`:

```yaml
click-requirements:
  permissions:
    - orven.example.use
  placeholders:
    - placeholder: "%player_world%"
      operator: "=="
      value: "world"
  denied:
    message: "<red>Requirements not met."
    actionbar: "<gray>Try later"
    commands:
      - "[message] <red>Denied by click-requirements"
```

Supported placeholder operators include:

```text
==
!=
contains
!contains
not_contains
```

Main file:

```text
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
```

### 8. Refresh menu after click

Added optional per-element menu refresh after click:

```yaml
refresh-menu:
  enabled: true
  delay: 2
```

Short form is also supported:

```yaml
refresh-menu: true
```

This is intended for menus where external state changes after a click, for
example permission updates or placeholder updates.

Main files:

```text
src-decompiled/com/cmenu/ui/CursorMenuPlugin.java
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
```

### 9. Item elements in normal menus

Added `type: item` layout elements using ItemDisplay. A hidden TextDisplay
hitbox is spawned next to the item so normal cursor hit detection can still
work.

```yaml
reward_item:
  type: item
  material: DIAMOND
  x: 0.0
  y: 0.25
  z: 3.5
  scale: 0.55
  glow: true
  rotate: true
  rotate-speed: 2.0
  hitbox:
    width: 0.8
    height: 0.8
    depth: 0.25
```

Normal text elements can still be used in menus that also contain item
elements.

Main files:

```text
src-decompiled/com/cmenu/ui/CursorMenuPlugin.java
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
src-decompiled/com/cmenu/ui/TextDisplayHitBox.java
```

### 10. Custom TextDisplay hitbox sizes

Added scoreboard-tag based custom hitbox dimensions:

```text
cmenu_hitbox:<width>:<height>:<depth>
```

This is used by item elements and can also improve click detection for small
or visually wide elements.

Main file:

```text
src-decompiled/com/cmenu/ui/TextDisplayHitBox.java
```

### 11. Runtime API

Added public API package:

```text
src-decompiled/com/cmenu/ui/api/CustomScreenMenuApi.java
src-decompiled/com/cmenu/ui/api/CustomScreenMenuApiImpl.java
src-decompiled/com/cmenu/ui/api/CustomScreenMenuProvider.java
src-decompiled/com/cmenu/ui/api/MenuElementEditor.java
src-decompiled/com/cmenu/ui/api/MenuElementSnapshot.java
src-decompiled/com/cmenu/ui/api/MenuElementSpec.java
src-decompiled/com/cmenu/ui/api/MenuMutationResult.java
```

API capabilities:

- Open menu.
- Close menu.
- Check active menu.
- List loaded menu ids.
- List element ids.
- Read element snapshot.
- Add runtime element.
- Remove runtime element.
- Edit runtime element.
- Add/edit/remove player-scoped runtime element.
- Clear player-scoped runtime elements.
- Refresh one player.
- Refresh all viewers of one menu.
- Focus input field.
- Read/write/clear input values.

Example:

```java
CustomScreenMenuApi api = CustomScreenMenuProvider.get();
api.openMenu(player, "demo_main");

// Global mutation: visible to everyone using this loaded menu.
api.addElement("demo_main",
    MenuElementSpec.builder("api_button")
        .text("<green>API Button")
        .position(0.0, -0.45, 3.5)
        .textSize(0.45)
        .commands(List.of("[message] <aqua>Clicked from API"))
        .hoverEnlarge(true, 1.2)
        .build());

// Player-scoped mutation: visible only to this player.
api.addElement(player, "demo_main",
    MenuElementSpec.builder("personal_reward")
        .text("<gold>Your reward")
        .position(0.0, -0.65, 3.5)
        .commands(List.of("[message] <green>Only you can see this."))
        .build());

api.editElement("demo_main", "api_button", edit -> edit
    .text("<yellow>Updated")
    .set("pressed-style.text", "<green>Pressed")
    .set("pressed-style.duration", 5));

api.editElement(player, "demo_main", "title", edit -> edit
    .text("<aqua>Hello, " + player.getName()));

api.removeElement(player, "demo_main", "personal_reward");
api.clearPlayerElements(player, "demo_main");

api.focusInputField(player, "nickname");
String nickname = api.getInput(player, "nickname").orElse("");
api.refresh(player);
api.closeMenu(player);
```

Main file registering the service:

```text
src-decompiled/com/cmenu/ui/CursorMenuPlugin.java
```

Scope behavior:

- `addElement(menuId, ...)`, `editElement(menuId, ...)`, and
  `removeElement(menuId, ...)` are global in-memory mutations.
- `addElement(player, menuId, ...)`, `editElement(player, menuId, ...)`, and
  `removeElement(player, menuId, ...)` are player-only overlays.
- Player-only overlays are not written to YAML.
- Player-only overlays are cleared by API clear calls, full reload/global
  cleanup, or player quit/session-reference cleanup. Ordinary menu close does
  not clear them.
- Rendering, hover, click, WASD target collection, and hit testing now read the
  player-aware layout set.

### 12. Menu transition / camera / cursor fixes

Local test builds included fixes around active menu session state and cursor
updates after menu transitions.

Areas touched:

- Keep active menu state consistent when `next-menu` opens another menu.
- Preserve cursor/menu references during refresh paths.
- Add PlayerMoveEvent fallback for cursor updates when packet rotation updates
  are missed.
- Cancel the known "Cannot interact with self" kick while a cursor menu is
  active.
- Keep pig/camera based fixed-view behavior.
- Call WASD/NPC menu switch hooks where the runtime changes menu content.

Main files:

```text
src-decompiled/com/cmenu/ui/CursorMenuPlugin.java
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
src-decompiled/com/cmenu/ui/wasd/WASDModule.java
src-decompiled/com/cmenu/ui/npc/NPCModule.java
```

## Menu/config examples added

Source menu examples:

```text
src-decompiled/menu/login.yml
src-decompiled/menu/login_menu.yml
src-decompiled/menu/mixed_elements_demo.yml
src-decompiled/menu/oc_agreement.yml
src-decompiled/menu/oc_agreement_accepted.yml
src-decompiled/menu/oc_connecting.yml
src-decompiled/menu/oc_friends.yml
src-decompiled/menu/oc_friends_alex.yml
src-decompiled/menu/oc_friends_mira.yml
src-decompiled/menu/oc_friends_rin.yml
src-decompiled/menu/oc_login.yml
src-decompiled/menu/oc_main_menu.yml
src-decompiled/menu/oc_profile_select.yml
src-decompiled/menu/oc_profile_select_mira.yml
src-decompiled/menu/oc_profile_select_reynar.yml
src-decompiled/menu/oc_register.yml
src-decompiled/menu/oc_settings.yml
src-decompiled/menu/oc_social_discord.yml
src-decompiled/menu/oc_social_site.yml
src-decompiled/menu/oc_social_vk.yml
src-decompiled/menu/permission_style_example.yml
src-decompiled/menu/link_example.yml
```

Server-data menu examples:

```text
server-data-current/menu/login.yml
server-data-current/menu/login_menu.yml
server-data-current/menu/mixed_elements_demo.yml
server-data-current/menu/oc_agreement.yml
server-data-current/menu/oc_agreement_accepted.yml
server-data-current/menu/oc_connecting.yml
server-data-current/menu/oc_friends.yml
server-data-current/menu/oc_friends_alex.yml
server-data-current/menu/oc_friends_mira.yml
server-data-current/menu/oc_friends_rin.yml
server-data-current/menu/oc_login.yml
server-data-current/menu/oc_main_menu.yml
server-data-current/menu/oc_profile_select.yml
server-data-current/menu/oc_profile_select_mira.yml
server-data-current/menu/oc_profile_select_reynar.yml
server-data-current/menu/oc_register.yml
server-data-current/menu/oc_settings.yml
server-data-current/menu/oc_social_discord.yml
server-data-current/menu/oc_social_site.yml
server-data-current/menu/oc_social_vk.yml
server-data-current/menu/orven_help.yml
server-data-current/menu/orven_hub.yml
server-data-current/menu/orven_permission_style.yml
server-data-current/menu/orven_profile.yml
server-data-current/menu/orven_rewards.yml
server-data-current/menu/orven_travel.yml
```

Main demo areas:

- Permission style and denied-click behavior.
- Hover style, hover cursor, hover tooltip, pressed style.
- MiniMessage messages and links.
- Login/register menu configs using existing auth.
- Agreement menu with toggle content.
- OrvenCraft profile/settings/friends/social placeholder flow.
- Mixed item/text elements.

## Documentation added

```text
docs/API_USAGE.md
docs/CustomScreenMenu_GitBook.html
docs/DEV12_CHANGE_LIST.md
UPSTREAM_HANDOFF.md
ORVENCRAFT_TZ_TEST_MENU_NOTES.md
```

`docs/CustomScreenMenu_GitBook.html` is a standalone GitBook-like HTML file
with:

- English / Chinese language switch.
- Light / dark theme switch.
- Command reference.
- Menu authoring guide.
- Positioning / cursor / hitbox explanation.
- Style and hover examples.
- Input field examples.
- Menu command examples.
- Items and text examples.
- WASD and NPC mirror notes.
- Runtime API examples.

## Packaged jars produced

```text
dist/CustomScreenMenu-1.4.8-dev2.jar
dist/CustomScreenMenu-1.4.8-dev3.jar
dist/CustomScreenMenu-1.4.8-dev4.jar
dist/CustomScreenMenu-1.4.8-dev5.jar
dist/CustomScreenMenu-1.4.8-dev6.jar
dist/CustomScreenMenu-1.4.8-dev7.jar
dist/CustomScreenMenu-1.4.8-dev8.jar
dist/CustomScreenMenu-1.4.8-dev9.jar
dist/CustomScreenMenu-1.4.8-dev10.jar
dist/CustomScreenMenu-1.4.8-dev11.jar
dist/CustomScreenMenu-1.4.8-dev12.jar
dist/CustomScreenMenu-1.4.8-login-input.jar
dist/CustomScreenMenu-1.4.8-hover-styles-minimessage.jar
```

Latest dev12 SHA256:

```text
Calculated after packaging; see build handoff/final response.
```

## Files currently changed after HEAD

Modified tracked files:

```text
src-decompiled/com/cmenu/ui/Commands.java
src-decompiled/com/cmenu/ui/CursorMenuPlaceholder.java
src-decompiled/com/cmenu/ui/CursorMenuPlugin.java
src-decompiled/com/cmenu/ui/MenuListener.java
src-decompiled/com/cmenu/ui/TextDisplayHitBox.java
src-decompiled/com/cmenu/ui/layout/MenuLayout.java
src-decompiled/com/cmenu/ui/section/MenuConfigLoader.java
src-decompiled/com/cmenu/ui/section/Section.java
src-decompiled/com/cmenu/ui/util/CommandUtils.java
src-decompiled/plugin.yml
```

New untracked source/API/docs/menu files are listed in the sections above.

## Known notes / limitations

- Auth was intentionally kept. Login/register/password systems were not
  removed in this workspace.
- Runtime API mutations are in-memory only. `/cmenu reload` reloads YAML and
  discards runtime changes.
- OP players still pass ordinary permission checks because that is existing
  plugin behavior.
- Smooth hover transition is not a full animation system; it mainly targets
  color.
- Existing `[op]` / `[console]` command behavior was not security-hardened.
- Some original upstream text/messages in the decompiled resources still have
  encoding issues; the new documentation files were written cleanly.
- This is not a clean upstream source tree. The developer should port the
  ideas/patches into the original source layout.
