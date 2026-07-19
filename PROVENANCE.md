# Provenance

This repository is a private OrvenCraft custom build workspace for CustomScreenMenu.

## Source baseline

- Upstream project: `JSGDJD/CustomScreenMenu`
- Upstream license: GNU Affero General Public License v3.0
- Functional baseline for this workspace: decompiled `CustomScreenMenu 1.4.8` test1 JAR with Orven dev20/dev21 changes.
- Embedded Maven metadata in the baseline JAR reports:
  - `groupId`: `com.example`
  - `artifactId`: `CustomScreenMenu`
  - `version`: `1.4.8`

No exact upstream `1.4.8` source tag was found during normalization. The archived decompiled dev21 state is preserved on branch:

`archive/decompiled-dev21-20260719`

## Orven build

The normalized source build uses:

- Maven multi-module layout.
- `customscreenmenu-api`: public API artifact.
- `customscreenmenu-plugin`: Paper plugin implementation and bundled resources.
- Version: `1.4.8-orven.1-SNAPSHOT`.
- Target Java: 25.
- Target server API: Paper `26.1.2.build.72-stable`.

This repository is private and must keep original license/provenance notices.
