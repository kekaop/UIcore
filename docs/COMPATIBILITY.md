# UIcore compatibility

> **Language:** English | [Русская версия](COMPATIBILITY_RU.md)

UIcore is built with Java 17 bytecode against the Paper 1.21.1 API. Its manifest declares `api-version: '1.21'` and `folia-supported: true`. It supports Minecraft 1.21.1 and later on Paper, Folia, Purpur, and Leaf when the server project provides a build for that Minecraft version. PacketEvents 2.14.0 is required.

| Server | Minecraft | Tested build | Server Java | Result |
| --- | --- | --- | --- | --- |
| Paper | 1.21.11 | 132 | 21 | startup, reload, shutdown |
| Folia | 1.21.11 | 14 | 21 | startup, reload, shutdown; a test player opened and closed `demo_main` |
| Paper | 26.2 | 129 | 25 | startup, reload, shutdown |
| Folia | 26.2 | 7 beta | 25 | startup, reload, shutdown |
| Purpur | 26.2 | 2633 | 25 | startup, reload, shutdown |
| Leaf | 26.2 | 123 experimental | 25 | startup, reload, shutdown |

The matrix records local checks with one UIcore 1.0.0 build and PacketEvents 2.14.0. The final JAR was not retested on every listed combination. Earlier checks completed `/uicore reload` and a clean server shutdown without UIcore errors, `NoSuchMethodError`, or `NoClassDefFoundError` in the log. Visual behavior, all interactions, and optional integrations still require the [smoke checklist](SMOKE_CHECKLIST.md).

Java 17 is the plugin's bytecode baseline, not a universal server runtime. Run each Minecraft version on the Java version required by its server build. On Folia, keep the camera and scene elements inside the same region; see the [Folia port status](PORT_26_2.md).
