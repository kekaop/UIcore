# Исходники MorePaperLib в UIcore

Эта папка содержит исходный код классов `space.arim.morepaperlib`, включённых в UIcore. Код MorePaperLib распространяется по GNU LGPL v3 или более поздней версии. Исходный проект: https://github.com/A248/MorePaperLib . Версия, встроенная в UIcore, взята из исходной кодовой базы CustomScreenMenu; в `GracefulScheduling.java` добавлены методы `isOnGlobalRegionThread()` и `cancelGlobalTasks()` для этого выпуска. Эта папка включает и остальной код библиотеки, чтобы можно было собрать её целиком.

Копии LGPL v3 и GPL v3 находятся в `../third-party/`. Эти условия относятся к MorePaperLib, а не дают права на исходники UIcore.

Чтобы собрать изменённую библиотеку и получить JAR плагина с ней:

1. Установите Java 17 или новее, Maven 3 и Python 3.
2. Измените файлы в `src/main/java/space/arim/morepaperlib/`.
3. Выполните в этой папке `mvn -B -ntp clean package`.
4. Выполните `python relink.py ../UIcore.jar UIcore-relinked.jar`.

Скрипт копирует объектный код UIcore из исходного JAR и заменяет только классы MorePaperLib на собранные в шаге 3. Он сохраняет исходный JAR без изменений. Итоговый JAR предназначен для вашей собственной проверки на сервере с обязательными зависимостями UIcore. Если вы изменили публичный API MorePaperLib, код UIcore может потребовать соответствующих изменений, которых этот комплект не содержит.
