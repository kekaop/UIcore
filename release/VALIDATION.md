# UIcore 1.0.0 release check — October 10, 2026

> **Language:** English | [Русская версия](VALIDATION_RU.md)

This pass reviewed menu sessions, scheduler boundaries, click and chat input, permissions, extension data, optional integrations, configuration, examples, documentation, and the built JAR. It supplements the [compatibility matrix](../docs/COMPATIBILITY.md); it does not replace connected-player testing.

## Verified

| Check                                                  | Result                                                                          |
| ------------------------------------------------------ | ------------------------------------------------------------------------------- |
| `mvn -B -ntp clean verify`, Windows, JDK 21 and JDK 25 | Passed on both JDKs                                                             |
| Latest JDK 21 test suite                               | 139 tests, zero failures, errors, or skipped tests                              |
| Plugin descriptor                                      | Version `1.0.0`, API `1.21`, Folia support declared, PacketEvents required      |
| Plugin/API bytecode                                    | Java 17; bundled RE2/J uses older compatible bytecode                           |
| Bundled menus                                          | Only `demo_main`, `demo_gallery`, and `demo_states`                             |
| Default cursor depth                                   | `z-offset: 1.0`                                                                 |
| Removed subsystems                                     | No WASD, pumpkin overlay, or experimental drag classes in the JAR               |
| Package contents                                       | API and third-party license texts included; no Markdown documentation or videos |

Documentation check: 39 Markdown files and 176 local file links, no missing targets. Tags in the Russian BBCode description are balanced. Remote links were not retested in this pass.

The regression tests cover command cancellation after a session changes, teleport destination/request matching, malformed access settings, and caching of empty or failed data results. The remaining drag implementation and its obsolete documentation were removed. Camera geometry and hitbox calculations were left as requested.

Final artifact: `UIcore-1.0.0.jar`, 644,292 bytes, built with JDK 21 on October 10, 2026.

```text
SHA-256: b3f33639427588fd3e90cd31e8de8f4b1d2a64a0ba5ec553f198c4bb41bcd696
```

## Checks still required

- Run the [smoke checklist](../docs/SMOKE_CHECKLIST.md) with this exact JAR: all three demos, hover/click/input, transitions, close, reconnect, reload, and restoration of the player's original state.
- Check visibility with at least two players for all three privacy flags, including menu switches, joins, respawns, world changes, and another plugin's vanish. Check player/projectile damage with `can-damaged` both enabled and disabled.
- Check minimum Minecraft 1.21.1 and the intended release server, including Folia region boundaries and schematic restoration. Earlier startup results are not a game test of this artifact.
- Exercise Vault, LuckPerms, PlaceholderAPI, FancyNpcs, SkinsRestorer, and CraftEngine with the actual installed versions.
- Run the build on Linux and measure sustained multi-player load. This pass ran on Windows and did not benchmark player counts or tick time.

## Remaining engineering limits

- Avatar profile loads have no plugin-defined timeout or cache size limit. NPC skin retrieval also relies on the provider completing its request. Slow or failing skin services need further resource-limit testing and hardening.
- Menu definitions are reparsed on some rendering paths, and journal/schematic file operations can block their caller. Their effect under load has not been measured.
- NoChatReports can disrupt signed chat in the reported client setup. The [compatibility notes](../docs/COMPATIBILITY.md#nochatreports-can-break-chat-input) describe the confirmed workaround and the unresolved trigger.

The build and regression suite pass. Release approval still depends on the game checks above; this report does not claim the plugin is free of bugs.
