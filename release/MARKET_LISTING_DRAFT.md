# UIcore marketplace listing draft

> **Language:** English | [Русская версия](MARKET_LISTING_DRAFT_RU.md)

**Draft for a marketplace listing. Do not publish until the [sale readiness checks](SALE_READINESS.md) are complete.**

UIcore is a plugin for interactive menus placed directly in the Minecraft world. Text, items, and buttons are configured in YAML. Elements can have states that change on click, and menus can show player-specific `.schem` previews without changing world blocks.

**Features:** text and item menus; click actions; element states (`initial-state`, `states`, `set-state`, and `set-states`); conditions including permissions; personal Sponge v2/v3 schematic previews; an API for other plugins; YAML configuration; and `/uicore reload`.

**Requirements:** Paper, Folia, Purpur, or Leaf for Minecraft 1.21.1 or newer; PacketEvents 2.14.0; and the Java runtime required by the selected server build. See the [compatibility matrix](../docs/COMPATIBILITY.md). PlaceholderAPI, FancyNpcs, SkinsRestorer, CraftEngine, Vault, and LuckPerms are optional integrations that need testing on the exact target version.

**Included:** `UIcore.jar`, installation instructions, YAML authoring guide, compatibility matrix, third-party notices, and MorePaperLib source with rebuild instructions. UIcore is based on [CustomScreenMenu](https://builtbybit.com/resources/customscreenmenu.108929/) and is published under AGPL-3.0 with the original author's permission.

**Limits:** Folia scenes should remain inside the camera region. Schematic previews do not show container or other block-entity contents, entities, or biomes. On 26.2, server startup is confirmed, while a complete normal-client playthrough remains pending.

**Complete before publishing:** price, permitted server count and term, support and update period, refund rules, and a support contact. Verify the marketplace's rules and the rights holder's permission before uploading a release.
