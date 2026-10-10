# Совместимость UIcore

> **Язык:** [English](COMPATIBILITY.md) | Русский

Один JAR собирается с `--release 17` против Paper API 1.21.1. В `plugin.yml` указан `api-version: '1.21'` и `folia-supported: true`. Поддерживаются Paper, Folia, Purpur и Leaf на Minecraft 1.21.1 и новее, если у серверного проекта есть сборка для нужной версии. PacketEvents 2.14.0 обязателен.

| Сервер | Minecraft | Проверенная сборка | Java сервера | Результат                                                             |
| ------ | --------- | ------------------ | ------------ | --------------------------------------------------------------------- |
| Paper  | 1.21.11   | 132                | 21           | запуск, reload, остановка                                             |
| Folia  | 1.21.11   | 14                 | 21           | запуск, reload, остановка; тестовый игрок открыл и закрыл `demo_main` |
| Paper  | 26.2      | 129                | 25           | запуск, reload, остановка                                             |
| Folia  | 26.2      | 7 beta             | 25           | запуск, reload, остановка                                             |
| Purpur | 26.2      | 2633               | 25           | запуск, reload, остановка                                             |
| Leaf   | 26.2      | 123 experimental   | 25           | запуск, reload, остановка                                             |

Матрица выше получена на одной сборке UIcore 1.0.0 с PacketEvents 2.14.0. На Folia 26.2 сборка прошла запуск и остановку. Остальные строки не повторялись именно для финального JAR. В ранее проверенных сочетаниях `/uicore reload` выполнилась, сервер завершился с кодом 0; ошибок UIcore, `NoSuchMethodError` и `NoClassDefFoundError` в журналах нет. Визуальный вид, все виды взаимодействия и дополнительные интеграции остаются задачами [игровой проверки](SMOKE_CHECKLIST.md).

Java 17 — нижний уровень bytecode плагина, а не универсальная Java для сервера: запускайте каждую версию Minecraft на Java, которую требует соответствующая сборка сервера. Поддержка Folia ограничивает расположение элементов пределами региона камеры; подробности — в [статусе переноса](PORT_26_2_RU.md).

## Проблемы чата с NoChatReports

Если ввод через чат перестал работать с установленным **NoChatReports**, отключите мод, полностью перезапустите клиент и подключитесь заново. Проверьте обычный чат до открытия меню, ввод в поле меню и обычный чат после закрытия меню.

10 октября 2026 года игрок сообщил о проблеме на UIcore 1.0.0, Paper 26.1.2 build 72, PacketEvents 2.14.0 и NoChatReports Fabric 26.1-v2.19.0. В клиенте стоял `defaultSigningMode: ON_DEMAND`, на сервере — `online-mode=true` и `enforce-secure-profile=true`. Игрок подтвердил, что после отключения мода чат и ввод в меню работают. Это результат для одного сочетания версий и настроек; он не означает, что проблема возникает со всеми версиями NoChatReports.

В журнале сервера могут появиться:

```text
Failed to update secure chat state for <player>: 'Chat disabled due to missing profile public key. Please try reconnecting.'
Failed to update secure chat state for <player>: 'Chat had an invalid signature. Please try reconnecting.'
Failed to update secure chat state for <player>: 'Chat disabled due to broken chain. Please try reconnecting.'
```

### Почему это может происходить

NoChatReports меняет механизм подписания чата. В режиме `ON_DEMAND` он сначала отправляет сообщения без подписи. Когда сервер отклоняет сообщение из-за отсутствующего ключа профиля, мод включает подпись и повторно отправляет последнее сообщение. Это поведение описано в [режимах подписания](https://github.com/Aizistral-Studios/No-Chat-Reports/blob/26.1-Unified/src/main/java/com/aizistral/nochatreports/common/core/SigningMode.java) и реализовано в [обработчике предупреждений чата](https://github.com/Aizistral-Studios/No-Chat-Reports/blob/26.1-Unified/src/main/java/com/aizistral/nochatreports/common/mixins/client/MixinChatListener.java) и [инициализации подписи](https://github.com/Aizistral-Studios/No-Chat-Reports/blob/26.1-Unified/src/main/java/com/aizistral/nochatreports/common/core/ServerSafetyState.java).

Это объясняет предупреждение об отсутствующем ключе. Если следующее сообщение не проходит проверку подписи, сервер помечает цепочку подписанных сообщений как нарушенную и отклоняет дальнейшие сообщения. Закрытие меню UIcore не восстанавливает эту цепочку. UIcore получает текст для полей через событие чата Bukkit, после проверки входящего сообщения сервером; плагин не удаляет подписи и не отменяет пакеты сессии чата.

Точный момент возникновения неверной подписи в этом сочетании пока не установлен. Проверка открытия и закрытия меню без ввода текста прошла и с включённым модом, поэтому сбой проявлялся не всегда. Отключение NoChatReports с перезапуском клиента — проверенное обходное решение. Режим подписи `ALWAYS` можно проверить как альтернативу, но для этого сочетания его работа не подтверждена.
