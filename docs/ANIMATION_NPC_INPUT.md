# Animation, NPC, And Input Notes

The canonical bilingual reference is `docs/UIcore_GitBook.html`.

## Entrance animation

Each menu element may define `entrance-animation` with a delay, duration, origin offset, and easing. Delays are ticks. Elements remain hidden until their delay expires, then move and/or fade toward their configured position.

## Runtime animation

The `animation` section supports continuous movement and transformation modes implemented by the element renderer, including move/float, rotate, scale, bounce, and fade where supported. Item displays and text displays use their own transformation paths.

## NPC mirror

NPC behavior can be configured globally in `npc_config.yml` and overridden per menu. FancyNpcs is optional. FancyNpcsModel support is an optional integration; menus must still work when it is absent.

## Input fields

Any element can focus a named input field. The next chat message is cancelled, stored per player, exposed as `%uicore_input_<field>%`, and can be inserted into menu commands. Password-like fields are masked in feedback and must never be logged in plain text.
