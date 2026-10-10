"""Check the two Maven components prepared for Central without uploading them."""

import argparse
from pathlib import Path
import struct
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = "1.0.0"
GROUP = "io.github.kekaop"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def element_text(project, path):
    node = project.find(path, NS)
    return node.text.strip() if node is not None and node.text else None


def check_pom(path, artifact):
    project = ET.parse(path).getroot()
    require(element_text(project, "m:artifactId") == artifact, f"Wrong artifactId: {path}")
    require(element_text(project, "m:description"), f"Missing description: {path}")
    if artifact == "uicore-parent":
        require(element_text(project, "m:groupId") == GROUP, "Wrong parent groupId")
        require(element_text(project, "m:version") == VERSION, "Wrong parent version")
        require(element_text(project, "m:packaging") == "pom", "Parent must have pom packaging")
        require(element_text(project, "m:url"), "Missing project URL")
        require(project.find("m:licenses/m:license", NS) is not None, "Missing license")
        require(len(project.findall("m:developers/m:developer", NS)) >= 2, "Missing developers")
        require(element_text(project, "m:scm/m:connection"), "Missing SCM connection")
    else:
        require(element_text(project, "m:parent/m:groupId") == GROUP, "Wrong API parent groupId")
        require(element_text(project, "m:parent/m:artifactId") == "uicore-parent", "Wrong API parent")
        require(element_text(project, "m:parent/m:version") == VERSION, "Wrong API parent version")
        require(element_text(project, "m:packaging") == "jar", "API must have jar packaging")
        paper = next((dep for dep in project.findall("m:dependencies/m:dependency", NS)
                      if element_text(dep, "m:artifactId") == "paper-api"), None)
        require(paper is not None, "Paper API dependency missing")


def check_signature(path, signature):
    require(signature.is_file(), f"Missing signature: {signature}")
    subprocess.run(["gpg", "--batch", "--verify", str(signature), str(path)], check=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--require-signatures", action="store_true")
    args = parser.parse_args()

    parent_pom = ROOT / "pom.xml"
    api_pom = ROOT / "uicore-api" / "pom.xml"
    target = ROOT / "uicore-api" / "target"
    main_jar = target / f"uicore-api-{VERSION}.jar"
    sources_jar = target / f"uicore-api-{VERSION}-sources.jar"
    javadoc_jar = target / f"uicore-api-{VERSION}-javadoc.jar"

    check_pom(parent_pom, "uicore-parent")
    check_pom(api_pom, "uicore-api")
    for path in (main_jar, sources_jar, javadoc_jar):
        require(path.is_file() and path.stat().st_size > 0, f"Missing or empty artifact: {path}")

    with zipfile.ZipFile(main_jar) as jar:
        data = jar.read("org/orvencraft/uicore/api/UIcoreApi.class")
        require(data[:4] == b"\xca\xfe\xba\xbe", "Invalid API class file")
        require(struct.unpack(">H", data[6:8])[0] == 61, "API bytecode is not Java 17")
        require("org/orvencraft/uicore/UIcorePlugin.class" not in jar.namelist(),
                "Plugin implementation leaked into API JAR")
        require("META-INF/LICENSE" in jar.namelist(), "AGPL license is absent from API JAR")
    with zipfile.ZipFile(sources_jar) as jar:
        require("org/orvencraft/uicore/api/UIcoreApi.java" in jar.namelist(),
                "API source is absent")
    with zipfile.ZipFile(javadoc_jar) as jar:
        require("index.html" in jar.namelist(), "Javadoc index is absent")

    if args.require_signatures:
        check_signature(parent_pom, ROOT / "target" / f"uicore-parent-{VERSION}.pom.asc")
        check_signature(api_pom, target / f"uicore-api-{VERSION}.pom.asc")
        for path in (main_jar, sources_jar, javadoc_jar):
            check_signature(path, Path(f"{path}.asc"))
    print("Parent POM, API POM, Java 17 JAR, sources, Javadoc"
          + (", and GPG signatures" if args.require_signatures else "") + " verified.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, KeyError, ValueError, zipfile.BadZipFile, subprocess.CalledProcessError) as exc:
        print(f"Artifact check failed: {exc}", file=sys.stderr)
        sys.exit(1)
