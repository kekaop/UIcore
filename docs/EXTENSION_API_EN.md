# UIcore extensions

[Русская версия](EXTENSION_API_RU.md).

A plugin can register actions, data providers, conditions, and components. Server admins use them in ordinary YAML menus. Each registration belongs to a Bukkit plugin and is removed when that plugin disables.

Vault and LuckPerms support is built into UIcore. See the [built-in integrations guide](BUILTIN_INTEGRATIONS_EN.md) for ready-to-use IDs and YAML examples.

The buildable [`uicore-example-extension`](../uicore-example-extension/) module registers all four extension types. Place its JAR alongside UIcore and copy [`example_extension.yml`](../uicore-example-extension/example_extension.yml) into `plugins/UIcore/menu/`.

## Register handlers

Get `UIcoreApi` from `UIcoreProvider.get()` after UIcore has enabled. Pass your plugin instance as the owner:

```java
api.actions().register(this, "quests:accept", context -> {
    questService.accept(context.player(), context.argument("quest"));
});

api.data().register(this, "economy:balance", 20, context ->
    economy.getBalance(context.player())
);

api.conditions().register(this, "quests:completed", context ->
    quests.hasCompleted(context.player(), context.argument("id"))
);

api.components().register(this, "quests:quest-card", context -> List.of(
    MenuElementSpec.builder("title")
        .text("<gold>" + context.parameter("title"))
        .position(0, 0.3, 0).build(),
    MenuElementSpec.builder("accept")
        .text("<green>Accept")
        .position(0, 0, 0)
        .action("quests:accept", Map.of("quest", context.parameter("quest")))
        .build()
));
```

IDs use lowercase namespaced form such as `quests:accept`. A duplicate ID of the same extension type is rejected. Close the returned `ExtensionRegistration` to unregister early. UIcore also removes registrations automatically when the owner disables and refreshes open menus.

Handlers and component factories run on the player's owning region on Paper/Folia. Keep them quick. For asynchronous work, schedule application on the player's region and check that the same menu is still open. Do not access Bukkit from an arbitrary asynchronous callback.

## Use extensions in YAML

```yaml
quests:
  schema-version: 2
  camera-position:
    type: player
    distance: 1.5
  layout:
    balance:
      name: '<gold>Balance: {economy:balance}'
      x: 0
      y: 0.5
      z: 3.5
    accept_button:
      name: '<green>Accept'
      x: 0
      y: 0
      z: 3.5
      action:
        type: quests:accept
        quest: daily_1
      visible-if:
        custom:
          type: quests:completed
          id: tutorial
    card:
      type: quests:quest-card
      title: Daily quest
      quest: daily_1
      x: 1
      y: 0
      z: 3.5
```

`action` runs after the usual click permission, visibility, and cooldown checks. Its arguments and custom condition arguments accept strings, numbers, and booleans. `context.cancel()` stops the rest of that click, including built-in commands.

`{economy:balance}` is inserted as literal text, so provider output is never interpreted as MiniMessage markup. A data registration can set a minimum refresh interval in ticks; the default is one tick. Call `api.refresh(player)` to invalidate the player's data cache and refresh the open menu. Numeric providers also work in dynamic coordinates and as the exact value of `bar.value`, `bar.max`, or `custom-model-data`. A string provider may supply `material`, and a boolean provider may supply `glow`. Item properties require a complete reference such as `material: '{items:current-material}'`.

Custom conditions work with `visible-if` and its existing `all`, `any`, and `not` operators. Missing or failing conditions keep the element hidden, even under `not`. Missing IDs and callback failures are logged without argument values, with repeated messages rate limited.

After dependent plugins enable, UIcore checks extension references in menus and templates. Diagnostics include the menu ID, element ID, property path, extension kind, and missing ID. The check runs again on configuration reload. Missing extensions do not prevent a menu from loading; its action or component becomes available when the owner registers it.

Components create ordinary UIcore elements for each player. Component and child positions must be numeric; child positions are relative to the component position. Child IDs are scoped to the component, for example `card/title`. Parent permissions and visibility are checked before calling the factory. Nested components are supported up to four levels; each factory may return up to 64 children, and a menu expansion stops at 256 factory calls or 256 elements. A factory result is cached within one tick; UIcore reuses a child `MenuLayout` while its definition stays the same, preserving click state. Call `api.refresh(player)` when the factory result depends on changed state.

The old string state API remains available. Typed access uses `StateKey.string`, `StateKey.integer`, `StateKey.decimal`, or `StateKey.bool`:

```java
StateKey<Integer> page = StateKey.integer("page", 1);
api.state(player, "quests").set(page, 3);
int currentPage = api.state(player, "quests").get(page);
```

State is isolated by player and menu ID. It remains in memory for the menu session and is cleared when that session ends or the player leaves. It does not survive server restarts.

New code can use `api.menus()`, `api.elements()`, `api.inputs()`, and `api.slots()` to group existing operations. Legacy `UIcoreApi` methods remain available.

Element descriptions use the immutable, YAML-independent `UiElementModel`. YAML loading, the Java API, and component factories pass through this model before UIcore creates a `MenuLayout` for its existing renderer. `MenuElementSpec.model()` and `MenuElementEditor.model()` expose the description without `YamlConfiguration`; `toConfiguration()` remains for compatibility.

Menus without `schema-version` are treated as version 1. Versions 1 and 2 currently load the same way, so old menus need no migration. Unknown versions fail loading with the menu ID in the error. Extension fields work in either version.

Build and automated tests pass. UIcore and the example extension also started and stopped cleanly with the example YAML menu on a local Paper 26.1.2 server. Clicks, data substitution, and component rendering with a connected player, as well as Folia behavior, still need in-game testing. The existing renderer still uses `MenuLayout`, and data bindings for every element property are not yet implemented.
