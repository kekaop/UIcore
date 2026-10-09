# UIcore showcase package

This directory contains the material used for the UIcore README and feature videos. It is intentionally outside `uicore-plugin/src/main/resources`, so Maven does not copy it into the plugin JAR.

## Record the videos in this order

1. `showcase_main.yml` — opening the menu and moving through the feature demos.
2. `showcase_interactions.yml` — hover states, click feedback, element states, and chat input.
3. `showcase_drag.yml` — moving a group and rotating one element independently.
4. `showcase_integrations.yml` — Vault balance, LuckPerms group, prefix, and suffix.
5. `showcase_slots.yml` — slot templates and item transitions.

To test one of these menus locally, copy the selected file to `plugins/UIcore/menu/`, run `/uicore reload`, and open it with `/uicore run <menu-id>`. The files are showcase sources and are not installed by the build.

The `videos/` folder is reserved for links, thumbnails, and recording notes. Keep the final videos on a public host such as YouTube or GitHub Releases, then replace the placeholders in the root README.
