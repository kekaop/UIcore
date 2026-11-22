# Groups, drag and drop, and zoom in UIcore 1.1.0

This snapshot build adds `parent`, `type: group`, drag handles, sliders, drop targets and `/uicore debug`. The block-digging input is experimental: server/protocol tests do not establish real vanilla mouse behavior or visual compatibility. Existing menus retain spectator input unless they opt in.

## Parent and groups

`parent: element_id` makes x/y/z local offsets. A group has no display or hitbox. Moving its coordinates moves descendants and their hitboxes. Without a parent, coordinates retain their existing camera/world meaning. Parent and child must use the same anchor. Missing parents, cycles and duplicate layout/slot IDs reject a loaded menu. An absent or hidden runtime parent hides its subtree. Rotation, scale and visual floating/entrance animation are not inherited; coordinate expressions are inherited.

## Input

- `drag-input: click` (default): click a draggable handle to capture it, click again to place it. This is explicitly a toggle gesture, not hold-to-drag.
- `drag-input: block-digging`: experimental hold input. The client sees an invisible, unbreakable barrier two blocks in front of the camera. Start/cancel digging packets control capture and release. The server stays in the protected menu mode and the real block stays intact. Use an empty main hand. Clients from 26.1 receive a Creative view with instant mining disabled, which hides the experience bar. Older clients use a Survival view. This mode disables edge pan and cannot start a cutscene. Losing the block target may end a drag even with LMB held.

In `block-digging` mode, Minecraft draws its first-person crosshair even when the experience bar is hidden. Press **F1** to hide the vanilla HUD during manual testing. Press **F1** again after closing the menu to restore it. For a menu that hides the crosshair automatically, use `drag-input: click` with the spectator view.

Closing, switching menus or leaving restores the real block display and clears the gesture. A forged digging packet for another block cannot move UI. Input queues are bounded. Moving, hidden, replaced, or inaccessible handles cancel their gesture. Permissions and click requirements are checked at capture and while dragging.

## Element settings

```yaml
parent: window
pointer-events: true
interaction-priority: 10
drag:
  enabled: true
  target: window # optional: self or an ancestor
  axis: xy       # x, y, xy
  bounds:
    x: [-1.5, 1.5]
    y: [-0.7, 0.7]
  drop-targets: [socket] # optional; without a valid drop, restore the old position
```

Coordinates and bounds describe the moved target in its parent's coordinate system. Depth remains fixed. Dragging does not write YAML or affect other players. Coordinates persist for this menu visit; reopening resets them. A dragged target's coordinate formula is overridden until the visit ends. World-anchored dragging is not supported. A drag does not execute the handle's ordinary click commands. Menu input uses priority first and distance second; `pointer-events: false` excludes decorations from both hover and click picking. Groups cannot themselves be clicked: give them a visible handle.

A receiving element or slot may set `drop-accepts: [gem, card]`. A listed drop target with no acceptance list accepts any dragged target. Drop snaps x/y to the receiver, preserving depth. A snap violating the handle axis or bounds is rejected. Empty slots with `drop-accepts` or a nonempty `on-drop` retain an invisible hitbox. Slot position, parent and interaction options belong to the slot. Extensions can grant items or assign slot templates after validating the game's rules.

## Actions after a drop

Set `on-drop` on the drag handle, the receiving element or slot, or both. The final position and any slider value are stored before UIcore emits `MenuDropEvent` and runs the handle's actions followed by the receiver's actions. A free drop runs only the handle's actions. An invalid or vetoed drop runs neither list. If a callback closes or switches the menu, UIcore skips the remaining actions for that session.

For example, on a receiving slot:

```yaml
drop-accepts: [gem]
on-drop:
  - '[state] socket_filled true'
  - '[actionbar] <green>Diamond placed'
  # Optional: run a Java action registered by an extension.
  # - '[action] puzzles:place_gem'
```

`on-drop` uses the existing menu action syntax, including `[state]`, `[element-state]`, commands and registered `[action]` handlers. An `[action]` receives the element or slot containing the action as `context.elementId()`, and arguments `drag-handle`, `drag-target` and, when applicable, `drop-target`. Delayed actions keep this context and require the same menu session. A confirmed drop starts the handle's and receiver's configured cooldowns. A failed action does not undo the stored drop.

## Sliders

```yaml
slider: {state: volume, min: 0, max: 100, step: 5}
drag: {axis: x, bounds: {x: [-0.8, 0.8]}}
```

A slider is an ordinary text/item handle. It requires one axis with finite bounds. Movement maps to a clamped, stepped number stored in `{state.volume}`. Cancellation restores the previous state value. Bounds constrain the handle position; the visual handle moves continuously while the value is quantized.

## Mouse wheel zoom

Enable zoom at the menu level. Omit `group` to zoom all camera-relative elements. Set it to a `type: group` element to zoom only that group and its descendants.

```yaml
zoom:
  enabled: true
  group: board       # optional
  min: 0.6
  max: 1.8
  step: 0.2
  sensitivity: 1
  smoothing: 0.45   # 1 applies each step immediately
  pivot: {x: 0, y: 0}
```

The pivot is relative to the selected group's origin or the menu origin for full-menu zoom. Coordinates, text, items and hitboxes scale together while element depth stays fixed. Default bounds are `0.5–2.0`. Accepted bounds are within `0.25–4.0` and must include `1.0`. Each menu visit starts at `1.0`. Zoom pauses during a capture so drag bounds, sliders and drops use stable geometry.

UIcore uses the existing spectator camera without opening a screen or asking for a mod or resource pack. The player's Adventure info allows a vanilla client to send held-slot changes. UIcore consumes adjacent changes, including wraparound, and leaves the server inventory selected slot unchanged. Jumping to a non-adjacent hotbar slot does not zoom. An adjacent number key is indistinguishable from the wheel at protocol level. UIcore resynchronizes the real selected slot on close or switch. Zoom requires `drag-input: click`.

Try `/uicore run demo_zoom`. Scroll over the menu, then click the diamond to pick it up and click again to place it.

## API and debugging

`MenuElementEditor.parent`, `draggable`, `pointerEvents`, `interactionPriority` and existing `set` expose these settings. `MenuDragEvent` reports START, MOVE, DROP and CANCEL on the player's owning scheduler, with handle ID, moved target ID, local coordinates and optional drop target ID. Cancelling START rejects capture; cancelling MOVE or DROP rolls back. CANCEL is a notification. Listen to the non-cancellable `MenuDropEvent` for the confirmed result and final coordinates. Both events run on the player's owning scheduler.

`/uicore debug` toggles a player-only overlay (`uicore.debug`, OP by default). Particles outline hitbox geometry and link children to parents; the action bar shows the picked ID and parent. Only that player receives the overlay. Use `/uicore run demo_drag` for the bundled example. To compare input modes, copy it and change `drag-input` to `click`.

Validation results and reproducible headless checks: [verification](interaction-verification/README.md).
