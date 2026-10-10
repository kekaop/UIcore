# UIcore compatibility

> **Language:** English | [Русская версия](COMPATIBILITY_RU.md)

UIcore is built with Java 17 bytecode against the Paper 1.21.1 API. Its manifest declares `api-version: '1.21'` and `folia-supported: true`. It supports Minecraft 1.21.1 and later on Paper, Folia, Purpur, and Leaf when the server project provides a build for that Minecraft version. PacketEvents 2.14.0 is required.

| Server | Minecraft | Tested build     | Server Java | Result                                                                 |
| ------ | --------- | ---------------- | ----------- | ---------------------------------------------------------------------- |
| Paper  | 1.21.11   | 132              | 21          | startup, reload, shutdown                                              |
| Folia  | 1.21.11   | 14               | 21          | startup, reload, shutdown; a test player opened and closed `demo_main` |
| Paper  | 26.2      | 129              | 25          | startup, reload, shutdown                                              |
| Folia  | 26.2      | 7 beta           | 25          | startup, reload, shutdown                                              |
| Purpur | 26.2      | 2633             | 25          | startup, reload, shutdown                                              |
| Leaf   | 26.2      | 123 experimental | 25          | startup, reload, shutdown                                              |

The matrix records local checks with one UIcore 1.0.0 build and PacketEvents 2.14.0. The final JAR was not retested on every listed combination. Earlier checks completed `/uicore reload` and a clean server shutdown without UIcore errors, `NoSuchMethodError`, or `NoClassDefFoundError` in the log. Visual behavior, all interactions, and optional integrations still require the [smoke checklist](SMOKE_CHECKLIST.md).

Java 17 is the plugin's bytecode baseline, not a universal server runtime. Run each Minecraft version on the Java version required by its server build. On Folia, keep the camera and scene elements inside the same region; see the [Folia port status](PORT_26_2.md).

## NoChatReports can break chat input

If chat input stops working with **NoChatReports** installed, disable the mod and fully restart the client, then reconnect. Test ordinary chat before opening a menu, input in a menu field, and ordinary chat after closing the menu.

On October 10, 2026, a player reported this problem with UIcore 1.0.0, Paper 26.1.2 build 72, PacketEvents 2.14.0, and NoChatReports Fabric 26.1-v2.19.0. The client used `defaultSigningMode: ON_DEMAND`; the server used `online-mode=true` and `enforce-secure-profile=true`. The player confirmed that chat and menu input worked after disabling the mod. This records one affected setup, not a failure on every NoChatReports version or configuration.

The server may log:

```text
Failed to update secure chat state for <player>: 'Chat disabled due to missing profile public key. Please try reconnecting.'
Failed to update secure chat state for <player>: 'Chat had an invalid signature. Please try reconnecting.'
Failed to update secure chat state for <player>: 'Chat disabled due to broken chain. Please try reconnecting.'
```

### Why this can happen

NoChatReports changes chat signing. Its `ON_DEMAND` mode initially sends unsigned messages. When the server rejects a message because its profile key is missing, the mod enables signing and resends the last message. This behavior is documented in the mod's [signing modes](https://github.com/Aizistral-Studios/No-Chat-Reports/blob/26.1-Unified/src/main/java/com/aizistral/nochatreports/common/core/SigningMode.java) and implemented in its [chat warning handler](https://github.com/Aizistral-Studios/No-Chat-Reports/blob/26.1-Unified/src/main/java/com/aizistral/nochatreports/common/mixins/client/MixinChatListener.java) and [signing initialization](https://github.com/Aizistral-Studios/No-Chat-Reports/blob/26.1-Unified/src/main/java/com/aizistral/nochatreports/common/core/ServerSafetyState.java).

That explains the missing-key warning. If a later message fails signature validation, the server marks its signed-message chain as broken and rejects subsequent messages. Closing a UIcore menu doesn't restore that chain. UIcore receives menu text through Bukkit's chat event, after the server validates the incoming message; it doesn't remove chat signatures or cancel chat-session packets.

The exact trigger for the invalid signature in this setup hasn't been isolated. Opening and closing a menu without entering text also passed with the mod enabled, so the failure was intermittent. Disabling NoChatReports and restarting the client is the verified workaround. Using its `ALWAYS` signing mode is an alternative to investigate, but hasn't been verified for this setup.
