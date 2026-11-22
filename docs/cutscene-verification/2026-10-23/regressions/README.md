# Verify the 26.1.2 interaction and camera fixes

This run addresses missed left clicks, missing hover during movement, the menu trailing the camera, and the hand/crosshair appearing in menus. It uses real disposable Paper and Folia servers with a headless Minecraft 26.1.2 protocol client. It does not run or render Fabric.

`results.json` records the tested JAR's SHA-256 and each assertion. `paper.log` and `folia.log` contain server builds, packet targets, click events, hover transitions, playback events, restoration, and shutdown. `automated-tests.json` records the Maven package result for the same JAR.

The environments use Paper 26.1.2 build 72 (`1a6b910`), Folia 26.1.2 build 8 (`62dc0f2`), PacketEvents 2.14.0, and Oracle JDK 25.0.2. Existing server installations were not modified. Both disposable servers bind to localhost and stop after the run.

The assertions cover:

- 20 clicks while translating and rotating, including moving the cursor to a second button and back.
- Five clicks while paused and five after moving the mouse on the paused menu.
- Ten clicks in a stationary menu.
- Alternating `SPECTATE_ENTITY` targets between the virtual Interaction receiver and physical camera carriers. Vanilla 26.1.2 can pick either; both must become menu input.
- Hover entry and exit while the cutscene is `PLAYING`, and red hover text metadata reaching the client before pausing.
- Camera and both text displays moving in the same bundle, with consistent local coordinates and one-tick position/rotation interpolation.
- No destruction of the virtual camera during playback and no repeated spawns of retained menu visuals.
- Relative zero-angle player corrections, so echoed correction packets cannot become mouse movement.
- Camera restoration, actual game-mode restoration, and removal of virtual entities after close.

The scene crosses yaw `179° → −179°`, changes pitch `0° → 30°`, and travels from `(0, 100, 0)` to `(40, 101, 40)` over 160 server ticks. The harness sends real incoming click and look packets. It does not activate buttons through the plugin API. The probe API is used for reporting and closing sessions.

## Repeat the run

Prepare the disposable servers in `target/cutscene-server-smoke` and `target/cutscene-folia-smoke` with the versions above, accepted test-server EULAs, offline test authentication, and ports 25586/25587. Install the current UIcore JAR and compile `CutsceneProbe.java` against UIcore, Paper, and PacketEvents 2.14.0. Package the compiled probe with the `plugin.yml` included in this directory and install it on both test servers.

Copy `regressions.cjs` into `target/cutscene-client`, install `minecraft-protocol` and its Minecraft data dependency there, then run:

```powershell
node target/cutscene-client/regressions.cjs
```

The script writes its scene/menu fixtures only into the disposable servers. It creates a fresh test player, sets the test worlds to peaceful, starts and stops the servers, and writes `regression-results.json`. Its JDK path is specific to this Windows workspace.

The automated run checks packet delivery and geometry. A Fabric client still needs to confirm rendered smoothness, hand/crosshair visibility, and mouse targeting. A confirmed independent Folia region transfer, deliberate network lag, and reduced-TPS visual behavior remain in the [gameplay checklist](../../../CUTSCENE_GAMEPLAY_CHECKS.md).
