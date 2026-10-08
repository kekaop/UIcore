# UIcore Runtime API

Full GitBook-style documentation is available in `docs/UIcore_GitBook.html`.

This API is for other plugins that need to open menus or mutate loaded menus at runtime. Call it on the Bukkit main thread. See [runtime contracts](RUNTIME_CONTRACTS.md) for session, input, and snapshot guarantees.

The service is registered through Bukkit Services:

```java
UIcoreApi api = UIcoreProvider.get();
if (api != null) {
    api.openMenu(player, "demo_main");
}
```

Direct access is also available from the plugin instance:

```java
UIcorePlugin plugin = JavaPlugin.getPlugin(UIcorePlugin.class);
UIcoreApi api = plugin.getMenuApi();
```

## Mutation Scope

There are two mutation scopes:

- Global calls do not take a `Player`. They mutate the loaded in-memory menu and affect every current/future viewer until `/uicore reload`.
- Player calls take a `Player`. They create an in-memory overlay for that player only and refresh only that player's active menu.

```java
// Global: visible to everyone using demo_main.
api.addElement("demo_main", element);

// Player-scoped: visible only to this player.
api.addElement(player, "demo_main", element);
```

Use player-scoped calls for profile screens, personal rewards, per-player buttons, temporary notices, and menu state that should not leak to other players.

## Open And Close

```java
api.openMenu(player, "demo_main");
api.closeMenu(player);
api.closeMenu(player, false); // close without teleporting back to the saved entry location
```

Useful read methods:

```java
boolean open = api.isMenuOpen(player);
Optional<String> current = api.getOpenMenu(player);
Set<String> menus = api.getMenuIds();
List<String> elements = api.getElementIds("demo_main");
```

## Add Element

```java
MenuMutationResult result = api.addElement(
    "demo_main",
    MenuElementSpec.builder("api_button")
        .text("&aAPI button")
        .position(0.0, -0.4, 3.5)
        .textSize(0.45)
        .commands(List.of("[player] say clicked from API"))
        .hoverEnlarge(true, 1.2)
        .build()
);
```

If the menu is currently open for players, active viewers are refreshed automatically.

Player-only add:

```java
api.addElement(player, "demo_main",
    MenuElementSpec.builder("personal_reward")
        .text("<gold>Your reward")
        .position(0.0, -0.65, 3.5)
        .commands(List.of("[message] <green>Only you can see this."))
        .build()
);
```

## Edit Element

```java
api.editElement("demo_main", "api_button", edit -> edit
    .text("&eUpdated text")
    .position(0.2, -0.4, 3.5)
    .set("pressed-style.text", "&aPressed")
    .set("pressed-style.duration", 5)
);
```

The editor starts from the element's loaded YAML values, so unspecified settings are preserved.

Player-only edit:

```java
api.editElement(player, "demo_main", "title", edit -> edit
    .text("<aqua>Hello, " + player.getName())
);
```

## Input Fields

Any menu element can focus a chat input field:

```yaml
layout:
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

You can also attach input focus to a normal button:

```yaml
input-field: nickname
```

When the player types in chat while a menu input field is focused, the chat message is cancelled, stored as menu input, and the current menu is refreshed.

Supported placeholders:

```text
%uicore_input_nickname%
%input_nickname%
{input.nickname}
{menu.input.nickname}
```

Inside commands these placeholders use the raw stored value. In menu text, password-like fields are masked unless password visibility is enabled.

API access:

```java
api.focusInputField(player, "nickname");
api.setInput(player, "nickname", "Orven");
String nickname = api.getInput(player, "nickname").orElse("");
Map<String, String> allInputs = api.getInputs(player);
api.clearInput(player, "nickname");
api.clearInputs(player);
```

Command example:

```yaml
command:
  - "[player] say My nickname is {input.nickname}"
  - "[console] lp user %player_name% meta set nickname {input.nickname}"
```

## Player Menu State

Menu state is player-scoped and menu-scoped. It is intended for dynamic UI
conditions such as pages, tabs, selected profiles, temporary flags, and
feature toggles.

```java
api.setState(player, "demo_main", "page", "2");
String page = api.getState(player, "demo_main", "page").orElse("1");
Map<String, String> state = api.getState(player, "demo_main");
api.clearState(player, "demo_main", "page");
api.clearState(player, "demo_main");
```

Changing state through the API refreshes that player's active menu if the
changed menu is currently open.

## Conditions

Elements can be hidden with `visible-if`. Hidden elements are not rendered and
cannot receive hover or click.

```yaml
world_only_button:
  name: "<green>World button"
  x: 0.0
  y: 0.2
  z: 3.5
  visible-if:
    placeholder: "%player_world%"
    equals: world
```

State shorthand:

```yaml
page_two_title:
  name: "<yellow>Page 2"
  x: 0.0
  y: 0.4
  z: 3.5
  visible-if:
    state.page: 2
```

Composed conditions:

```yaml
admin_world_button:
  name: "<aqua>Admin world action"
  visible-if:
    all:
      - permission: "example.admin"
      - placeholder: "%player_world%"
        equals: "world"
      - state:
          page: 2
