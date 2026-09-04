# UIcore: краткий справочник автора меню

Полная английская и китайская документация находится в `docs/UIcore_GitBook.html`. Этот файл служит краткой русской памяткой.

## Файлы и команды

- Меню: `plugins/UIcore/menu/*.yml`.
- Основной конфиг: `plugins/UIcore/config.yml`.
- Предметы: `plugins/UIcore/items.yml`.
- Текстовые дисплеи: `plugins/UIcore/text.yml`.
- Открыть меню: `/uicore run <menu>`.
- Закрыть: `/uicore stop`.
- Перезагрузить: `/uicore reload`.

## Структура меню

Верхний ключ является id меню. `camera-position` задаёт камеру, а `layout` содержит элементы. Координаты `x/y/z` являются смещениями в мировом пространстве относительно базиса камеры. Для каждого элемента доступны текст, размер, наклон, команды, переход в другое меню и закрытие.

```yaml
demo:
  camera-position:
    distance: 1.5
    world: world
    x: 0
    y: 70
    z: 0
    yaw: 180
    pitch: 0
  layout:
    close:
      name: "<red>Close"
      x: 0
      y: -0.5
      z: 3.5
      hover-enlarge:
        enabled: true
        scale: 1.1
      stop-menu:
        enabled: true
```

## Возможности элементов

- `style`, `hover-style`, `pressed-style`, `permission-style` управляют цветом и форматированием.
- `hover-cursor` меняет предмет курсора; `hover-tooltip` показывает подсказку.
- `command`, `hover-commands`, `leave-commands`, `permission-denied-commands` выполняют действия.
- `click-requirements` и `visible-if` проверяют permission, placeholder и состояние меню.
- `toggle-content` и `toggle-style` хранят состояние отдельно для каждого игрока; команды `on-commands` и `off-commands` зависят от нового состояния.
- `input-field` принимает следующий текст из чата и делает его доступным через placeholder/API.
- `cooldown`, `bar-text`, entrance/runtime animations, slots/templates и ItemDisplay описаны в полном HTML.
- `x`, `y`, `z` могут быть формулами; плавность задаётся `position-update.smoothing`.

MiniMessage поддерживается в сообщениях, включая click/hover events. Привилегированные console-команды блокируются политикой `security` по умолчанию.
