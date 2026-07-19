# Smoke Checklist

Use this checklist on `test1` after installing the built JAR.

## Basic

- Start server on Java 25 with Paper 26.1.2.
- Confirm PacketEvents loads before CustomScreenMenu.
- Confirm CustomScreenMenu enables without dependency or plugin.yml warnings.
- Run `/cmenu reload`.

## Menu Runtime

- `/cmenu run demo_main`
- Confirm fixed camera is stable.
- Confirm cursor is visible and moves.
- Confirm left click works.
- Confirm right click does not disconnect the player.
- Confirm hover enlarge/style works.
- Confirm pressed-style duration is visible.
- Confirm menu switch keeps cursor movement and clicks.
- Confirm no blank-screen flicker is worse than dev21 baseline.
- Confirm `/cmenu stop` restores player state.
- Quit while menu is open and confirm cleanup.

## Feature Demos

- `demo_styles`: hover style, permission style, cooldown.
- `demo_toggle_input`: toggle-content, toggle-style, input field, pressed text.
- `demo_items_api`: TextDisplay + ItemDisplay mixed menu and custom-model-data.
- `demo_conditions`: visible-if and condition examples.
- `login` / `login_menu`: password input is captured from chat, cancelled, masked in feedback, and not logged by the plugin.

## API

- Install a test plugin against `org.orvencraft.customscreenmenu:customscreenmenu-api:1.4.8-orven.1-SNAPSHOT`.
- Resolve API through Bukkit `ServicesManager`.
- Test open/close, player-scoped add/edit/remove element, input read/write, state, refresh, and events.

## Security

- Confirm `[op]` commands do not grant temporary OP.
- Confirm `[console]` commands are denied by default.
- If console commands are needed, enable `security.allow-console-commands` and configure `security.console-command-allowlist`.
