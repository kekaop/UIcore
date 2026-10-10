# UIcore runtime and API contracts

> **Language:** English | [Русская версия ключевых ограничений](COMPATIBILITY_RU.md)

This source build compiles against Paper 1.21.1 with Java 17 bytecode (`release 17`). The manifest declares `api-version: '1.21'` and `folia-supported: true`. Local startup/reload/shutdown results across Paper, Folia, Purpur, and Leaf are in the [compatibility matrix](COMPATIBILITY.md). Visual and interaction checks are still needed. See [Folia port status](PORT_26_2.md).

| Dependency | Compile target | Runtime role |
| --- | --- | --- |
| PacketEvents | 2.14.0 | Required. UIcore registers and removes its own listeners; PacketEvents owns its lifecycle. |
| PlaceholderAPI | 2.12.3 | Optional. Unavailable or unresolved condition placeholders deny access. |
| FancyNpcs | 2.8.0 | Optional. Personal NPCs are shown only to their viewer. |
| SkinsRestorer API | 15.8.2 | Optional. Skin lookup runs asynchronously; application returns to the server thread. |
| CraftEngine | Capability adapter | Optional. Resolver discovery uses public Adventure interfaces. Real server compatibility requires a smoke check. |
| RE2/J | 1.8, shaded and relocated | Bundled regex implementation. Patterns are static and compiled when loading the element. |

Call the Bukkit service API on the player's scheduler thread (the server main thread on Paper-derived servers). Menu opening starts a transition; an accepted request does not mean that the camera mount has already activated. Input submission uses the same cancellable `MenuInputEvent` for chat, commands, and the API. Snapshots are deeply immutable; editing uses independent mutable copies.

A menu visit borrows the player's game mode, invisibility, collision setting, and location. The origin snapshot is saved before side effects and restored through the common close path. Scene switches keep this original snapshot. Delayed actions belong to a session generation and cannot affect a newer visit. A failed return or a death before respawn retains the recovery journal.

Camera block clearing sends client block changes only. It never changes live blocks. Recovery of old blockcache files restores only air or acknowledges an already matching block; conflicts and unavailable worlds remain in their journals. Old files stored BlockData only and cannot recover inventory or block-entity NBT.

Input values enter display and message components as literal text. Password fields are masked for display, remain available as raw values to configured actions, and are omitted from denied-command logs. `[op]` never grants temporary operator status. Console/op actions remain controlled by `security` settings and exact command allowlist boundaries.

The Markdown documentation in this repository is the maintained reference. Documentation is kept outside the plugin JAR so the runtime package stays focused on the server code. Historical dev changelogs are not a support matrix. Actual validation for this correction is recorded in [audit fixes](AUDIT_FIXES.md); the [smoke checklist](SMOKE_CHECKLIST.md) lists server scenarios that still need execution.
