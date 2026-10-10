# UIcore Folia port status

> **Language:** English | [Русская версия](PORT_26_2_RU.md)

UIcore is built with Java 17 bytecode against the Paper 1.21.1 API. Newer server builds may require a newer Java runtime; the tested 26.2 server used Java 25. The plugin manifest declares `folia-supported: true`.

An experimental Folia 26.2 beta build 7 loaded UIcore with PacketEvents 2.14.0 and completed startup, `/uicore reload`, and shutdown. This does not replace an in-game smoke test on the exact server build you plan to use.

Folia schedules UIcore work on the player's or entity's region. Keep camera-relative elements, NPCs, and schematic previews inside the camera region. Large scenes that cross a region boundary need a smaller layout or a different camera position.

For the broader test record, see the [compatibility matrix](COMPATIBILITY.md). For scenarios that still need verification, see the [smoke checklist](SMOKE_CHECKLIST.md).
