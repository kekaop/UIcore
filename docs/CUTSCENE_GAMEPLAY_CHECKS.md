# Verify cutscenes on Paper and Folia

The [26.1.2 regression run](cutscene-verification/2026-10-23/regressions/README.md) covers the later click, hover, camera tracking, and HUD fixes. It sends spectator left-click packets to both the invisible receiver and physical camera carriers, verifies hover entry/exit during playback and the corresponding text metadata, and identifies its own JAR hash. The original smoke run below used API choices and did not cover these regressions. Neither run renders Fabric; visual acceptance remains open.

Protocol smoke checks passed on Paper and Folia on October 23, 2026. Headless Minecraft protocol clients exercised real servers, player sessions, camera entities, choices, and restoration. Visual smoothness, rendered mouse interaction, and video recording remain pending.

The [machine-readable results](cutscene-verification/2026-10-23/results.json) identify each tested JAR by SHA-256. Both disposable servers stopped after the run. See the [1.1.0 release verification](../release/VALIDATION.md) for checks on the release JAR.

Automated geometry, curves, configuration, state transitions, and resource checks run through the Maven test suite. The command and resource tests also verify point persistence and the supplied choice element references. See the build output for the executed test count and result. Automated checks cannot prove client camera smoothness or the alignment of the rendered cursor with click targets.

## Record the environment before testing

Use a fresh copy of the built JAR on both server families. Start with the supplied `demo_cutscene` menu and `cutscenes/demo.yml`, replacing points with eye positions in a clear test area.

| Field                                    | Paper run                                           | Folia run                                          |
| ---------------------------------------- | --------------------------------------------------- | -------------------------------------------------- |
| Test date and tester                     | 2026-10-23, automated protocol harness              | 2026-10-23, automated protocol harness             |
| UIcore JAR version and SHA-256           | 1.0.0; see results JSON                             | 1.0.0; see results JSON                            |
| Server Minecraft version and exact build | Paper 1.21.11-132, c5eb079                          | Folia 26.1.2-8, 62dc0f2                            |
| Java version                             | Oracle JDK 25.0.2                                   | Oracle JDK 25.0.2                                  |
| PacketEvents version                     | 2.14.0                                              | 2.14.0                                             |
| Client version and installed mods        | minecraft-protocol 1.21.11; headless, no client mod | minecraft-protocol 26.1.2; headless, no client mod |
| Resource pack and optional plugins       | No resource pack; test-only CutsceneProbe           | No resource pack; test-only CutsceneProbe          |
| TPS during each case                     | Around 20; transcript includes TPS query            | Around 20; transcript includes region health query |
| Round-trip latency in milliseconds       | Localhost, 0–2 ms reported by server                | Localhost, 0–2 ms reported by server               |
| Video file or timestamped recording link | None; visual check pending                          | None; visual check pending                         |
| Server logs and profiler capture         | Linked transcript; no profiler capture              | Linked transcript; no profiler capture             |

The protocol run verified both branch endpoints `(2, 101, 12)` and `(10, 101, 12)`, and a separate 40-tick route ending at `(40, 100, 40)` across chunk boundaries. All measured eye coordinates matched the configured endpoints to eight decimal places. Local cursor X/Y stayed at zero without mouse input. Two competing API choices produced exactly one accepted choice. Paused progress stayed unchanged across a 1.3-second interval. Each close removed the camera and cursor references and restored the menu session. These checks passed on both server families.

The Folia route was processed in loaded regions that can merge. A transfer across a confirmed independent Folia region boundary has not been demonstrated.

Record TPS and latency per case, especially during deliberate server lag. Keep the rendered cursor and button under test visible in the video. Save changed scenario files with the recording so coordinates, durations, and curves can be reproduced.

## Run the acceptance cases

For each row, record **pass**, **fail**, or **blocked**, the recording timestamp, server log findings, and measured coordinates when applicable. Protocol evidence below covers server behavior; the visual acceptance checks remain open.

