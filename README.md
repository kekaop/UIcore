# CustomScreenMenu Orven Build

Private OrvenCraft custom build of CustomScreenMenu.

## Build

```bash
mvn clean package
```

Artifacts:

- API: `customscreenmenu-api/target/customscreenmenu-api-1.4.8-orven.1-SNAPSHOT.jar`
- Plugin: `customscreenmenu-plugin/target/customscreenmenu-plugin-1.4.8-orven.1-SNAPSHOT.jar`

## Maven Coordinates

```xml
<dependency>
  <groupId>org.orvencraft.customscreenmenu</groupId>
  <artifactId>customscreenmenu-api</artifactId>
  <version>1.4.8-orven.1-SNAPSHOT</version>
</dependency>
```

## Runtime Target

- Java 25
- Paper `26.1.2`
- Required plugin: PacketEvents
- Optional plugins: PlaceholderAPI, FancyNpcs, SkinsRestorer, CraftEngine

## Notes

- `auth` remains part of this build.
- The archived dev21 decompiled snapshot is kept on `archive/decompiled-dev21-20260719`.
- Build outputs, unpacked JARs, local server state, and old patch folders are intentionally ignored on the normal source branch.
