# UIcore Lab

[Русская версия](README_RU.md)

UIcore Lab is a five-screen showcase for UIcore 1.1. It runs with vanilla items, text displays and a viewer-only Sponge schematic. Players need no mod or resource pack.

1. Install UIcore 1.1 and PacketEvents on a test server. Copy the contents of `menu/`, `cutscenes/` and `schematics/` into the matching folders under `plugins/UIcore/`.
2. Keep `use-player-location: false` in UIcore's `config.yml`. The sample uses the world named `world` and a fixed camera near `0.5, 110, 0.5`. For another world or stage location, adjust the five camera settings, the world anchor of the schematic and the tour points together.
3. Start the server or reload UIcore with no demo sessions open. Validate the route with `/uicore cutscene validate`, then open `/uicore run showcase_lab`. From the console, append the player's name.
4. For a recording, use daylight and set the client to fullscreen. The preview wall does not change the world's light level.

| Exhibit | What to show |
| --- | --- |
| Gallery | Select Compass, Geode or Relay. The middle slot changes for that player. Hover the specimen and compare it with the viewer's avatar. |
| Controls | Scroll the wheel, drag the board by its label, then drop the diamond in the socket. Drag the slider to change the signal in steps. A click picks up an item and a second click drops it. |
| Terminal | Activate and reset the relay to change materials, text and visibility. Select **Set callsign**, press T, and enter a test callsign. |
| Camera Tour | Start the tour, select **Observation** or **Systems**, then watch the camera return. |

The stage is a personal block preview. Closing the menu restores the real blocks for the viewer. No world edits or optional plugins are required. For the separate experimental hold-to-drag mode, use the bundled `demo_drag` menu.