| Case                                  | Procedure and acceptance criteria                                                                                                                                                  | Paper                                                  | Folia                                                             |
| ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------ | ----------------------------------------------------------------- |
| Straight route                        | Move between two points with `uniform` speed. Finish after the configured ticks with endpoint error ≤ 0.0001 block.                                                                | Protocol pass: 40 ticks, exact endpoint                | Protocol pass: 40 ticks, exact endpoint                           |
| Smooth route                          | Use three or more points and `path: smooth`. Check uniform distance per tick away from deliberate easing and exact endpoint arrival.                                               | Not run                                                | Not run                                                           |
| Turn in place                         | Use identical position and different yaw/pitch. Coordinates stay fixed; the view turns smoothly.                                                                                   | Not run                                                | Not run                                                           |
| Angle wrap                            | Turn from yaw 179 to −179. Observe the short two-degree turn, with no full revolution.                                                                                             | Not run                                                | Not run                                                           |
| Simultaneous translation and rotation | Watch text, items, slots, composite elements, and custom hitboxes. Their local positions remain aligned throughout.                                                                | Not run                                                | Not run                                                           |
| Stationary mouse                      | Do not touch the mouse during scripted rotation. Cursor local coordinates remain fixed. Repeat after pause/resume and camera rebinding.                                            | Final local coordinates pass; visual pending           | Final local coordinates pass; visual pending                      |
| Moving mouse and clicking             | Use **Test moving click** during flight and rotation. The visually selected element receives each click. Test cursor/click policies and explicit edge pan.                         | Not run                                                | Not run                                                           |
| Custom curve                          | Add a zero-speed interval. Translation stops there, resumes, and still reaches the endpoint in the configured ticks. Reject an all-zero curve.                                     | Not run                                                | Not run                                                           |
| Pause mid-move                        | Use `/uicore cutscene pause`, inspect status twice, then resume. Progress stays fixed while paused and resumes without a jump or repeated actions.                                 | Protocol pass; visual pending                          | Protocol pass; visual pending                                     |
| External wait                         | Use `duration: external`; it stays fixed until resume. UI animations/data continue by default; repeat with `freeze-ui: true`.                                                      | Not run                                                | Not run                                                           |
| Branches                              | Run the supplied demo twice and select each route. Only that route's entry message and state change execute.                                                                       | Both API choices and endpoints pass                    | Both API choices and endpoints pass                               |
| Double click                          | Rapidly double-click one option and then the other. Exactly one choice event and branch execute.                                                                                   | Not run                                                | Not run                                                           |
| Choice against timeout                | Submit a choice through API in the timeout tick. Exactly one timeout or accepted option transition occurs. Repeat at least 20 times.                                               | Not run                                                | Not run                                                           |
| Availability changes                  | Remove permission or hide an option immediately before choosing through API. Recheck availability; deny the option. Hide both and verify fallback or `NO_AVAILABLE_CHOICE`.        | Not run                                                | Not run                                                           |
| Two players                           | Start two sessions. Pause, choose, and stop one while the other continues independently.                                                                                           | Not run                                                | Not run                                                           |
| Chunks and regions                    | Cross chunk boundaries and, on Folia, a real region boundary. Camera and interface stay present and synchronized; no foreign-region access appears in logs.                        | Chunk crossing protocol pass; visual pending           | Chunk crossing protocol pass; independent region crossing pending |
| Preparation timeout                   | Arrange unavailable route preparation. Motion pauses; timeout reports a specific reason and cleanup/restoration completes.                                                         | Not run                                                | Not run                                                           |
| Lifecycle cleanup                     | Separately test close, switch, quit, death, external teleport, and plugin disable. Tasks and temporary entities are removed; saved player restoration follows the menu close flow. | Close references/restoration pass; other cases pending | Close references/restoration pass; other cases pending            |
| Stale callbacks                       | Close/switch during preparation, then open a new menu. Delayed work from the old session cannot move or close the new one.                                                         | Not run                                                | Not run                                                           |
| Reload snapshot                       | Start a route, edit it, and use `/uicore cutscene reload`. Running motion keeps its old definition; a new start uses the edited definition.                                        | Not run                                                | Not run                                                           |
| Reduced TPS                           | Reduce TPS during movement and choice. Time slows in server ticks without skipped steps, repeated actions, or wall-clock catch-up.                                                 | Not run                                                | Not run                                                           |
| World integrity                       | Compare the route area before and after repeated start, stop, and failure. Cutscenes must not modify world blocks.                                                                 | Not run                                                | Not run                                                           |

## Capture and resolve failures

Copy this block for each failure:

```text
Case:
Server/build, client/version:
UIcore JAR SHA-256:
Scenario/menu files:
TPS and latency:
Steps to reproduce:
Expected and observed result:
Final eye coordinates and endpoint error:
Video path/link and timestamp:
Log path and relevant exception:
Retest result:
```

Client interpolation and movement across Folia region boundaries remain release verification gates until these runs produce evidence. Do not mark the gameplay acceptance criteria complete based only on Maven tests or the plugin's `folia-supported` manifest flag.
