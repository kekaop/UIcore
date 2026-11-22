# Move a menu camera with a cutscene

> **Language:** English | [Русский](CUTSCENES_RU.md)

Cutscenes move the camera of an open UIcore menu. Each player has a separate session, cursor, progress, and choice. Coordinates describe the camera's **eye position**, in blocks, in one world. The plugin calculates the player and camera entity offsets. Cutscenes do not edit world blocks and do not require a client mod.

Use the **left mouse button** to activate elements. Menus use a viewer-local display camera and a client-only spectator appearance to hide the hand and crosshair. The server keeps the menu session's game mode and restores the original player state on close. Camera, cursor, and menu movement use one bundled frame with matching interpolation; input and hover use the last committed frame while the next frame moves.

## Run the supplied example

1. Review `plugins/UIcore/cutscenes/demo.yml`. Replace `world` and the example eye positions with a suitable area on your server. The supplied route starts at `(0, 100, 0)`.
2. Run `/uicore cutscene validate`, then `/uicore cutscene reload`.
3. Open `/uicore run demo_cutscene`.
4. Keep the mouse still during the flight and turn. Use **Test moving click** to check that the visible button receives the click. When the status requests a choice, select **Left route** or **Right route**.
5. Use **Close** to close the menu and restore the player's original location.

The example includes a smooth route with a custom speed curve, a short `179° → −179°` rotation, a turn in place, a timed pause, and two branches. Its `initial` start mode builds the menu at the first point. Completion leaves the menu open at the final viewpoint. This explicit initial placement can take the player far from the place where the menu was opened.

The [gameplay verification record](CUTSCENE_GAMEPLAY_CHECKS.md) distinguishes automated checks from checks that still need a Paper or Folia server and a connected client. Existing compatibility results for stationary menus do not verify cutscene smoothness.

## Attach a separately stored scenario to a menu

Put one scenario in each `plugins/UIcore/cutscenes/<id>.yml` file. The top-level `id` identifies the scenario. A menu refers to it by ID:

```yaml
my_menu:
  camera-position: { type: player, distance: 1.5 }
  cutscene: { id: tour, auto-start: true }
  layout:
    start:
      name: "Start tour"
      x: 0
      y: 0
      z: 3.5
      command: ["[cutscene] start tour"]
```

Autostart runs after the menu becomes active. Set `auto-start: false` to start through an element action or Java API. Preview and API starts require an active menu.

Camera-attached elements use `position-space: camera`, the default. Their `x`, `y`, and `z` remain local to the moving camera. Explicit `position-space: world` keeps an element at its absolute coordinates in the menu world. Dynamic element coordinates and UI animations are evaluated before the camera transform. The cursor, hover test, and click test use the same camera transform as the displayed menu.

## Define eye positions and movement

This complete scenario moves from the current camera to a point, waits, then finishes:

```yaml
id: tour
start: approach
policy:
  start-mode: transition
  transition-ticks: 20
  completion: keep-open
points:
  arrival: { world: world, x: 12.5, y: 75.62, z: -4.0, yaw: 179.0, pitch: 0.0 }
steps:
  approach:
    type: move
    from: current
    to: arrival
    duration: 80
    path: linear
    speed: ease-in-out
    rotation: { mode: interpolate, curve: ease-in-out }
    next: hold
  hold: { type: wait, duration: 40, next: done }
  done: { type: end }
```

All durations are server ticks: 20 ticks last one second at 20 TPS. Playback slows with the server when TPS drops; it does not jump ahead to catch up with wall-clock time.

Use `/uicore cutscene point save tour arrival` while standing at the desired viewpoint. The command records the player's current eye location, yaw, and pitch, preserving other scenario fields. It writes through an atomic file replacement and leaves active playback unchanged. Saving a point into a new file creates a draft; add `start` and `steps` before validating and reloading it. IDs accepted by the point commands contain 1–80 letters, numbers, underscores, or hyphens.

Point editing commands use `cutscenes/<scene>.yml`; its declared `id` must match `<scene>`. The runtime loader also accepts other file names and `.yaml` files, which you can edit directly.

| Move field                        | Meaning                                                                      |
| --------------------------------- | ---------------------------------------------------------------------------- |
| `from`                            | `current` (also the default) or a named point.                               |
| `to`                              | One point ID or an ordered list, such as `[bend, arrival]`.                  |
| `duration`                        | Positive number of ticks for the whole move.                                 |
| `path`                            | `linear` for straight segments, or `smooth` for a spline through the points. |
| `tangent-before`, `tangent-after` | Optional point IDs controlling the incoming/outgoing spline tangent.         |
| `speed`                           | Built-in or configured speed curve ID.                                       |
| `rotation`                        | Direction and a separate rotation speed curve.                               |
| `next`                            | Explicit ID of the next step.                                                |

