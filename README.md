# UIcore Orven Build

Private OrvenCraft custom build of UIcore.

## Build

```bash
mvn clean package
```

Artifacts:

- API: `uicore-api/target/uicore-api-1.4.8-uicore.1-SNAPSHOT.jar`
- Plugin: `uicore-plugin/target/UIcore-1.4.8-uicore.1-SNAPSHOT.jar`

## Maven Coordinates

```xml
<dependency>
  <groupId>org.orvencraft.uicore</groupId>
  <artifactId>uicore-api</artifactId>
  <version>1.4.8-uicore.1-SNAPSHOT</version>
</dependency>
```

## Runtime Target

- Java 25
- Paper `26.1.2`
- Required plugin: PacketEvents
- Optional plugins: PlaceholderAPI, FancyNpcs, SkinsRestorer, CraftEngine

## Notes

- The archived dev21 decompiled snapshot is kept on `archive/decompiled-dev21-20260719`.
- Build outputs, unpacked JARs, local server state, and old patch folders are intentionally ignored on the normal source branch.
