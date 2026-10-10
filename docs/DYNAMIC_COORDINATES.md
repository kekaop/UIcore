# Dynamic menu coordinates

Element and slot coordinates are offsets in the menu camera basis: `x` is camera-right, `y` is camera-up, and `z` is forward. Each axis accepts a number or a numeric expression. Dynamic expressions are evaluated while the menu is open; the clock starts again for each menu session.

## Clock, operators, and functions

Available variables are `t` (elapsed seconds), `ticks` (elapsed ticks), `pi`, and `e`. Operators are `+`, `-`, `*`, `/`, `%`, unary `+`/`-`, and parentheses. Supported functions are:

| Function | Arguments | Example |
|---|---:|---|
| `sin`, `cos`, `abs` | 1 | `sin(t * 2)` |
| `min`, `max` | 2 | `max(%player_level%, 1)` |
| `clamp`, `normalize`, `lerp` | 3 | `clamp(t, 0, 3)` |
| `map` | 5 | `map(%player_level%, 1, 100, -1, 1)` |

Expressions can read a PlaceholderAPI value such as `%player_level%` or a menu data value such as `{state.page}`. These values must resolve to a finite number. Missing, non-numeric, infinite, or otherwise invalid results keep the previous coordinate (or fallback before the first successful update) and are logged. They are never evaluated as arbitrary Java or scripting code.

```yaml
moving_title:
  name: "<aqua>Time-driven movement"
  x: "0.9 * sin(t * 1.4)"
  y: 0.25
  z: 3.5
  position-update:
    interval: 1
    smoothing: 0.18
    fallback:
      x: 0
    clamp:
      x: [-1.0, 1.0]
```

## Update period, smoothing, fallback, and clamp

| Setting | Default | Meaning |
|---|---:|---|
| `position-update.interval` or `interval-ticks` | `1` tick | Update period, clamped to 1–1200 ticks |
| `position-update.smoothing` | `1.0` | Interpolation step from 0.01 to 1.0; `1.0` applies the target directly |
| `position-update.fallback.x/y/z` | Static `x/y/z` value | Coordinate to use before the first valid result and after a failed expression |
| `position-update.clamp.x/y/z` | No bounds | Two-number `[min, max]` range applied to the result |

An invalid expression is reported during configuration loading and the static axis value is used. A runtime failure leaves the last valid position. Clamp applies to static and expression values. Smoothing applies to movement from the previous position toward the computed result.

For a slot, the slot owns its position and the selected template supplies its display settings. A slot can specify `position-update` values too; those values override the template's corresponding position-update options.

Copy the example above into your own menu to try dynamic coordinates. The bundled `demo_main`, `demo_gallery`, and `demo_states` examples use static positions.
