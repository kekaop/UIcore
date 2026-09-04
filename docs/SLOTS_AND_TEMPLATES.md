# Slots And Templates

Slots let a menu reserve named positions while another system chooses which prepared element template occupies each position. UIcore only renders the assignment; random selection and game rules remain outside the plugin.

```yaml
perk_menu:
  slots:
    first:
      default-template: common_speed
    second:
      default-template: empty
  templates:
    common_speed:
      name: "Speed"
      x: -0.8
      y: 0
      z: 3.5
    empty:
      name: ""
      x: 0
      y: 0
      z: 3.5
```

Commands:

```text
/uicore slot set <player> <menu> <slot> <template>
/uicore slot clear|reset <player> <menu> <slot>
/uicore slot refresh <player> <menu>
/uicore slot set-global <menu> <slot> <template>
```

Player-scoped assignments affect one viewer. Global assignments affect every viewer until reload. The same operations are available from `UIcoreApi` through `setSlot`, `clearSlot`, and `resetSlot`.
