# Built-in Vault and LuckPerms support

> **Language:** English | [Русская версия](BUILTIN_INTEGRATIONS_RU.md)

Vault and LuckPerms work directly with UIcore. You don't need a separate adapter. Both plugins are optional: UIcore starts without them, while their data and conditions remain unavailable. Install the plugin you need and restart the server.

## Show a Vault balance

`vault:balance` requires Vault and a registered economy service. It returns a number, so you can also use it in numeric element properties. `vault:formatted-balance` returns a string formatted by the economy provider.

```yaml
shop:
  camera-position:
    type: player
    distance: 1.5
  layout:
    balance:
      name: '<gold>Balance: {vault:formatted-balance}'
      x: 0
      y: 0.5
      z: 3.5
    offer:
      name: '<green>Available with a balance of 100 or more'
      x: 0
      y: 0
      z: 3.5
      visible-if:
        custom:
          type: vault:has-balance
          amount: 100
```

`vault:has-balance` accepts a nonnegative numeric `amount`. It is false when the amount is invalid, Vault is absent, or no economy service is available. These built-in handlers only read balances. They don't withdraw or deposit money.

## Use LuckPerms groups and metadata

`{luckperms:primary-group}`, `{luckperms:prefix}`, and `{luckperms:suffix}` read data for the online player. `luckperms:in-group` checks inherited group membership using the player's current context. `luckperms:meta-equals` compares a string metadata key with a given value in the current context.

```yaml
rank_menu:
  camera-position:
    type: player
    distance: 1.5
  layout:
    rank:
      name: '<yellow>Group: {luckperms:primary-group}'
      x: 0
      y: 0
      z: 3.5
      visible-if:
        custom:
          type: luckperms:in-group
          group: vip
```

For `luckperms:meta-equals`, set `key` and `value` in the same `custom` section, for example `key: menu_theme` and `value: dark`.

To check an ordinary permission, use the element's existing `permission` property or a standard UIcore condition. No extra LuckPerms condition is needed.

Data is cached for 20 ticks per player and element. `UIcoreApi.refresh(player)` clears the cache and updates the open menu. Values are inserted as plain text; MiniMessage markup in a prefix or suffix isn't interpreted. If LuckPerms hasn't loaded the player yet, data is empty and conditions are false.
