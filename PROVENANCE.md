# Provenance

This repository is a private build workspace maintained by Orven for a modified version of CustomScreenMenu.

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
- Version: `1.0.0`.
- Java bytecode: 17. Use the Java runtime required by the selected server build.
- Compile-time server API: Paper `1.20.1-R0.1-SNAPSHOT`.
- Tested runtime range: Minecraft 1.20.1–26.2 on Paper, Folia, Purpur, and available Leaf builds.

This repository is intended to be public. The upstream AGPLv3 license and provenance notices remain in the source workspace. The maintainer reports written permission from the upstream author to sell a patched version without source code, with an explicit restriction against BuiltByBit. Keep the correspondence in private release records and check the proposed sales channel and buyer-facing terms against it before publication.

On 2026-10-07, the author, `wuhaoxian123` on Discord, also agreed to publish the full UIcore source on GitHub under AGPL-3.0 after being told that it includes code decompiled from the paid CustomScreenMenu 1.4.8 build. The author asked for their Discord username and a link to the [original plugin post](https://builtbybit.com/resources/customscreenmenu.108929/) to be included in the attribution. The correspondence is retained privately; it is not part of this repository. This source-publication permission is separate from the earlier binary-sale permission and its marketplace restriction.

The plugin also embeds classes from MorePaperLib, licensed LGPLv3 or later. The binary release candidate carries the LGPL/GPL license texts and the source of those embedded classes, including the UIcore changes to `GracefulScheduling`, plus build and relink instructions. This does not include the source of UIcore itself. Review the separate LGPL obligations before sale.
