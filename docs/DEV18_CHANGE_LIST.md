# UIcore 1.4.8-dev18 changes

> Historical changelog. It does not describe the current supported runtime or certify testing. See [Runtime and API contracts](RUNTIME_CONTRACTS.md) and [audit fixes](AUDIT_FIXES.md).


This file lists the local dev18 additions that should be easy to port into the
original source tree.

## Added: toggle-style

`toggle-style` changes the rendered text style depending on the current
`toggle-content` state.

Use quoted `'off'` and `'on'` keys in YAML. Bukkit/YAML can otherwise parse
unquoted `off`/`on` as boolean keys.

```yaml
toggle-content:
  enabled: true
  'off': "[ ] Enabled"
  'on': "[x] Enabled"

toggle-style:
  'off':
    color: "#ff6b6b"
    strikethrough: true
  'on':
    color: "#62e67a"
    bold: true
```

Style priority:

```text
style -> permission-style.allowed -> toggle-style -> hover-style -> pressed-style -> permission-style.denied
```

## Added: per-layout cooldown

`cooldown` blocks accepted clicks for a configured duration per player and per
cooldown key. Denied clicks can send a message, actionbar, and commands.

```yaml
cooldown:
  key: reward_button
  time: 5s
  denied:
    actionbar: "<red>Wait {cooldown.left}s"
    message: "<gray>This button is still on cooldown."
    commands:
      - "[message] <gray>Cooldown denied command"
```

Supported duration forms:

```text
500ms
5s
2m
20ticks
ticks: 20
seconds: 5
```

Denied placeholders:

```text
{cooldown.left}
%cooldown_left%
{cooldown.left_ticks}
%cooldown_left_ticks%
```

## Added: bar-text layout

`type: bar-text` renders a text progress bar from placeholder/numeric values.
It does not create a separate image or resource pack asset.

```yaml
type: bar-text
value: "%player_health%"
max: "20"
segments: 20
filled: "<green>|"
empty: "<dark_gray>|"
prefix: "<red>HP "
suffix: " <gray>{value}/{max} ({percent}%)"
```

Supported placeholders in the generated text:

```text
{value}
{max}
{percent}
{filled}
{empty}
```

## Demo menu

Added:

```text
src-decompiled/menu/dev18_features.yml
server-data-current/menu/dev18_features.yml
```

Open with:

```text
/uicore run dev18_features
```

The demo includes:

- toggle text with on/off style;
- cooldown button with denied actionbar/message;
- health `bar-text`;
- back/close buttons.
