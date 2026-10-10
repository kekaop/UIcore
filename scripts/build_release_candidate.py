"""Build and package a review-only UIcore binary release candidate."""

from __future__ import annotations

import hashlib
import os
import shutil
import subprocess
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
VERSION = "1.0.0"
JAR = ROOT / "uicore-plugin" / "target" / f"UIcore-{VERSION}.jar"
OUT = ROOT / "release" / "out" / f"UIcore-{VERSION}-RC.zip"


def main() -> None:
    maven = shutil.which("mvn.cmd" if os.name == "nt" else "mvn")
    if maven is None:
        raise SystemExit("Maven executable was not found on PATH")
    subprocess.run([maven, "-B", "-ntp", "clean", "verify"], cwd=ROOT, check=True)
    if not JAR.is_file():
        raise SystemExit(f"Missing plugin JAR: {JAR}")

    with zipfile.ZipFile(JAR) as plugin:
        entries = set(plugin.namelist())
        required = {
            "plugin.yml",
            "menu-resources.txt",
            "META-INF/licenses/RE2J-LICENSE.txt",
            "META-INF/licenses/BSTATS-METRICS-LICENSE.txt",
            "META-INF/licenses/MOREPAPERLIB-LICENSE.txt",
            "META-INF/licenses/GPL-3.0.txt",
        }
        if not required <= entries:
            raise SystemExit(f"Missing JAR resources: {sorted(required - entries)}")
        forbidden = [name for name in entries if name.startswith("menu/oc_") or "DragPanGesture" in name
                     or name.startswith("org/orvencraft/uicore/wasd/") or name == "wasd_config.yml"]
        if forbidden:
            raise SystemExit(f"Unwanted release entries: {sorted(forbidden)}")
        metadata = plugin.read("plugin.yml").decode("utf-8")
        if f"version: {VERSION}" not in metadata or "SNAPSHOT" in metadata:
            raise SystemExit("Incorrect plugin.yml version")

    files = {
        "docs/COMPATIBILITY_RU.md": ROOT / "docs" / "COMPATIBILITY_RU.md",
        "docs/MENU_AUTHORING_GUIDE_RU.md": ROOT / "docs" / "MENU_AUTHORING_GUIDE_RU.md",
        "docs/UIcore_GitBook.html": ROOT / "docs" / "UIcore_GitBook.html",
        "third-party/RE2J-LICENSE.txt": ROOT / "release" / "third-party" / "RE2J-LICENSE.txt",
        "third-party/BSTATS-METRICS-LICENSE.txt": ROOT / "release" / "third-party" / "BSTATS-METRICS-LICENSE.txt",
        "third-party/MOREPAPERLIB-LICENSE.txt": ROOT / "release" / "third-party" / "MOREPAPERLIB-LICENSE.txt",
        "third-party/GPL-3.0.txt": ROOT / "release" / "third-party" / "GPL-3.0.txt",
        "morepaperlib-source/pom.xml": ROOT / "release" / "morepaperlib-source" / "pom.xml",
        "morepaperlib-source/relink.py": ROOT / "release" / "morepaperlib-source" / "relink.py",
        "morepaperlib-source/README_RU.md": ROOT / "release" / "morepaperlib-source" / "README_RU.md",
    }
    library_sources = ROOT / "uicore-plugin" / "src" / "main" / "java" / "space" / "arim" / "morepaperlib"
    relink_sources = ROOT / "release" / "morepaperlib-source" / "src" / "main" / "java" / "space" / "arim" / "morepaperlib"
    for source in sorted(library_sources.rglob("*.java")):
        copied = relink_sources / source.relative_to(library_sources)
        if not copied.is_file() or copied.read_bytes() != source.read_bytes():
            raise SystemExit(f"MorePaperLib source bundle is out of sync: {source}")
        files[f"morepaperlib-source/src/main/java/space/arim/morepaperlib/{source.relative_to(library_sources).as_posix()}"] = copied
    for source in files.values():
        if not source.is_file():
            raise SystemExit(f"Missing package file: {source}")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    jar_hash = hashlib.sha256(JAR.read_bytes()).hexdigest()
    with zipfile.ZipFile(OUT, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as package:
        package.write(JAR, "UIcore.jar")
        for name, source in files.items():
            package.write(source, name)
        for name, source in {
            "README_RU.md": ROOT / "release" / "INSTALL_RU.md",
            "RELEASE_NOTES_RU.md": ROOT / "release" / "RELEASE_NOTES_RU.md",
            "THIRD_PARTY_NOTICES_RU.md": ROOT / "release" / "THIRD_PARTY_NOTICES_RU.md",
        }.items():
            text = source.read_text(encoding="utf-8").replace("../docs/", "docs/")
            package.writestr(name, text)
        package.writestr(
            "NOT_FOR_DISTRIBUTION.txt",
            "Internal review candidate. Add approved buyer terms and finish release gates before publication.\n",
        )
        package.writestr("SHA256SUMS.txt", f"{jar_hash}  UIcore.jar\n")

    with zipfile.ZipFile(OUT) as package:
        if package.testzip() is not None:
            raise SystemExit("Release candidate ZIP failed integrity check")
    print(f"Release candidate: {OUT}")
    print(f"UIcore.jar SHA-256: {jar_hash}")


if __name__ == "__main__":
    main()
