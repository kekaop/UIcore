# CustomScreenMenu local changes for upstream

This folder contains a local patch based on the current server jar. The changes are small and focused on menu text styling, hover feedback, and MiniMessage chat output.

## Added

- `style` for normal text display appearance.
- `hover-style` for text display appearance while the cursor is inside the button hitbox.
- `pressed-style` for short click feedback after a button is clicked.
- `permission-style.allowed` and `permission-style.denied` for permission-aware button visuals.
- `hover-cursor` for changing the visible cursor item while hovering a button.
- `hover-tooltip` for actionbar or chat tooltip text on hover.
- `permission-denied` for denied click feedback: message, actionbar, sound, commands, and deny/ignore mode.
- MiniMessage support for `[message]` and `[actionbar]` commands.
- MiniMessage support for `[link]url|display text`, while keeping the open-url click event.
- A ready-to-test permission style example menu:
  - `server-data-current/menu/orven_permission_style.yml`
  - `src-decompiled/menu/permission_style_example.yml`

## Style priority

The current priority is:

1. `style`
2. `permission-style.allowed`
3. `hover-style`
4. `pressed-style`
5. `permission-style.denied`

This means denied permission visuals win over hover and pressed visuals, so locked buttons stay visually locked.

## Example YAML

```yaml
layout:
  profile:
    permission: orven.profile.open
    name: "&aProfile"
    x: -1.45
    y: 0.2
    z: 3.5
    text-size: 1.0

    style:
      color: "#62e67a"

    hover-style:
      color: "#ffffff"
      underlined: true
      transition:
        type: smooth
        duration: 6

    pressed-style:
      color: "#2ecc71"
      bold: true
      duration: 3

    permission-style:
      denied:
        color: "#777777"
        strikethrough: true

    hover-tooltip:
      type: actionbar
      text: "<green>Open profile"

    hover-cursor:
      material: EMERALD
      custom-model-data: 0
      scale: 1.15
```

## Notes

- Smooth transitions are implemented for color only. Formatting and text changes switch immediately.
- `hover-tooltip.type` supports `actionbar` by default, plus `message`/`chat`.
- `hover-cursor.material` uses Bukkit material names.
- `permission-denied.commands` run only when the player clicks without the required button permission. Normal click commands, links, next-menu, and stop-menu do not run in that case.
- Old `hover-enlarge`, `hover-sound`, `hover-commands`, and `leave-commands` continue to work.
- Existing command behavior is not hardened in this patch. `[op]` and `[console]` are still supported because changing that would break compatibility.

## Files changed

- `src-decompiled/com/cmenu/ui/style/TextStyleConfig.java`
- `src-decompiled/com/cmenu/ui/layout/MenuLayout.java`
- `src-decompiled/com/cmenu/ui/HoverEnlargeManager.java`
- `src-decompiled/com/cmenu/ui/util/CommandUtils.java`
- `src-decompiled/com/cmenu/ui/CursorMenuPlugin.java`
- `src-decompiled/com/cmenu/ui/TextDisplayManager.java`
- Demo menu configs under `server-data-current/menu/` and `src-decompiled/menu/`
