# CustomScreenMenu Test1 Baseline

This repository is a private working copy created from the currently tested
CustomScreenMenu jar in the OrvenCraft `test1` server.

## Baseline Jar

Source jar:

```text
E:\Minecraft servers\OrvenCraft\test1\plugins\CustomScreenMenu-1.4.8.jar
```

SHA256:

```text
514075FDB8CD56A3833CD5DC3F55EC4B93C22712D9E21A210951A2D18BA2AF3F
```

## Folder Layout

```text
original/              copied baseline jar
extracted/             raw jar contents
src-decompiled/        Vineflower decompiled Java/resources
server-data-current/   copied plugin data/config/menu files from test1
```

`server-data-current/users.db` and `server-data-current/blockcache/` are local
runtime state and are intentionally ignored.

## Notes

- This is decompiled third-party plugin code for private maintenance/testing.
- The active baseline is the jar listed above, not UIcore.
- Decompiled source may need manual cleanup before it can be rebuilt as a normal
  Maven/Gradle project.
- Keep this repository private unless license status is reviewed separately.
