# UIcore audit plan

> **Language:** English | [Русская версия](BUG_AUDIT_PLAN_RU.md)

This plan lists checks to complete before the next stable release. It does not install test menus or confirm that a scenario has passed.

## Verify lifecycle behavior

- Enable and disable on supported Paper and Folia builds.
- Reload with an open menu, switch quickly through `next-menu`, and close immediately after a click.
- Test quit, kick, death, world changes, teleports, and two simultaneous players.

## Verify input and hitboxes

- Check hover at the upper and lower edge of text, elements at different camera depths, left and right clicks, and repeated clicks after a transition.
- Test chat, command, and Java API input, then cursor state after switching and closing a menu.
- Test group movement and single-element rotation in the same menu.

## Verify rendering and integrations

- Check styles, tooltips, states, slots, templates, schematic previews, scale, FOV, resolution, and client restart behavior.
- Start with and without Vault, LuckPerms, PlaceholderAPI, FancyNpcs, SkinsRestorer, and CraftEngine where support is claimed.

## Verify failure handling

- Test invalid YAML, missing extension IDs, invalid coordinates and dimensions, long external callbacks, and command allowlists.
- Confirm logs do not reveal password or other secret input values.

Run the automated suite with:

```text
mvn -q clean package
```

After the build, confirm that `UIcore-1.0.0.jar` contains no showcase menus, `docs/`, or HTML documentation. Mark in-game checks only after testing with a normal Minecraft client.
