# Публикация UIcore API в Maven Central

**Статус версии 1.0.0: опубликована.** Deployment
`587d6925-332d-4b7b-963b-452928d71503` имеет состояние `PUBLISHED`.
[Публичный POM](https://repo.maven.apache.org/maven2/io/github/kekaop/uicore-api/1.0.0/uicore-api-1.0.0.pom),
JAR, sources, Javadoc и подписи доступны в Maven Central. Тег исходников:
`api-v1.0.0-r2`. Повторно загружать эту версию нельзя: компоненты Maven Central
неизменяемы.

Этот документ предназначен владельцу репозитория. Подготовленная конфигурация
выпускает **один** компонент версии `1.0.0`: `uicore-api` (JAR, sources и Javadoc).
Для публикации создаётся отдельный POM с полными метаданными и без ссылки на
`uicore-parent`, поэтому потребителю достаточно одного компонента.
`uicore-parent`, `uicore-plugin` и `uicore-example-extension` не публикуются.
Java package names остаются `org.orvencraft.uicore`.

Обычный `mvn clean verify` не публикует ничего. Профили `central-release,paper-build`
создают потребительский POM и подписи и подключают официальный
`central-publishing-maven-plugin` версии `0.11.0`.
Плагин формирует контрольные суммы. `autoPublish=false` оставляет окончательное
решение о публикации владельцу в Central Portal.

Для локальной проверки можно установить parent и API в Maven-репозиторий компьютера:

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

Для полной сборки нужен JDK 21 или новее. Для выпуска используйте Maven 3.9.16:
workflow закрепляет эту версию, поскольку Maven 3.10.0 создаёт несовместимые
metadata-файлы при работе `central-publishing-maven-plugin:0.11.0`. API остаётся
скомпилированным для Java 17 (`release 17`). В корне репозитория выполните:

```bash
mvn -B -ntp clean verify
mvn -B -ntp -Pcentral-release,paper-build -pl uicore-api -am verify -Dgpg.skip=true
python3 scripts/check-central-artifacts.py
```

Вторая команда собирает только parent POM и API с sources и Javadoc. Параметр
`-Dgpg.skip=true` нужен лишь для локального просмотра без приватного ключа;
**не используйте его при deploy**. Проверка подписей, Portal validation и
разрешение опубликованной зависимости окончательно проверяются только с настоящими
ключом, токеном и доступом к Portal.

В исходном API POM профиль `paper-build` подключает
`paper-api:1.21.1-R0.1-SNAPSHOT` для компиляции. Публикуемый POM не содержит
snapshot-зависимостей и ссылки на parent. Компилирующему расширение проекту
нужно самому объявить `paper-api` с `provided` scope и репозиторий Paper.
После загрузки обязательно проверьте результат валидации Portal и соберите
внешний проект с опубликованными POM и JAR.

## 4. Как был запущен выпуск 1.0.0

Тег `v1.0.0` занят существующим GitHub-релизом плагина. Тег `api-v1.0.0`
фиксирует первую попытку подготовки API, которую Portal отклонил. Успешный выпуск
собран из `api-v1.0.0-r2`. Для него использованы команды:

```bash
git tag api-v1.0.0-r2 151d884
git push origin api-v1.0.0-r2
```

Workflow запущен из default branch; он загрузил содержимое указанного тега:

```bash
gh workflow run publish-api.yml --ref orven/normal-source-build -f release_tag=api-v1.0.0-r2
gh run watch
```

Workflow не запускается при обычном push и завершается без работы при указании
иного тега. Он сверяет тег, `groupId`, версию `1.0.0`, запускает полную
сборку и тесты, проверяет JAR, sources, Javadoc, POM и GPG-подписи. После этого
Maven загружает bundle в Portal с ручным выпуском.

Для следующей версии обновите версию, тег и проверки workflow, затем повторите
процедуру. Текущую версию повторно не загружайте.

Для ручной загрузки с локального компьютера настройте `~/.m2/settings.xml` с
`<server><id>central</id>` и частями publishing token в `username` и `password`.
Импортируйте приватный GPG-ключ в локальный keyring, передайте парольную фразу
через `MAVEN_GPG_PASSPHRASE` или подготовленный `gpg-agent`, затем выполните:

```bash
mvn -B -ntp clean verify
mvn -B -ntp -Pcentral-release,paper-build -pl uicore-api -am verify
python3 scripts/check-central-artifacts.py --require-signatures
mvn -B -ntp -Pcentral-release,paper-build -pl uicore-api -am deploy -DskipTests
```

Последняя команда **загружает** bundle в Portal. Для опубликованной версии `1.0.0`
её повторять нельзя. Она не нажимает **Publish** в Portal.

## 5. Проверьте deployment и подключите API

В [Central Portal Deployments](https://central.sonatype.com/publishing/deployments)
найдите deployment `587d6925-332d-4b7b-963b-452928d71503`. Его состояние —
**Published**; в нём ровно один компонент `io.github.kekaop:uicore-api:1.0.0`.
Публичные POM, JAR, sources, Javadoc и подписи доступны в Maven Central.

Другой плагин подключает API так:

```xml
<repositories>
  <repository>
    <id>papermc</id>
    <url>https://repo.papermc.io/repository/maven-public/</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>paper-api</artifactId>
    <version>1.21.1-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
  </dependency>
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
