# Сторонние компоненты в JAR

UIcore включает классы MorePaperLib, распространяемые по GNU LGPL v3 или более поздней версии. В `GracefulScheduling.java` добавлены методы для работы с Folia. Исходники включённых классов и способ собрать собственную изменённую версию JAR находятся в папке [`morepaperlib-source/`](morepaperlib-source/README_RU.md). Тексты LGPL v3 и GPL v3 находятся в `third-party/MOREPAPERLIB-LICENSE.txt` и `third-party/GPL-3.0.txt`, а также в `META-INF/licenses/` внутри JAR. Исходный проект: [A248/MorePaperLib](https://github.com/A248/MorePaperLib). Пользователь может изменять эту библиотеку и отлаживать её изменения в составе плагина.

UIcore включает RE2/J 1.8 для безопасной обработки регулярных выражений. Его лицензионные условия и уведомления сохранены в `META-INF/licenses/RE2J-LICENSE.txt` внутри JAR и в [копии для архива](third-party/RE2J-LICENSE.txt). Источник: [google/re2j](https://github.com/google/re2j).

Исходная кодовая база содержит классы bStats Metrics. В этой сборке они не запускают сбор статистики, но остаются в JAR. Уведомление MIT сохранено в `META-INF/licenses/BSTATS-METRICS-LICENSE.txt` и в [копии для архива](third-party/BSTATS-METRICS-LICENSE.txt). Источник: [Bastian/bstats-metrics](https://github.com/Bastian/bstats-metrics).

UIcore основан на [CustomScreenMenu автора JSGDJD](https://github.com/JSGDJD/CustomScreenMenu). Условия коммерческого распространения производной сборки должны соответствовать отдельному письменному разрешению правообладателя. Этот документ не заменяет коммерческую лицензию покупателя.
