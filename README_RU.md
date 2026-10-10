# UIcore

> **Язык:** [English](README.md) | Русский

### UI-фреймворк для Minecraft, работающий прямо в мире

UIcore строит меню вокруг камеры игрока. Текст, предметы, кнопки, поля ввода, состояния, слоты, NPC-зеркала и персональные предпросмотры схем находятся в одной сцене с игроком.

Основной язык репозитория и документации — английский. Эта страница помогает русскоязычным администраторам начать работу и ведёт к русским переводам справочника.

## Что входит

- Мировые меню, элементы текста и предметов, кнопки, поля ввода, состояния и анимации.
- Слоты и персональные предпросмотры Sponge `.schem`.
- Java API для действий, источников данных, условий, компонентов, меню и слотов.
- Встроенная поддержка Vault и LuckPerms.
- Планировщики Paper и Folia, bytecode Java 17.

## Сборка

```bash
mvn clean package
```

Результаты находятся в `uicore-api/target/`, `uicore-plugin/target/` и `uicore-example-extension/target/`.

## Требования

- Minecraft 1.21.1 и новее на Paper, Folia, Purpur или совместимом форке.
- Java, требуемая выбранной сборкой сервера. Сам плагин собран с bytecode Java 17.
- PacketEvents 2.14.0.

PlaceholderAPI, FancyNpcs, SkinsRestorer, CraftEngine, Vault и LuckPerms подключаются по необходимости.

## Демонстрационные меню

Открой `/uicore run demo_main`. Три примера входят в плагин и устанавливаются при первом запуске:

- [demo_main](uicore-plugin/src/main/resources/menu/demo_main.yml) — наведение, смена курсора, ввод через чат, переключатель и задержка между кликами.
- [demo_gallery](uicore-plugin/src/main/resources/menu/demo_gallery.yml) — предметы, аватар игрока и смена шаблонов в слоте.
- [demo_states](uicore-plugin/src/main/resources/menu/demo_states.yml) — изменение текста, материала и видимости, оформление по правам и ограничения кликов.

Ресурс-пак и дополнительные плагины не нужны. Для кнопок выбора в галерее требуется `uicore.slot`; OP получает это право по умолчанию. [Инструкция и сценарий демонстрации](showcase/README_RU.md) · [English](showcase/README.md).

## Документация

- [Английский индекс документации](docs/README.md)
- [Справочник автора меню](docs/MENU_AUTHORING_GUIDE_RU.md) · [English](docs/MENU_AUTHORING_GUIDE.md)
- [API расширений](docs/EXTENSION_API_RU.md) · [English](docs/EXTENSION_API.md)
- [Vault и LuckPerms](docs/BUILTIN_INTEGRATIONS_RU.md) · [English](docs/BUILTIN_INTEGRATIONS.md)
- [Совместимость](docs/COMPATIBILITY_RU.md) · [English](docs/COMPATIBILITY.md)
- [Установка](release/INSTALL_RU.md) · [English](release/INSTALL.md)

UIcore основан на CustomScreenMenu 1.4.8 и публикуется с разрешения автора исходного плагина, **wuhaoxian123**. Подробнее: [страница CustomScreenMenu](https://builtbybit.com/resources/customscreenmenu.108929/) и [LICENSE](LICENSE).
