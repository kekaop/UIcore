# Try the UIcore demos

> **Language:** English | [Русская версия](README_RU.md)

The three demo menus are the plugin's bundled examples. Their YAML sources live in [the menu resource directory](../uicore-plugin/src/main/resources/menu); this folder contains instructions for testing and recording them.

| Menu                                                                      | What to show                                                                   |
| ------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| [demo_main](../uicore-plugin/src/main/resources/menu/demo_main.yml)       | Hover feedback, cursor changes, chat input, toggles, and cooldowns             |
| [demo_gallery](../uicore-plugin/src/main/resources/menu/demo_gallery.yml) | Static items, a personal avatar, and three templates in a player-specific slot |
| [demo_states](../uicore-plugin/src/main/resources/menu/demo_states.yml)   | Text, material, visibility, and permission-based appearance and access         |

## Install and open the menus

On a fresh installation, the plugin installs these three menus automatically. On an existing server:

1. Back up any existing `demo_main.yml` you have customized.
2. Copy all three YAML files from the source directory to `plugins/UIcore/menu/`.
3. Remove old demo files from that server only if you no longer use them. Updating the plugin does not delete existing menus.
4. Run `/uicore reload`, then `/uicore run demo_main`.

The tester needs `uicore.start`, `uicore.close`, and `uicore.slot`. The administrator running reload also needs `uicore.reload`. These commands work for OP players with default permissions. For the access demo, use a non-OP tester and grant the needed command permissions individually.

A resource pack and optional integrations are not required. The camera follows the player's opening position; the **Close** button returns the player to the original location. The source config uses `cursor-item.z-offset: 1.0`. Existing server configs retain their current value; change it manually if you want the same cursor depth.

## Walk through the interactions

In **demo_main**:

1. Hover the name field and **Send signal** to show the color and cursor changes.
2. Click **Save name** while the field is empty to show the denied actionbar message.
3. Click the name field, enter a short name in chat, and click **Save name** again.
4. Toggle **Notifications** twice to show both labels and styles.
5. Click **Send signal** twice within three seconds to show cooldown feedback.

The name and notification controls demonstrate UI interactions. They do not update an external profile or notification service.

## Switch the gallery previews

Open **Gallery**, then select **Knight**, **Miner**, and **Scout**. The center slot switches templates. Hover the book and avatar to show tooltips. With two players, select different templates to demonstrate that one player's choice does not change the other's preview.

A slot override can remain after reopening the menu. To restore the default, run `/uicore slot reset <player> demo_gallery loadout`.

## Show states and permissions

Open **States**. Click **Activate** to change the status text, replace coal with an emerald, and reveal the hidden label. Click **Reset** to restore all three elements. Element states are cleared when the menu closes.

For the access test, open the menu as a non-OP player without `uicore.demo.vip`. **VIP access** should look unavailable, and both access buttons should deny the click. Grant `uicore.demo.vip` with your permissions plugin and reopen the menu to show the allowed appearance and actions.

These examples do not use dragging or automatic item rotation. Check appearance, cursor alignment, and clicks in a Minecraft client before recording; a source build cannot verify those visuals.

Use the [recording notes](videos/README.md) when preparing README videos.
