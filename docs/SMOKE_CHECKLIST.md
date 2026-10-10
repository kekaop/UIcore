# UIcore Smoke Checklist

Run on each target Minecraft/server combination with its required Java version and PacketEvents 2.14.0 installed. The combinations in the [compatibility matrix](COMPATIBILITY_RU.md) passed local startup, `/uicore reload`, and shutdown checks. The detailed visual and interaction checks below remain pending.

1. Start the server and confirm UIcore enables without errors.
2. Run `/uicore reload` and verify menus reload once.
3. Open `demo_main`; verify camera lock, cursor movement, hover, and both clicks.
4. Switch to another menu; verify no blank transition and no stale cursor.
5. Test hover, pressed, permission, tooltip, and hover-cursor styles.
6. Toggle an element twice; verify text/style state and on/off commands.
7. Focus an input field; verify chat is cancelled and the value appears in the menu.
8. Verify `visible-if`, cooldown text, item displays, and animations.
9. Test `/uicore slot` and player-scoped API mutations.
10. Close the menu and quit; verify camera, entities, metadata, and tasks are cleaned up.

Also test quoted YAML keys `"on"`/`"off"` and legacy boolean-parsed equivalents.

## Audit regression checks

These are pending server checks, not a record of passed integration tests.

- Open/switch/close from each game mode with nondefault invisibility/collision. Cancel the camera and return teleports; verify retained recovery journals.
- Switch to a different camera position, reopen during mounting, die before activation, lose the mount, disconnect, kick, disable, and restart before respawn.
- Use two players in one scene; verify independent clicks, cooldowns, hover, personal NPCs, and API overrides.
- Repeat typewriter, slots, and temporary text groups; compare entities/tasks/tickets with baseline after each close.
- Keep a foreign hologram, force-loaded chunk, and PacketEvents listener active through UIcore disable.
- Test damaged YAML reload without rewriting the file; check invalid material/world/numbers and recovery journal conflicts.
- Enter MiniMessage tags, RGB codes, Unicode, and a password. Check literal display, raw configured action arguments, and absence of arguments in denied logs.
- Run FancyNpcs/SkinsRestorer/CraftEngine both present and absent, then profile 1/10/50 viewers and unrelated displays.
