# Cursor movement range notes

This describes how the current cursor area works in the patched test build.

## Config keys

```yaml
cursor-item:
  max-x: 2
  max-y: 3
  sensitivity: 1.0
  x: 0.0
  y: 0.0
  movement-range:
    enabled: true
    x:
      min: -2.0
      max: 2.0
    y:
      min: -3.0
      max: 3.0
```

## Coordinate model

The cursor is positioned on the menu camera plane, not in screen pixels.

- X is horizontal offset on the camera right vector.
- Y is vertical offset on the camera up vector.
- Values are world-space block offsets from the menu camera basis.
- Element `layout.x` and `layout.y` use the same world-space plane idea.

## Rotation to cursor offset

The code compares the player's current rotation to the fixed menu camera
rotation:

```text
yawDelta   = playerYaw   - cameraYaw
pitchDelta = playerPitch - cameraPitch
```

Then it converts degrees into world-space cursor offsets:

```text
cursorX = yawDelta   / (90 / max-x) * sensitivity + cursor-item.x
cursorY = pitchDelta / (90 / max-y) * sensitivity + cursor-item.y
```

With the default values:

```text
max-x: 2
max-y: 3
sensitivity: 1.0
```

This means:

```text
90 degrees yaw   = 2 blocks horizontally
90 degrees pitch = 3 blocks vertically
```

So the cursor area before hard clamping is controlled by `max-x`, `max-y`,
and `sensitivity`.

## Hard movement range clamp

If `cursor-item.movement-range.enabled` is true, the plugin clamps the final
cursor offsets:

```text
cursorX = clamp(cursorX, movement-range.x.min, movement-range.x.max)
cursorY = clamp(cursorY, movement-range.y.min, movement-range.y.max)
```

The hard cursor zone size is:

```text
width  = movement-range.x.max - movement-range.x.min
height = movement-range.y.max - movement-range.y.min
```

Example:

```yaml
movement-range:
  enabled: true
  x:
    min: -2.0
    max: 2.0
  y:
    min: -3.0
    max: 3.0
```

The cursor can move inside a 4 x 6 block rectangle on the menu plane.

## Current live test1 config

The live test config currently has:

```yaml
movement-range:
  enabled: false
  x:
    min: -9.0
    max: 9.0
  y:
    min: -9.0
    max: 9.0
```

Because `enabled` is false, these min/max values are not applied. The cursor
movement is effectively limited by the player's rotation clamp and by
`max-x`, `max-y`, and `sensitivity`.

If this is enabled as-is, the hard cursor zone becomes 18 x 18 blocks.

## Practical tuning

- Increase `max-x` if the cursor feels too narrow horizontally.
- Increase `max-y` if the cursor feels too narrow vertically.
- Increase `sensitivity` if the cursor should move farther for the same mouse
  movement.
- Enable `movement-range` only when you need a strict rectangular border.
- For a menu whose buttons are around x `-2..2` and y `-1..1`, a clamp around
  x `-2.5..2.5` and y `-1.5..1.5` is usually enough.
