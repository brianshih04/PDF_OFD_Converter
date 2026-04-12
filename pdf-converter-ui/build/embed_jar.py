"""Script to bundle the Java JAR next to the built Python EXE.

Run after PyInstaller build to copy the JAR into the dist folder
alongside the EXE, so the packaged app can find it at runtime.

Usage:
    python build/embed_jar.py [--jar-path <path-to-jar>]
"""

import argparse
import shutil
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.parent
JAVA_PROJECT = PROJECT_ROOT.parent
JAR_NAME = "jpeg2pdf-ofd-nospring-0.21.jar"
DIST_DIR = PROJECT_ROOT / "dist"
DEFAULT_JAR_SRC = JAVA_PROJECT / "target" / JAR_NAME


def embed(jar_src: Path, dest: Path | None = None):
    """Copy JAR to destination (defaults to dist/ next to EXE)."""
    if not jar_src.exists():
        print(f"ERROR: JAR not found at {jar_src}")
        print("Build the Java project first: mvn clean package -DskipTests")
        sys.exit(1)

    target = dest or DIST_DIR
    target.mkdir(parents=True, exist_ok=True)
    target_jar = target / JAR_NAME
    shutil.copy2(jar_src, target_jar)
    size_mb = target_jar.stat().st_size / (1024 * 1024)
    print(f"Embedded {JAR_NAME} ({size_mb:.1f} MB) -> {target_jar}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Bundle JAR into dist folder")
    parser.add_argument(
        "--jar-path",
        type=Path,
        default=DEFAULT_JAR_SRC,
        help="Path to the Java JAR file",
    )
    parser.add_argument(
        "--dest", type=Path, default=None, help="Destination directory (default: dist/)"
    )
    args = parser.parse_args()
    embed(args.jar_path, args.dest)
