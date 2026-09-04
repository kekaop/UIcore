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
- `uicore-api`: public API artifact.
- `uicore-plugin`: Paper plugin implementation and bundled resources.
- Runtime name: `UIcore`.
- Version: `1.4.8-uicore.1-SNAPSHOT`.
- Target Java: 25.
- Target server API: Paper `26.1.2.build.72-stable`.

This repository is private and must keep original license/provenance notices.
