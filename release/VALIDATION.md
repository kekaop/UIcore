# UIcore 1.1.0 release verification

> **Language:** English | [Русская версия](VALIDATION_RU.md)

Verification ran on November 22, 2026. The release JAR was built on Windows with JDK 25 using `mvn -B -ntp clean verify`.

| Check | Result |
| --- | --- |
| Maven tests | 205 passed |
| Paper 26.1.2 build 72 and Folia 26.1.2 build 8 | Nine interaction scenarios per server with PacketEvents 2.14.0 |
| Cutscene protocol checks | 40 clicks received on each server in the passing run, plus hover, frame, camera, and game mode checks |
| JAR inspection | Plugin and default config version 1.1.0, Java 17 bytecode, six demo menus, a bundled cutscene, and PacketEvents as a required dependency |

The first Folia cutscene run received 9 of 10 clicks in its initial sequence. The rerun received all 40. Folia logged cleanup warnings when the first run stopped with the menu still open. The passing runs closed their menus before shutdown. The protocol checks send packets directly. Physical hold-to-drag input and cutscene smoothness on a connected client still need gameplay checks for this artifact.

- [Compatibility matrix](https://github.com/kekaop/UIcore/wiki/Compatibility)

`UIcore-1.1.0.jar` · 832,904 bytes

```text
SHA-256: d63acc638871c6bccc8d98913dfe07b00549a93b68f3626247190b3e342f9d35
```
