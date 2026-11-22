# Interaction verification — UIcore 1.1.0

The test clients have no GUI and do not capture or move the operating-system cursor. No windows were opened on VG272. All servers are disposable localhost test instances, not production servers.

## Automated coverage

- Maven clean verify: 205 tests passed, including nested coordinates, invalid graphs, runtime orphan handling, slider math, and nested slot configuration.
- Paper and Folia 26.1.2 with PacketEvents 2.14.0: `results.json` records the SHA256 of the tested plugin and each scenario.
- The interaction probe checks capture/movement/release, group and rendered-child coordinates, cancelled-drop rollback, block/camera cleanup, populated-slot sliders, axis and bounds, absent-state restoration, empty receiving slots, rejected drops, event-listener mutations, revoked permissions, same-camera menu switches and parent removal followed by an unrelated API addition.
- The camera carrier is hidden from the player in block-digging mode, preventing it from intercepting the client ray.
- The passing [cutscene regression run](cutscene-regression-results.json) covers 40 clicks on each server, moving/paused/static menus, hover, frame consistency, and restoration of camera and game mode. See [release verification](../../release/VALIDATION.md) for the full result and the first Folia attempt.

## What this does not prove

The probe writes UI cursor coordinates on the player's scheduler and sends protocol digging packets. It does **not** establish that a vanilla client actually sends the required packets in response to physical mouse input. Real-client reach, ray selection, release/target-loss behavior, hand/HUD, particles and sound still need gameplay verification. No independent Folia-region boundary crossing is claimed. Inventory grants and automatic slot-template transfers are outside the visual drop operation.

The plugin remains a snapshot, and `block-digging` is experimental. Existing menus use the unchanged spectator camera with explicit click-toggle dragging unless opted into `drag-input: block-digging`.

## Repeating the checks

1. Run `mvn -B -ntp clean verify` with JDK 21+ (this run used JDK 25.0.2).
2. Compile `InteractionProbe.java` against the built UIcore JAR, Paper and its dependency classpath, then package it with a descriptor declaring main `smoke.InteractionProbe`, dependency `UIcore`, `folia-supported: true`, and the `interactionprobe` command (OP-only permission).
3. Put that probe at `target/interaction-probe/InteractionProbe.jar`.
4. Prepare the disposable servers under `target/cutscene-server-smoke` and `target/cutscene-folia-smoke`, bound to `127.0.0.1:25586` and `:25587`; install PacketEvents 2.14.0. The harness copies the current UIcore and probe JARs and its own test menus into them.
5. Use the existing `minecraft-protocol` / `minecraft-data` installation in `target/cutscene-client/node_modules`, then run `node docs/interaction-verification/verify.cjs`.

The runner uses the recorded local JDK path and shuts both servers down after testing. Preserve the artifact hash when interpreting logs; test results for an older hash are not evidence for a later build.

Set `UICORE_INTERACTION_LOG_DIR` to keep new logs separate from the recorded results in this directory. The updated probe also checks that a committed drop sees the stored coordinates, then runs YAML state and Java extension actions once, with no corresponding event or actions on rejection or veto.

The zoom scenario sends held-slot packets and verifies client mode packets, wrapped wheel steps, an ignored non-adjacent selection, the zoomed item and hitbox, drag/drop at the new scale, and restoration of the real selected slot. The protocol client neither renders the HUD nor scrolls a physical wheel.