```

Supported comparisons for placeholder conditions: `equals`, `not-equals`,
`contains`, `not-contains`, `matches`, `greater-than`, `greater-or-equal`,
`less-than`, `less-or-equal`, or the same values through `operator` plus
`value`.

## Animation

The new `animation` block is a clearer alias over the existing per-display
animation task system. It works on TextDisplay menu elements.

```yaml
animated_title:
  name: "<gold>Animated"
  x: 0.0
  y: 0.5
  z: 3.5
  animation:
    type: bounce
    speed: 1.2
    amplitude: 0.12
```

Supported animation types:

- `fade`
- `move` with optional `axis: x` or `axis: y`
- `rotate`
- `scale`
- `bounce`

The old `floating-animation` block remains compatible.

## Remove Element

```java
api.removeElement("demo_main", "api_button");
```

Player-only remove or clear:

```java
api.removeElement(player, "demo_main", "personal_reward");
api.clearPlayerElements(player, "demo_main");
api.clearPlayerElements(player);
```

## Positioning And Cursor

Menus use a fixed camera scene. `camera-position` places the camera, and each layout element is placed from that scene:

- `x` moves the element left/right.
- `y` moves the element up/down.
- `z` moves the element forward from the camera.
- `camera-position.distance` controls the cursor plane distance.
- `hitbox.width`, `hitbox.height`, and `hitbox.depth` control click detection.

Current test menus are tuned around `camera-position.distance: 1.5` and element `z: 3.5`.

```yaml
demo_main:
  camera-position:
    world: world
    x: -187.0
    y: 100.0
    z: 347.0
    yaw: 0.0
    pitch: 0.0
    distance: 1.5
  layout:
    start:
      name: "<green>Start"
      x: 0.0
      y: 0.1
      z: 3.5
      hitbox:
        width: 0.9
        height: 0.28
        depth: 0.25
```

Global cursor tuning lives in `config.yml`:

```yaml
cursor-item:
  material: ARROW
  custom-model-data: 0
  scale: 0.5
  sensitivity: 1.0
  smoothing: 1.0
  max-x: 2
  max-y: 3
  movement-range:
    enabled: false
```

## WASD Navigation

WASD is an optional selection mode. It tracks the currently selected menu element, moves selection with `W/A/S/D`, and executes the selected element with `Space`.

```yaml
demo_main:
  wasd-enabled: true
```

```yaml
# wasd_config.yml
enabled: true
navigation:
  horizontal-threshold: 0.5
  dot-product-threshold: 0.5
  selection-cooldown: 500
menu-settings:
  enable-for-all-menus: false
  use-whitelist-mode: true
  enabled-menus:
    - demo_main
```

Useful placeholders:

```text
%cmp_wasd_menu%
%cmp_wasd_enabled%
%cmp_wasd_index%
%cmp_wasd_x%
%cmp_wasd_y%
%cmp_wasd_z%
```

Helper calls:

```java
if (WASDModule.isModuleEnabled() && WASDModule.isMenuEnabled("demo_main")) {
    String menu = WASDModule.getPlayerCurrentMenu(player);
    Location selected = WASDModule.getPlayerCurrentLocation(player);
}
```

## NPC Mirror

NPC mirror is an optional visual companion feature. It uses FancyNpcs for the mirror entity and can optionally use SkinsRestorer for skin sync.

```yaml
# npc_config.yml
enabled: true
offset:
  x: 2.0
  y: 0.0
  z: -4.0
sync:
  equipment: true
  skin: true
menu-settings:
  enable-for-all-menus: false
  use-whitelist-mode: true
  enabled-menus:
    - demo_main
```

Lower-level lifecycle hooks are normally called by the menu runtime:

```java
if (NPCModule.isModuleEnabled()) {
    NPCModule.onMenuOpen(player, cameraLocation, yaw, pitch, "demo_main");
    NPCModule.onMenuSwitch(player, newCameraLocation, newYaw, newPitch);
    NPCModule.onMenuClose(player);
}
```

## Event API

The plugin fires Bukkit events for menu lifecycle and interaction:

- `MenuOpenEvent`
- `MenuCloseEvent`
- `MenuSwitchEvent`
- `MenuHoverEvent`
- `MenuClickEvent`
- `MenuInputEvent`

```java
@EventHandler
public void onMenuClick(MenuClickEvent event) {
    if ("danger_button".equals(event.getElementId())) {
        event.setCancelled(true);
    }
}

@EventHandler
public void onMenuInput(MenuInputEvent event) {
    if ("nickname".equals(event.getField())) {
        event.setValue(event.getValue().trim());
    }
}
```

`MenuClickEvent` and `MenuInputEvent` are cancellable. Cancelling click prevents
the element command from running. Cancelling input prevents the chat value from
being stored.

## Notes

- Runtime mutations change the loaded in-memory menu only.
- `/uicore reload` reloads menu YAML files from disk and discards runtime mutations.
- API calls should be made from normal server/plugin logic, not from arbitrary async threads.
- For persistent changes, edit the YAML file and run `/uicore reload`.
- `refreshMenu(menuId)` can be used after external state changes to redraw active viewers.
