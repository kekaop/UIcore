# UIcore sale readiness

> **Language:** English | [Русская версия](SALE_READINESS_RU.md)

This is an internal release checklist. It is not included in a buyer archive.

## Ready

- The project version is `1.0.0` without `SNAPSHOT`, and `mvn clean verify` builds the release.
- Personal `oc_*` examples are outside the JAR and are not installed for a buyer.
- Main demonstration menus start from the player's position.
- The JAR contains RE2/J and bStats notices.
- The archive includes MorePaperLib source, its Maven build, and a relinking script.
- English is the primary documentation language; Russian translations remain linked beside their English pages.

## Complete before publishing

- Keep the written conversation with wuhaoxian123 private. It records the permission to publish the CustomScreenMenu-derived source with the required attribution and excludes BuiltByBit.
- Confirm that the planned distribution and marketplace terms comply with AGPL-3.0, the upstream permission, and MorePaperLib's LGPL v3 obligations.
- Complete a normal-client test on the exact server builds being listed: clicks, menu transitions, states, schematics, and cleanup.
- Test two players, cancelled teleports, death, restart recovery, and optional FancyNpcs, SkinsRestorer, and CraftEngine integrations.
- Finalize a public listing that accurately states requirements, tested combinations, limits, support, pricing, and refund terms.
