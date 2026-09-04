# UIcore Smoke Checklist

Run on Paper 26.1.2 with Java 25 and PacketEvents installed.

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
