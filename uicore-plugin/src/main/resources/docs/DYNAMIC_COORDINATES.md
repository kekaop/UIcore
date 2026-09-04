# Dynamic Coordinates

Element `x`, `y`, and `z` values may be numbers or expressions. Expressions are evaluated while the menu is active and produce world-space coordinates relative to the menu camera basis.

```yaml
position-update:
  interval-ticks: 1
  smoothing: 0.2

layout:
  moving_title:
    name: "Moving title"
    x: "0.25 * t"
    y: "sin(t * 0.1) * 0.2"
    z: 3.5
```

Available values include menu time (`t`, in ticks), constants, arithmetic, supported math functions, and PlaceholderAPI values through the external-value syntax documented in `UIcore_GitBook.html`. `min`/`max` clamps are available for external values.

`smoothing: 1.0` applies the computed position immediately. Lower positive values interpolate toward it. Invalid expressions log a warning and use the static coordinate.

Open the bundled example with `/uicore run demo_dynamic_position`.
