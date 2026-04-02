"""Build pipeline for PDF-Converter-UI.

Steps:
  1. Maven build Java JAR  (main_new_ui)
  2. PyInstaller build Python EXE
  3. Combine into distributable package under dist/
"""

import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.parent
JAVA_PROJECT = PROJECT_ROOT.parent / 'main_new_ui'
DIST_DIR = PROJECT_ROOT / 'dist'
PACKAGE_NAME = 'JPEG2PDF-OFD-OCR'
JAR_NAME = 'jpeg2pdf-ofd-nospring-3.0.0.jar'
EXE_NAME = 'JPEG2PDF-OFD-OCR.exe'


def build_jar():
    """Run Maven clean package on the Java project."""
    print('[1/3] Building Java JAR ...')
    subprocess.run(
        ['mvn', 'clean', 'package', '-DskipTests'],
        cwd=str(JAVA_PROJECT),
        check=True,
    )
    jar = JAVA_PROJECT / 'target' / JAR_NAME
    if not jar.exists():
        print(f'ERROR: JAR not found at {jar}')
        sys.exit(1)
    print(f'  -> {jar}')


def build_exe():
    """Run PyInstaller to produce the Python EXE."""
    print('[2/3] Building Python EXE with PyInstaller ...')
    subprocess.run(
        [
            sys.executable, '-m', 'PyInstaller',
            'build/pdf-converter-ui.spec', '--clean', '--noconfirm',
        ],
        cwd=str(PROJECT_ROOT),
        check=True,
    )
    exe = DIST_DIR / EXE_NAME
    if not exe.exists():
        print(f'ERROR: EXE not found at {exe}')
        sys.exit(1)
    print(f'  -> {exe}')


def package():
    """Copy EXE + JAR into the final distributable folder."""
    print('[3/3] Packaging distributable ...')
    dist_app = DIST_DIR / PACKAGE_NAME
    if dist_app.exists():
        shutil.rmtree(dist_app)
    dist_app.mkdir(parents=True, exist_ok=True)

    # Copy EXE
    exe_src = DIST_DIR / EXE_NAME
    shutil.copy2(exe_src, dist_app / EXE_NAME)

    # Copy JAR
    jar_src = JAVA_PROJECT / 'target' / JAR_NAME
    shutil.copy2(jar_src, dist_app / JAR_NAME)

    # Create start.bat
    bat_content = '@echo off\r\ncd /d "%~dp0"\r\nstart "" "JPEG2PDF-OFD-OCR.exe"'
    (dist_app / 'start.bat').write_text(bat_content, encoding='ascii')

    print(f'  -> {dist_app}')
    print(f'Done! Distributable package: {dist_app}')


if __name__ == '__main__':
    build_jar()
    build_exe()
    package()
