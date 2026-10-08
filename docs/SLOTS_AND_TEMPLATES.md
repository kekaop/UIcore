# Menu templates and slots

A **template** is a complete prepared menu element. It is not rendered on its own. A **slot** is a named display position that selects one template at runtime. UIcore renders the selected element; another plugin or server logic decides which content is appropriate for the player.

```yaml
reward_menu:
  camera-position:
    type: player
    distance: 1.5
  templates:
    bronze:
      type: item
      material: IRON_SWORD
      transform: gui
      scale: 0.7
      hitbox: { width: 0.9, height: 0.9, depth: 0.4 }
    diamond:
      type: item
      material: DIAMOND_SWORD
      transform: gui
      scale: 0.7
  slots:
    first_reward:
      x: -0.6
      y: 0.2
      z: 3.5
      content: bronze
      fallback: bronze
    second_reward:
      x: 0.6
      y: 0.2
      z: 3.5
      content: "{state.second_reward}"
      fallback: bronze
```

Each slot requires no `default-template` field: its initial selection comes from `content`. The optional `fallback` is used if the resolved template ID is empty or does not exist. Slot `x/y/z` set the rendered element's position; the template supplies type, text or item model, hitbox, style, actions, and other display settings. A slot can set `position-update` settings; they override the same settings from its template. Dynamic slot coordinates may use numeric expressions too.

`content` supports a literal template ID, a PlaceholderAPI expression such as `%someplugin_reward%`, or UIcore data such as `{state.reward}`. Placeholder expansion happens for the viewer. An unresolved template uses a valid fallback or leaves the slot empty. Templates must be declared under the same menu's `templates` section.

## Runtime assignment and reset

```text
/uicore slot set <player> <menu> <slot> <template>
/uicore slot clear <player> <menu> <slot>
/uicore slot reset <player> <menu> <slot>
/uicore slot refresh <player> <menu>
/uicore slot set-global <menu> <slot> <template>
/uicore slot clear-global <menu> <slot>
/uicore slot reset-global <menu> <slot>
```

Player-scoped assignment affects that player's menu. It takes precedence over a global assignment, which takes precedence over YAML `content`. `clear` explicitly assigns an empty result, so the configured content is not used. `reset` removes the player's assignment and restores the next available source. Global assignments apply to all viewers until cleared, reset, or the plugin reloads. These commands require `uicore.slot`; they can also be invoked through `UIcoreApi` methods.

`refresh` rebuilds the target player's active menu so updated external placeholders or data become visible. UIcore does not implement random rolls, reward rules, or a game-specific carousel pool; the calling plugin supplies those decisions.

## Animate a slot transition

```text
/uicore slot transition <player> <menu> <slot> <template> <direction> <ticks> [distance] [easing]
```

The transition animates the slot's current item away and assigns the new template. Directions are `top`, `bottom`, `left`, `right`, `front`, and `back`. Duration is in ticks; optional distance and easing configure the outgoing movement. The replacement currently appears at its target position without a coordinated incoming animation, even when the template declares `entrance-animation`. Account for this behavior when designing a carousel. The command updates the selected ItemDisplay and its hitbox.

The menu author's examples are `demo_slots` and `item_carousel`. The comprehensive author guide also documents interaction with coordinates, hitboxes, and display transforms.
