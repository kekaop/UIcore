# Protocol verification evidence

`results.json` records the assertions and tested JAR hash. The two logs include server versions, step events, requests, exact camera eye coordinates, cursor coordinates, cleanup checks, TPS queries, and server shutdown.

`automated-tests.json` records the successful Maven package build: 187 tests, zero failures, and zero errors. Its JAR hash matches the two protocol runs.

`CutsceneProbe.java`, `bot.cjs`, and `verify.cjs` preserve the test harness source. The probe plugin was installed only in disposable servers under `target/cutscene-server-smoke` and `target/cutscene-folia-smoke`. The clients use the `minecraft-protocol` npm package from `target/cutscene-client/node_modules`.

To repeat the run, prepare these two local server directories with the recorded server versions, PacketEvents 2.14.0, the current UIcore JAR, and the compiled probe plugin. Bind them to localhost ports 25586 and 25587. Copy the client scripts into `target/cutscene-client`, then run `node target/cutscene-client/verify.cjs` from the workspace. The script starts and stops both disposable servers and writes a new result file. Its Java path is specific to this Windows test environment.

The harness checks both branches, competing API choices, pause/resume, endpoint precision, final cursor coordinates, chunk crossing, and menu closure. It does not render Minecraft or prove visual interpolation, mouse picking, reduced-TPS behavior, or an independent Folia region transfer. Those checks remain listed in [the gameplay checklist](../../CUTSCENE_GAMEPLAY_CHECKS.md).
