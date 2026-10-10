# MorePaperLib source included with UIcore

> **Language:** English | [Русская версия](README_RU.md)

This folder contains the `space.arim.morepaperlib` classes included in UIcore. MorePaperLib is available under GNU LGPL v3 or later. Its source project is [A248/MorePaperLib](https://github.com/A248/MorePaperLib). The UIcore copy originates from the CustomScreenMenu source base; `GracefulScheduling.java` adds `isOnGlobalRegionThread()` and `cancelGlobalTasks()` for this release.

LGPL v3 and GPL v3 copies are in `../third-party/`. They apply to MorePaperLib and do not grant rights to UIcore source.

To build the modified library and relink a plugin JAR:

1. Install Java 17 or newer, Maven 3, and Python 3.
2. Edit files in `src/main/java/space/arim/morepaperlib/`.
3. Run `mvn -B -ntp clean package` in this folder.
4. Run `python relink.py ../UIcore.jar UIcore-relinked.jar`.

The script copies UIcore bytecode from the source JAR and replaces only MorePaperLib classes with the classes built in step 3. It leaves the source JAR unchanged. If you change MorePaperLib's public API, UIcore may need matching source changes.
