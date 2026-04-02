# -*- mode: python ; coding: utf-8 -*-
"""PyInstaller spec for JPEG2PDF-OFD-OCR (onefile EXE)."""

import os
from pathlib import Path

PROJECT_ROOT = Path(SPECPATH)

a = Analysis(
    ['app.py'],
    pathex=[str(PROJECT_ROOT)],
    binaries=[],
    datas=[
        ('ui/index.html', 'ui'),
        ('settings/default.json', 'settings'),
    ],
    hiddenimports=[],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=['tkinter', 'matplotlib', 'numpy', 'scipy', 'PIL'],
    noarchive=False,
)

pyz = PYZ(a.pure)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.datas,
    [],
    name='JPEG2PDF-OFD-OCR',
    debug=False,
    console=False,
    # icon='assets/icon.ico',  # uncomment when icon exists
    onefile=True,
)