Smooth paths are sampled by traveled distance, so `uniform` speed is independent of the spline's internal parameter. Endpoints are applied exactly. For a route that must pass through intermediate points without stopping, put those points in a single move. Use `from: current` on adjoining moves to keep the camera pose at their boundary. A named `from` that disagrees with the actual camera pose aborts instead of silently teleporting the camera.

To split a smooth `A → B → C` route at `B`, give the first `A → B` move `tangent-after: C`, and the second `B → C` move `tangent-before: A`. Also match the endpoint velocities: normalized curve endpoint velocity × route length ÷ duration. The tangent fields align direction; they do not automatically match speed. Equal route-length/duration ratios with `uniform` curves match speed.

`start-mode: initial` places an autostart menu at the first explicit start point before activation. A manual/API start in this mode requires the open menu's camera to already match that point; otherwise it fails preparation. `start-mode: transition` keeps the current menu camera and blends into an explicit start point over `transition-ticks`. Choose initial placement deliberately; use transition when the camera should visibly travel from an arbitrary open menu.

## Reuse curves for translation and rotation

Built-in curve IDs are `uniform`, `accelerate`, `decelerate`, and `ease-in-out`. Translation and rotation select curves independently. Add local curves to the scenario, or shared curves under `curves:` in `cutscenes/curves.yml`:

```yaml
curves:
  cruise:
    interpolation: smooth
    points:
      - { time: 0.0, speed: 0.0 }
      - { time: 0.2, speed: 1.0 }
      - { time: 0.8, speed: 1.0 }
      - { time: 1.0, speed: 0.0 }
```

`time` is normalized time in `[0, 1]`; the first and last points must be at `0` and `1`, and intermediate times must strictly increase. `speed` is a finite, nonnegative relative velocity. `interpolation: linear` connects velocity samples with lines; `smooth` connects them with smooth, nonnegative segments. The runtime integrates and normalizes velocity so the whole route is completed within the move duration. A zero-speed interval holds translation still. An all-zero curve is invalid.

For `rotation.curve`, these same samples represent relative **angular** velocity. They are integrated and normalized with the same semantics as translation; they do not directly specify an angle or positional easing value.

## Set the viewing direction independently

| `rotation.mode` | Behavior                                                                                                          |
| --------------- | ----------------------------------------------------------------------------------------------------------------- |
| `fixed`         | Retain the starting direction, or use explicit `yaw` and `pitch`.                                                 |
| `interpolate`   | Blend from the starting direction to the last route point's direction, with optional `yaw` and `pitch` overrides. |
| `path`          | Look along the route tangent.                                                                                     |
| `look-at`       | Look at the position of the point named by `target`.                                                              |

For a turn in place, move to a point with the same coordinates and different angles. `pitch` must stay within `−90..90`. Interpolation defaults to the shortest yaw path; `179 → −179` turns about two degrees. `direction` accepts `shortest`, `clockwise`, or `counterclockwise`; nonnegative integer `turns` adds intentional full revolutions. When a target coincides with the camera, the last valid direction is retained.

A move's initial viewing direction must match the previous camera direction. Before switching to `path`, `look-at`, or a different fixed direction, add an interpolated turn to align the view. A mismatched starting direction fails with a `steps.<id>.rotation` diagnostic instead of snapping.

## Pause or branch once

Every step has a unique ID. The four step types are `move`, `wait`, `choice`, and `end`. A timed wait uses `duration: 40`; an external wait uses `duration: external` and continues when `resume(player)` or the resume command is called. Manually pausing a move keeps its elapsed progress and does not rerun its entry actions when resumed.

```yaml
steps:
  pick:
    type: choice
    options:
      left: { element: left_button, next: left_route }
      right: { element: right_button, next: right_route }
    timeout: 200
    timeout-next: done
    fallback-next: done
  left_route: { type: wait, duration: 20, next: done }
  right_route: { type: wait, duration: 40, next: done }
  done: { type: end }
```

The `element` value identifies an element in the active menu. Clicking it submits that option automatically while this choice is active. Existing element visibility, permissions, and click requirements still apply and are checked again when accepting a choice. API calls use the option ID (`left`), rather than the element ID (`left_button`).

Acceptance is serialized on the player's scheduler. A duplicate click or an API request racing a timeout cannot enter two branches. `timeout-next` is required when a timeout is configured. `fallback-next` is taken when no options are available; without a fallback, the session aborts with `NO_AVAILABLE_CHOICE`.

## Run entry and exit actions once per visit

`enter` and `exit` lists run once on actual entry and departure from a step. Pauses and camera updates do not repeat them. A later loop that visits the same step runs them again.

```yaml
enter:
  - "[state] route choosing"
  - "[element-state] status waiting"
  - "[action] quests:show-routes"
  - "[message] Choose your route."
exit:
  - "[element-state] status moving"
```

`[state]` sets the active menu's player-scoped state key. `[element-state]` selects an existing element state, including states with `visible: false`. `[action] namespace:id` invokes a registered UIcore action with empty arguments and an empty element ID for the menu-level step. Other entries use existing UIcore command prefixes, including `[message]`, `[actionbar]`, `[player]`, and `[console]`, with their usual command security rules.

