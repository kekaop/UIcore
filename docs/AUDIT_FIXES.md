# UIcore technical audit fixes

> **Language:** English | [Русская версия](AUDIT_FIXES_RU.md)

This report records code fixes and automated checks completed for the September 29, 2026 source build. It does not confirm an in-game smoke test, production performance, or compatibility with optional plugins.

## Fixed areas

| Area | Result |
| --- | --- |
| Camera math and lifecycle | Non-finite rotations are rejected, player state is saved before side effects, and close, quit, death, mount loss, failed open, and disable use a common cleanup path. |
| Input and interaction | Hit testing reads only the current player's displays. Chat, command, and API input share one event path; password input is masked in displays and denied-command logs. |
| Menu configuration | YAML loads strictly, reload runs a preflight check, and an invalid source file is never replaced by empty defaults. |
| Actions and conditions | Action type is identified before input substitution. Console and operator commands remain controlled by security settings and an exact allowlist. |
| Rendering and tasks | Typewriter, temporary text, slots, NPCs, and old session tasks are cleaned up through their generation or session token. |
| API safety | Public snapshots are deeply immutable, while editing works on independent mutable copies. |
| Optional integrations | CraftEngine uses the public Adventure resolver API. Personal NPCs and skin lookups remain player-scoped and return to the server thread before applying changes. |
| Removed subsystems | WASD navigation and the pumpkin overlay were removed from the current UIcore version. |

## Checks recorded for that build

- `mvn -B -ntp clean verify` completed successfully on JDK 21.
- 75 automated tests completed with no failures, errors, or skips.
- The final JAR was checked for relocated RE2/J classes, `MenuSession`, and the resource manifest.
- `git diff --check` found no whitespace errors.

The current branch targets Minecraft 1.21.1 and newer. Check supported server combinations in the [compatibility matrix](COMPATIBILITY.md), then complete the [smoke checklist](SMOKE_CHECKLIST.md) before using a release on a production server.

## Remaining checks

Test two players in one scene, cancelled teleports and game-mode changes, scene switches while mounting, death and restart recovery, disable with an active scene, and optional NPC, skin, and CraftEngine integrations. No 1/10/50 viewer load measurement is recorded.

Old block-cache data contains no block-entity NBT or inventory. It cannot restore lost container contents; use a world backup. Do not edit configuration files while `/uicore reload` is running because reload cannot make simultaneous external writes transactional.
