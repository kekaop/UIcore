"""Replace LGPL MorePaperLib bytecode in a UIcore JAR after rebuilding it."""

from __future__ import annotations

import sys
import zipfile
from pathlib import Path


PREFIX = "space/arim/morepaperlib/"
ROOT = Path(__file__).resolve().parent
CLASSES = ROOT / "target" / "classes" / "space" / "arim" / "morepaperlib"


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("Usage: python relink.py <UIcore.jar> <UIcore-relinked.jar>")
    source = Path(sys.argv[1]).resolve()
    destination = Path(sys.argv[2]).resolve()
    if source == destination:
        raise SystemExit("Choose a new output path; the original JAR is not overwritten")
    if not source.is_file() or not CLASSES.is_dir():
        raise SystemExit("Missing UIcore.jar or compiled classes; run mvn clean package first")
    replacements = sorted(CLASSES.rglob("*.class"))
    if not replacements:
        raise SystemExit("No compiled MorePaperLib classes found")
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(
        destination, "w", compression=zipfile.ZIP_DEFLATED
    ) as rebuilt:
        original_names = set(original.namelist())
        if not any(name.startswith(PREFIX) and name.endswith(".class") for name in original_names):
            raise SystemExit("Input JAR contains no MorePaperLib classes")
        for entry in original.infolist():
            if entry.filename.startswith(PREFIX) and entry.filename.endswith(".class"):
                continue
            rebuilt.writestr(entry, original.read(entry.filename))
        for replacement in replacements:
            name = PREFIX + replacement.relative_to(CLASSES).as_posix()
            rebuilt.write(replacement, name)
    print(f"Rebuilt JAR: {destination}")
    print(f"MorePaperLib classes replaced: {len(replacements)}")


if __name__ == "__main__":
    main()