Menu element command lists can control playback with `[cutscene] start <id>`, `pause`, `resume`, `skip`, `stop`, or `choose <option>`. Registered actions and event listeners can also call the API.

## Set lifecycle and interaction policies

| `policy` field        | Default / behavior                                                                     |
| --------------------- | -------------------------------------------------------------------------------------- |
| `completion`          | `keep-open`; use `close-menu` for the existing menu close and player restoration flow. |
| `restart`             | `reject`; use `replace` to abort the old cutscene before accepting a new one.          |
| `allow-skip`          | `false`. When enabled, skip visits `skip-to`, or completes if it is omitted.           |
| `skip-to`             | Optional target step. Actions of skipped steps are not executed.                       |
| `cursor`, `clicks`    | `true`; control cursor input and clicks independently.                                 |
| `edge-pan`            | `false`; enable only if edge panning should be added to scripted camera movement.      |
| `freeze-ui`           | `false`; UI animations and data normally continue during pauses.                       |
| `chunk-timeout-ticks` | `200`; limit for waiting for route preparation.                                        |

Session states are `PREPARING`, `PLAYING`, `PAUSED`, `WAITING_CHOICE`, `COMPLETED`, and `ABORTED`. Closing or switching menus, player death or quit, plugin shutdown, or an external teleport aborts active playback. A preparation failure or timeout aborts with a specific reason. The menu session generation guards callbacks so work queued for an old menu cannot update a replacement session.

The cutscene owns an immutable configuration snapshot. `/uicore cutscene reload` replaces definitions for future starts while active playback keeps its snapshot. Validation reads configuration without changing active sessions or loaded definitions. `/uicore reload` also reloads the rest of UIcore; use the dedicated cutscene command when changing only scenarios.

Preparation is bounded to 4,096 chunks and 100,000 prepared route segments per scenario. Split larger routes into smaller scenarios. These limits produce a diagnostic during preparation rather than letting one start consume unbounded server resources.

## Use administrative commands

| Command                                                       | Permission                                      |
| ------------------------------------------------------------- | ----------------------------------------------- |
| `/uicore cutscene point save <scene> <point>`                 | `uicore.cutscene.edit`; player only.            |
| `/uicore cutscene point list <scene>`                         | `uicore.cutscene.edit`.                         |
| `/uicore cutscene validate`                                   | `uicore.cutscene.edit`.                         |
| `/uicore cutscene reload`                                     | `uicore.cutscene.edit`.                         |
| `/uicore cutscene preview <scene> [player]`                   | `uicore.cutscene.preview`; `start` is an alias. |
| `/uicore cutscene stop\|pause\|resume\|skip\|status [player]` | `uicore.cutscene.preview`.                      |

Both permissions default to operators and can be granted separately. The console must name a player for playback controls. `/ui` is an alias for `/uicore`. Preview needs an open menu; it uses the same playback path as API and menu starts.

Validation rejects missing points, curves, step transitions, invalid durations and angles, malformed choices, and immediate transition cycles that have no movement, wait, or input. Errors identify the scenario, step, and parameter path, for example `Cutscene 'tour', step 'approach', path 'steps.approach.to': ...`.

## Control playback through Java

```java
UIcoreApi api = UIcoreProvider.get();
CutsceneStartResult result = api.cutscenes().start(player, "tour");
// ACCEPTED means the request was accepted. Await CutsceneStartEvent for playback.
api.cutscenes().pause(player);
api.cutscenes().resume(player);
api.cutscenes().choose(player, "left");
api.cutscenes().state(player).ifPresent(snapshot -> {
    String step = snapshot.stepId();
    double progress = snapshot.stepProgress();
    long ticks = snapshot.elapsedTicks();
});
api.cutscenes().stop(player);
```

Types are in `org.orvencraft.uicore.api.cutscene`. `CutsceneStartResult` is `ACCEPTED`, `UNKNOWN_SCENE`, `NO_MENU`, or `ALREADY_RUNNING`. Calls dispatched from another scheduler return request acceptance, not a promise that a later choice or preparation will succeed. Read the immutable snapshot or listen to events for the result.

Events in `org.orvencraft.uicore.api.event` are `CutsceneStartEvent`, `CutsceneStepEnterEvent`, `CutscenePauseEvent`, `CutsceneResumeEvent`, `CutsceneChoiceEvent`, `CutsceneCompleteEvent`, and `CutsceneAbortEvent`. All expose the player, scenario ID, and snapshot; choice adds `getOptionId()`, and abort adds `getReason()` and `getDiagnostic()`. They run on the player's scheduler, which can be a Folia region thread. Schedule unrelated entity or world work on its own owning scheduler.

The snapshot exposes session ID, scenario ID, state, step ID, total elapsed ticks, step elapsed ticks, and normalized step progress. Preparation and manual pauses do not advance motion time.
