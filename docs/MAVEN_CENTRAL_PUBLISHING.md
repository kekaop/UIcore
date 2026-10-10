# Публикация UIcore API в Maven Central

Этот документ предназначен владельцу репозитория. Подготовленная конфигурация
выпускает **два** компонента версии `1.0.0`: `uicore-parent` (POM) и `uicore-api`
(JAR, sources и Javadoc). API ссылается на parent POM, поэтому оба компонента
должны попасть в один выпуск. `uicore-plugin` и `uicore-example-extension` не
публикуются. Java package names остаются `org.orvencraft.uicore`.

Пока выпуск не подтверждён в Central Portal, этих компонентов в Maven Central нет.
Обычный `mvn clean verify` не публикует ничего. Профиль `central-release` создаёт
подписи и подключает официальный `central-publishing-maven-plugin` версии `0.11.0`.
Плагин формирует контрольные суммы. `autoPublish=false` оставляет окончательное
решение о публикации владельцу в Central Portal.

До выпуска можно установить parent и API только в локальный Maven-репозиторий:

```bash
mvn -B -ntp -pl uicore-api -am install
```

## 1. Подтвердите namespace

Maven `groupId` проекта — `io.github.kekaop`. Откройте
[Central Portal](https://central.sonatype.com/), войдите через GitHub-аккаунт
`kekaop` и откройте **View Namespaces**. Sonatype часто подтверждает личный
GitHub namespace автоматически. Проверьте, что `io.github.kekaop` имеет статус
**Verified** и этот аккаунт имеет право публикации.

Если namespace отсутствует, [добавьте его](https://central.sonatype.org/register/namespace/)
и выполните предложенную Portal проверку владения GitHub-аккаунтом. Для этого
namespace не нужен домен или DNS TXT-запись. Не запускайте deploy до состояния
**Verified**. Maven coordinates не требуют переименования Java-пакетов:
`org.orvencraft.uicore` остаётся именем пакета в исходниках и JAR.

## 2. Создайте publishing token и ключ подписи

В Portal откройте [User Tokens](https://central.sonatype.com/usertoken), создайте
токен и сохраните обе выданные части: token username и token password. Повторно
посмотреть их после закрытия окна нельзя.

Создайте отдельный GPG-ключ с парольной фразой или используйте подходящий
существующий ключ. Опубликуйте **публичный** ключ в доступном сервере ключей,
как описано в [инструкции Sonatype по GPG](https://central.sonatype.org/publish/requirements/gpg/).
Проверьте отпечаток и возможность подписи локально. Приватный ключ и парольную
фразу не добавляйте в Git. Для GitHub Actions экспортируйте ASCII-armored
приватный ключ через `gpg --armor --export-secret-keys <KEY_ID>` и поместите его
только в секрет GitHub.

В настройках репозитория GitHub откройте **Settings → Secrets and variables →
Actions** и добавьте:

| Secret                   | Значение                                      |
| ------------------------ | --------------------------------------------- |
| `CENTRAL_TOKEN_USERNAME` | Username publishing token из Portal           |
| `CENTRAL_TOKEN_PASSWORD` | Password publishing token из Portal           |
| `GPG_PRIVATE_KEY`        | Полный ASCII-armored экспорт приватного ключа |
| `GPG_PASSPHRASE`         | Парольная фраза приватного ключа              |

Workflow создаёт Maven `settings.xml` через `actions/setup-java` и импортирует
ключ в GnuPG на временном runner. В репозитории нет токенов и приватных ключей.

## 3. Проверьте сборку перед выпуском

Для полной сборки нужен JDK 21 или новее и Maven 3.9 или новее. API остаётся
скомпилированным для Java 17 (`release 17`). В корне репозитория выполните:

```bash
mvn -B -ntp clean verify
mvn -B -ntp -Pcentral-release -pl uicore-api -am verify -Dgpg.skip=true
python3 scripts/check-central-artifacts.py
```

Вторая команда собирает только parent POM и API с sources и Javadoc. Параметр
`-Dgpg.skip=true` нужен лишь для локального просмотра без приватного ключа;
**не используйте его при deploy**. Проверка подписей, Portal validation и
разрешение опубликованной зависимости окончательно проверяются только с настоящими
ключом, токеном и доступом к Portal.

В API POM указана `paper-api:1.21.1-R0.1-SNAPSHOT` с `provided` scope. Она
получается из репозитория Paper, объявленного в parent POM. Это не транзитивная
runtime-зависимость другого плагина, но компилирующему расширение проекту тоже
потребуется репозиторий Paper. После загрузки обязательно проверьте результат
валидации Portal и соберите внешний проект с опубликованными POM и JAR.

## 4. Запустите выпуск по тегу вручную

Тег `v1.0.0` уже занят существующим GitHub-релизом плагина и указывает на более
ранний коммит. После проверки и отдельного решения о выпуске API создайте
отдельный тег `api-v1.0.0` **на проверенном коммите**, затем отправьте его:

```bash
git tag api-v1.0.0 <COMMIT_SHA>
git push origin api-v1.0.0
```

После того как workflow окажется в default branch, запустите его из этой ветки
через GitHub CLI. Workflow сам загрузит содержимое указанного тега:

```bash
gh workflow run publish-api.yml --ref orven/normal-source-build -f release_tag=api-v1.0.0
gh run watch
```

Workflow не запускается при обычном push и завершается без работы при указании
иного тега. Он сверяет тег, `groupId`, версию `1.0.0`, запускает полную
сборку и тесты, проверяет JAR, sources, Javadoc, POM и GPG-подписи. После этого
Maven загружает bundle в Portal с ручным выпуском.

Если job пропущен, проверьте, что workflow уже находится в default branch и
команда указала `release_tag`. Не запускайте deploy повторно после успешной загрузки:
найдите уже созданный deployment в Portal.

Для ручной загрузки с локального компьютера настройте `~/.m2/settings.xml` с
`<server><id>central</id>` и частями publishing token в `username` и `password`.
Импортируйте приватный GPG-ключ в локальный keyring, передайте парольную фразу
через `MAVEN_GPG_PASSPHRASE` или подготовленный `gpg-agent`, затем выполните:

```bash
mvn -B -ntp clean verify
mvn -B -ntp -Pcentral-release -pl uicore-api -am verify
python3 scripts/check-central-artifacts.py --require-signatures
mvn -B -ntp -Pcentral-release -pl uicore-api -am deploy -DskipTests
```

Последняя команда **загружает** bundle в Portal. Не выполняйте её без разрешения
владельца на этот конкретный выпуск. Она не нажимает **Publish** в Portal.

## 5. Проверьте deployment и подключите API

В [Central Portal Deployments](https://central.sonatype.com/publishing/deployments)
найдите созданный deployment. Проверьте состояние **Validated**, отсутствие
ошибок, координаты `io.github.kekaop`, версию `1.0.0` и ровно два компонента:
`uicore-parent` и `uicore-api`. Просмотрите POM, JAR, sources, Javadoc и подписи.
После разрешения владельца нажмите **Publish** и дождитесь **Published**.
Проверьте доступность обоих компонентов в Maven Central; она может появиться
не сразу.

После подтверждённой публикации другой плагин подключает API так:

```xml
<repositories>
  <repository>
    <id>papermc</id>
    <url>https://repo.papermc.io/repository/maven-public/</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>io.github.kekaop</groupId>
    <artifactId>uicore-api</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

На сервере должен быть установлен совместимый UIcore plugin. `provided`
предотвращает включение API в JAR расширения.

Требования и порядок работы сверены с документацией Sonatype:
[requirements](https://central.sonatype.org/publish/requirements/),
[Maven plugin](https://central.sonatype.org/publish/publish-portal-maven/),
[namespaces](https://central.sonatype.org/register/namespace/) и
[tokens](https://central.sonatype.org/publish/generate-portal-token/).
