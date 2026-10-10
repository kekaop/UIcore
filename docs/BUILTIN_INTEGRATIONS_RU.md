# Встроенная поддержка Vault и LuckPerms

> **Язык:** [English](BUILTIN_INTEGRATIONS.md) | Русский

Vault и LuckPerms подключаются прямо из UIcore. Отдельный адаптер устанавливать не нужно. Оба плагина необязательны: без них UIcore запускается, а связанные с ними значения и условия недоступны. Установите нужный плагин и перезапустите сервер.

## Покажите баланс через Vault

Для `vault:balance` нужен Vault и зарегистрированный им сервис экономики. Значение — число, поэтому его можно использовать и в числовых свойствах элемента. `vault:formatted-balance` возвращает строку в формате экономики.

```yaml
shop:
  camera-position:
    type: player
    distance: 1.5
  layout:
    balance:
      name: '<gold>Баланс: {vault:formatted-balance}'
      x: 0
      y: 0.5
      z: 3.5
    offer:
      name: '<green>Доступно при балансе от 100'
      x: 0
      y: 0
      z: 3.5
      visible-if:
        custom:
          type: vault:has-balance
          amount: 100
```

`vault:has-balance` принимает неотрицательное число `amount`. Если сумма неверна, Vault отсутствует или сервис экономики недоступен, условие ложно. Эти встроенные обработчики только читают баланс. Они не списывают и не начисляют деньги.

## Используйте группы и метаданные LuckPerms

Источники данных `{luckperms:primary-group}`, `{luckperms:prefix}` и `{luckperms:suffix}` читают сведения подключённого игрока. `luckperms:in-group` проверяет членство с учётом наследования групп и текущего контекста игрока. `luckperms:meta-equals` сравнивает строковое метаполе с заданным значением в текущем контексте.

```yaml
rank_menu:
  camera-position:
    type: player
    distance: 1.5
  layout:
    rank:
      name: '<yellow>Группа: {luckperms:primary-group}'
      x: 0
      y: 0
      z: 3.5
      visible-if:
        custom:
          type: luckperms:in-group
          group: vip
```

Для `luckperms:meta-equals` укажите `key` и `value` в той же секции `custom`, например `key: menu_theme` и `value: dark`.

Для проверки обычного права используйте существующее поле `permission` элемента или стандартное условие UIcore. Дополнительное условие LuckPerms для этого не требуется.

Данные кэшируются на 20 тиков для каждого элемента и игрока. Вызов `UIcoreApi.refresh(player)` сбрасывает кэш и обновляет открытое меню. Значения вставляются как обычный текст: разметка MiniMessage из префикса или суффикса не исполняется. Если игрок ещё не загружен в LuckPerms, источник данных пуст, а условие ложно.
