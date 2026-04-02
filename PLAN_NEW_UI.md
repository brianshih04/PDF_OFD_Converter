# Plan: Restructure to Python+pywebview UI

**Date**: 2026-04-02
**Status**: Draft — Awaiting Architect Approval

---

## Executive Summary

Replace the JavaFX WebView GUI (`GuiApp.java`) with a Python application using `pywebview` (system-native Edge/Chromium on Windows). The Java CLI core is preserved as a standalone `mvn package` JAR artifact. Python communicates with the JAR via `subprocess`, parsing structured stdout for progress. The existing `index.html` frontend is reused with minimal modifications.

---

## Phase 0: Java Side — Remove GUI, Keep CLI Core

### 0.1 Files to DELETE

| File | Reason |
|------|--------|
| `src/main/java/com/ocr/nospring/GuiApp.java` | Entire JavaFX GUI application (824 lines) |
| `conveyor.conf` | Conveyor packaging config (JavaFX modules, GUI launcher) |
| `src/main/resources/web/index.html` | Moved to Python project (not needed in JAR) |
| `build.ps1` | Conveyor-based build script |
| `repack.ps1` | Conveyor repack script |
| `scripts/build.ps1` | jpackage-based build script |
| `scripts/build-exe.ps1` | Alternative build script |

### 0.2 Files to KEEP (untouched)

| File | Role |
|------|------|
| `Main.java` | CLI entry point — strip `--gui` launch path (lines 42-45), remove `GuiApp` import |
| `Config.java` | Configuration POJO (font, text layer, OCR language, etc.) |
| `OcrService.java` | RapidOCR engine wrapper |
| `TesseractOcrService.java` | Tesseract fallback OCR engine |
| `TesseractLanguageHelper.java` | Language-to-engine routing |
| `PdfService.java` | PDFBox PDF generation |
| `OfdService.java` | OFDRW OFD generation |
| `TextService.java` | Plain text output |
| `PdfToImagesService.java` | PDF-to-image rendering for searchable PDF mode |
| `ProcessingService.java` | Core orchestration with `ProgressCallback` interface |
| `PathValidator.java` | Path traversal prevention |

### 0.3 Files to KEEP (resources)

| File | Reason |
|------|--------|
| `src/main/resources/fonts/GoNotoKurrent-Regular.ttf` | Embedded CJK font |
| `src/main/resources/default.json` | Factory-default settings (reused by Python) |
| `src/main/resources/version.properties` | App version metadata |
| `src/main/resources/logback.xml` | Logging config |

### 0.4 pom.xml Changes

**Remove** (all JavaFX dependencies):
```xml
<dependency>org.openjfx:javafx-controls</dependency>
<dependency>org.openjfx:javafx-web</dependency>
<dependency>org.openjfx:javafx-swing</dependency>
```

**Remove** property:
```xml
<javafx.version>21.0.2</javafx.version>
```

**Remove** from maven-shade-plugin JavaFX exclusion filter (no longer needed since deps are removed).

**Keep** all other dependencies: PDFBox, OFDRW, RapidOCR, Tesseract, OpenCC4j, Jackson, SLF4J/Logback, JUnit 5, Mockito.

### 0.5 Main.java Modification

Strip the GUI launch branch at the top of `main()`:

```java
// BEFORE (lines 41-45):
if (args.length == 0 || (args.length == 1 && "--gui".equals(args[0]))) {
    GuiApp.launchGui(args);
    return;
}

// AFTER: no args = print usage
if (args.length == 0) {
    printUsage();
    System.exit(0);
}
```

Remove the `GuiApp` import. The `--gui` flag can either be removed or kept as a no-op warning.

### 0.6 Enhanced CLI Structured Output

Currently `ProcessingService` logs progress via SLF4J. For Python subprocess parsing, add **structured JSON progress lines** to `ProcessingService`. This is the key interface contract:

**New stdout protocol** — Java prints structured lines to stdout that Python parses:

```
{"type":"progress","current":3,"total":10,"message":"[3/10] photo.jpg"}
{"type":"progress","current":3,"total":10,"message":"  Image size: 3840x2160"}
{"type":"progress","current":3,"total":10,"message":"  Running OCR..."}
{"type":"progress","current":3,"total":10,"message":"  OK: OCR completed (42 blocks)"}
{"type":"progress","current":3,"total":10,"message":"  OK: PDF -> photo_20260402_153000.pdf"}
{"type":"complete","files":["C:\\OCR\\Output\\photo_20260402_153000.pdf","C:\\OCR\\Output\\photo_20260402_153000.ofd"]}
{"type":"error","message":"Cannot read image"}
```

**Implementation approach**: Add a `CliProgressCallback` in `Main.java` that:
1. Emits JSON lines to stdout (not log.info, which goes to stderr)
2. Preserves existing `log.info()` for diagnostic logging
3. Summary line (`Done!`) also emitted as JSON complete message

This means only `Main.java` and `ProcessingService.ProgressCallback` usage in `Main.java` change. The service layer itself is untouched.

### 0.7 Build Output

After cleanup, `mvn clean package -DskipTests` produces:
- `target/jpeg2pdf-ofd-nospring-3.0.0.jar` — fat JAR (~150MB with native OCR libs)
- No JavaFX runtime dependency
- Runs with `java -jar` (no Conveyor, no JDK bundling needed for CLI)

---

## Phase 1: Python UI Project Structure

### 1.1 Directory Layout

```
pdf-converter-ui/                  # New Python project (sibling or subdirectory)
├── app.py                         # Entry point — pywebview window + app lifecycle
├── requirements.txt               # Python dependencies
├── pyproject.toml                 # Project metadata (optional, for modern tooling)
├── README.md                      # (auto-generated, not hand-written)
│
├── ui/
│   ├── index.html                 # Migrated from Java resources (with modifications)
│   └── assets/                    # Future: icons, etc.
│
├── core/
│   ├── __init__.py
│   ├── config.py                  # Settings management (load/save/default/reset)
│   ├── bridge.py                  # Python-Java bridge (subprocess + stdout parsing)
│   └── converter.py               # Config JSON builder + subprocess invocation
│
├── api/
│   ├── __init__.py
│   └── js_api.py                  # JavaScript-callable Python functions (pywebview expose)
│
├── settings/
│   ├── default.json               # Copy of Java's default.json (factory defaults)
│   └── __init__.py
│
└── build/
    ├── build.py                   # PyInstaller build script
    ├── pdf-converter-ui.spec       # PyInstaller spec file
    └── embed_jar.py               # Script to bundle JAR into Python dist
```

### 1.2 Python Dependencies (`requirements.txt`)

```
pywebview>=5.0
```

That's the only runtime dependency. pywebview on Windows uses Edge/Chromium WebView2 (pre-installed on Windows 10/11).

**Build-time only**:
```
pyinstaller>=6.0
```

No Flask, no web server, no async framework needed. pywebview handles the native window.

---

## Phase 2: Core Python Modules — Detailed Design

### 2.1 `app.py` — Entry Point

```python
import webview
from api.js_api import JsApi

def main():
    api = JsApi()
    window = webview.create_window(
        title='JPEG2PDF-OFD OCR',
        url='ui/index.html',
        width=900,
        height=750,
        min_size=(800, 600),
        js_api=api,        # Exposed as window.pywebview.api
        text_select=True   # Allow text selection in WebView
    )
    webview.start(debug=False)  # True for dev

if __name__ == '__main__':
    main()
```

**Key difference from JavaFX**: pywebview's `js_api` replaces `window.javaApp`. All bridge calls go through `window.pywebview.api.*`.

### 2.2 `core/config.py` — Settings Management

Replaces `GuiApp.SettingsManager`. Manages the same JSON settings file:

```python
import json, os
from pathlib import Path

SETTINGS_DIR = Path.home() / '.jpeg2pdf-ofd'
SETTINGS_FILE = SETTINGS_DIR / 'settings.json'
DEFAULTS_FILE = Path(__file__).parent.parent / 'settings' / 'default.json'

class Settings:
    def __init__(self):
        SETTINGS_DIR.mkdir(parents=True, exist_ok=True)
        self._defaults = self._load_json(DEFAULTS_FILE)

    def load(self) -> dict:
        """Load user settings, falling back to defaults."""
        if not SETTINGS_FILE.exists():
            self.save(self._defaults)
            return dict(self._defaults)
        user = self._load_json(SETTINGS_FILE)
        merged = {**self._defaults, **user}
        return merged

    def save(self, settings: dict):
        """Persist settings to disk."""
        with open(SETTINGS_FILE, 'w', encoding='utf-8') as f:
            json.dump(settings, f, ensure_ascii=False, indent=2)

    def delete(self):
        """Delete user settings (revert to defaults on next load)."""
        SETTINGS_FILE.unlink(missing_ok=True)

    def load_defaults(self) -> dict:
        """Return factory defaults."""
        return dict(self._defaults)

    @staticmethod
    def _load_json(path: Path) -> dict:
        with open(path, 'r', encoding='utf-8') as f:
            return json.load(f)
```

**Backward compatible**: Uses the same `~/.jpeg2pdf-ofd/settings.json` path and `default.json` schema as the Java version. Users upgrading from the JavaFX version keep their settings.

### 2.3 `core/bridge.py` — Java CLI Subprocess Bridge

This is the most critical module. It launches the Java JAR as a subprocess and parses structured JSON progress from stdout.

```python
import subprocess, json, threading
from pathlib import Path
from typing import Callable, Optional

JAR_PATH = Path(__file__).parent.parent.parent / 'target' / 'jpeg2pdf-ofd-nospring-3.0.0.jar'
# For packaged builds, JAR is bundled alongside the EXE
PACKAGED_JAR_PATH = Path(__file__).parent.parent / 'jpeg2pdf-ofd-nospring-3.0.0.jar'

class ConversionBridge:
    def __init__(self):
        self._process: Optional[subprocess.Popen] = None
        self._cancelled = False
        self.jar_path = PACKAGED_JAR_PATH if PACKAGED_JAR_PATH.exists() else JAR_PATH

    def start_conversion(
        self,
        config_json: str,
        on_progress: Callable[[int, int, str], None],
        on_complete: Callable[[list[str]], None],
        on_error: Callable[[str], None],
        on_log: Callable[[str], None],
    ):
        """Run Java CLI in a background thread."""
        self._cancelled = False
        thread = threading.Thread(
            target=self._run,
            args=(config_json, on_progress, on_complete, on_error, on_log),
            daemon=True
        )
        thread.start()

    def _run(self, config_json, on_progress, on_complete, on_error, on_log):
        # Write temp config file for Java CLI
        config_path = self._write_temp_config(config_json)

        try:
            self._process = subprocess.Popen(
                ['java', '-Xmx2G', '-jar', str(self.jar_path), str(config_path)],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                encoding='utf-8',
                bufsize=1,          # Line-buffered
                cwd=str(self.jar_path.parent)
            )

            # Parse stdout line by line for structured JSON progress
            output_files = []
            for line in self._process.stdout:
                line = line.strip()
                if not line:
                    continue

                # Try parsing as structured JSON progress
                try:
                    msg = json.loads(line)
                    msg_type = msg.get('type', '')

                    if msg_type == 'progress':
                        on_progress(msg['current'], msg['total'], msg['message'])
                    elif msg_type == 'complete':
                        output_files = msg.get('files', [])
                    elif msg_type == 'error':
                        on_error(msg['message'])
                        return
                    else:
                        on_log(line)
                except json.JSONDecodeError:
                    # Not JSON — pass through as log
                    on_log(line)

            # Wait for process to finish
            returncode = self._process.wait(timeout=30)

            if returncode == 0 and output_files:
                on_complete(output_files)
            elif returncode != 0:
                stderr = self._process.stderr.read()
                on_error(f"Process exited with code {returncode}: {stderr[:500]}")
            elif not self._cancelled:
                on_complete([])

        except Exception as e:
            on_error(str(e))
        finally:
            config_path.unlink(missing_ok=True)
            self._process = None

    def cancel(self):
        if self._process:
            self._cancelled = True
            self._process.terminate()
            try:
                self._process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self._process.kill()

    def _write_temp_config(self, config_json: str) -> Path:
        """Write the frontend config JSON to a temp file for Java CLI consumption."""
        import tempfile
        config = json.loads(config_json)

        # Transform frontend config format to Java CLI config format
        java_config = self._transform_config(config)

        tmp = Path(tempfile.gettempdir()) / 'jpeg2pdf_conversation_config.json'
        with open(tmp, 'w', encoding='utf-8') as f:
            json.dump(java_config, f, ensure_ascii=False, indent=2)
        return tmp

    def _transform_config(self, frontend_config: dict) -> dict:
        """Transform UI config JSON to Java CLI config.json format.

        Frontend sends:
          { inputType, inputPath, outputPath, language, formats, multiPage,
            ocrEngine, tesseractDataPath, fontMode, customFontPath,
            textColor, textOpacity, chineseConversion }

        Java CLI expects:
          { input: { type, folder|file }, output: { folder, format(s), multiPage },
            ocr: { language, engine, tesseractDataPath },
            font: { path }, textLayer: { color, opacity }, textConvert }
        """
        input_section = {}
        if frontend_config.get('inputType') == 'pdf':
            input_section['type'] = 'pdf'
            input_section['file'] = frontend_config.get('inputPath')
        else:
            input_section['type'] = 'image'
            input_section['folder'] = frontend_config.get('inputPath')

        output_section = {
            'folder': frontend_config.get('outputPath'),
            'format': frontend_config.get('formats', 'pdf'),
            'multiPage': frontend_config.get('multiPage', False)
        }

        ocr_section = {
            'language': frontend_config.get('language', 'chinese_cht'),
            'engine': frontend_config.get('ocrEngine', 'auto')
        }
        if frontend_config.get('tesseractDataPath'):
            ocr_section['tesseractDataPath'] = frontend_config['tesseractDataPath']

        java_config = {
            'input': input_section,
            'output': output_section,
            'ocr': ocr_section
        }

        # Optional sections
        font_mode = frontend_config.get('fontMode', 'auto')
        if font_mode == 'custom' and frontend_config.get('customFontPath'):
            java_config['font'] = {'path': frontend_config['customFontPath']}

        text_color = frontend_config.get('textColor')
        if text_color:
            java_config['textLayer'] = {'color': text_color}

        text_opacity = frontend_config.get('textOpacity')
        if text_opacity is not None:
            java_config.setdefault('textLayer', {})['opacity'] = text_opacity

        chinese_conversion = frontend_config.get('chineseConversion')
        if chinese_conversion and chinese_conversion != 'null':
            java_config['textConvert'] = chinese_conversion

        return java_config
```

### 2.4 `api/js_api.py` — JavaScript-Python Bridge

Replaces `GuiApp.JavaBridge`. Exposed to JavaScript via pywebview's `js_api`.

```python
import json, os, subprocess
from pathlib import Path
from core.config import Settings
from core.bridge import ConversionBridge

class JsApi:
    def __init__(self):
        self.settings = Settings()
        self.bridge = ConversionBridge()
        self._progress_callback = None
        self._complete_callback = None
        self._error_callback = None
        self._log_callback = None

    # === File Dialogs ===

    def open_directory_chooser(self, current_path: str = '') -> str:
        """Open native directory picker dialog."""
        folder = webview.windows[0].create_file_dialog(
            dialog_type=webview.FOLDER_DIALOG,
            directory=current_path or os.path.expanduser('~')
        )
        return folder[0] if folder else ''

    def open_file_chooser(self) -> str:
        """Open native file picker for PDF files."""
        result = webview.windows[0].create_file_dialog(
            dialog_type=webview.OPEN_DIALOG,
            file_types=('PDF Files (*.pdf)',)
        )
        return result[0] if result else ''

    def open_font_file_chooser(self) -> str:
        """Open native file picker for TTF font files."""
        result = webview.windows[0].create_file_dialog(
            dialog_type=webview.OPEN_DIALOG,
            file_types=('Font Files (*.ttf)',)
        )
        return result[0] if result else ''

    # === Conversion ===

    def start_conversion(self, config_json: str):
        """Start conversion via Java CLI subprocess."""
        self.bridge.start_conversion(
            config_json=config_json,
            on_progress=self._on_progress,
            on_complete=self._on_complete,
            on_error=self._on_error,
            on_log=self._on_log,
        )

    def cancel_conversion(self):
        """Cancel the running conversion."""
        self.bridge.cancel()

    # === Settings ===

    def load_settings(self) -> str:
        return json.dumps(self.settings.load(), ensure_ascii=False)

    def save_settings(self, settings_json: str):
        self.settings.save(json.loads(settings_json))

    def load_default_settings(self) -> str:
        return json.dumps(self.settings.load_defaults(), ensure_ascii=False)

    def delete_settings(self):
        self.settings.delete()

    def get_version(self) -> str:
        return '3.0.0 (pywebview)'

    # === Callbacks to JavaScript ===

    def _on_progress(self, current, total, message):
        window = webview.windows[0]
        window.evaluate_js(
            f'window.pythonBridge.onProgress({current},{total},'
            f'{json.dumps(message, ensure_ascii=False)})'
        )

    def _on_complete(self, files):
        window = webview.windows[0]
        window.evaluate_js(
            f'window.pythonBridge.onComplete({json.dumps(files, ensure_ascii=False)})'
        )

    def _on_error(self, message):
        window = webview.windows[0]
        window.evaluate_js(
            f'window.pythonBridge.onError({json.dumps(message, ensure_ascii=False)})'
        )

    def _on_log(self, message):
        window = webview.windows[0]
        window.evaluate_js(
            f'window.pythonBridge.onLog({json.dumps(message, ensure_ascii=False)})'
        )
```

**Note**: pywebview JS API methods must return simple types (str, int, bool, list, dict). Complex objects and callbacks require `window.evaluate_js()` for the reverse direction.

---

## Phase 3: Frontend Migration (`index.html`)

### 3.1 Changes Required

The existing 1532-line `index.html` is ~90% reusable. Changes are limited to replacing the Java bridge API:

| Current (JavaFX) | New (pywebview) | Scope |
|---|---|---|
| `window.javaApp.openDirectoryChooser(path)` | `window.pywebview.api.open_directory_chooser(path)` | All bridge calls |
| `window.javaApp.openFileChooser()` | `window.pywebview.api.open_file_chooser()` | 1 call |
| `window.javaApp.openFontFileChooser()` | `window.pywebview.api.open_font_file_chooser()` | 1 call |
| `window.javaApp.startConversion(json)` | `window.pywebview.api.start_conversion(json)` | 1 call |
| `window.javaApp.cancelConversion()` | `window.pywebview.api.cancel_conversion()` | 1 call |
| `window.javaApp.saveSettings(json)` | `window.pywebview.api.save_settings(json)` | 2 calls |
| `window.javaApp.loadSettings()` | `window.pywebview.api.load_settings()` | 1 call (async!) |
| `window.javaApp.loadDefaultSettings()` | `window.pywebview.api.load_default_settings()` | 2 calls (async!) |
| `window.javaApp.deleteSettings()` | `window.pywebview.api.delete_settings()` | 2 calls |
| `window.javaApp.getVersion()` | `window.pywebview.api.get_version()` | 0 calls currently |
| `window.javaBridge.onProgress(...)` | `window.pythonBridge.onProgress(...)` | Callback object name |
| `window.javaBridge.onComplete(...)` | `window.pythonBridge.onComplete(...)` | Callback object name |
| `window.javaBridge.onError(...)` | `window.pythonBridge.onError(...)` | Callback object name |
| `window.javaBridge.onLog(...)` | `window.pythonBridge.onLog(...)` | Callback object name |

### 3.2 Critical: Async Bridge Calls

pywebview's JS API calls are **asynchronous** (returning `Promise`). The current `index.html` uses synchronous calls (e.g., `var path = window.javaApp.openDirectoryChooser(...)`). This requires converting to `await`:

```javascript
// BEFORE (synchronous JavaFX bridge):
function browseInput() {
    var isFolder = document.querySelector('input[name="inputType"]:checked').value === 'folder';
    var path;
    if (isFolder) {
        path = window.javaApp.openDirectoryChooser(currentPath);
    } else {
        path = window.javaApp.openFileChooser();
    }
    if (path) { ... }
}

// AFTER (async pywebview bridge):
async function browseInput() {
    var isFolder = document.querySelector('input[name="inputType"]:checked').value === 'folder';
    var path;
    if (isFolder) {
        path = await window.pywebview.api.open_directory_chooser(currentPath);
    } else {
        path = await window.pywebview.api.open_file_chooser();
    }
    if (path) { ... }
}
```

**All bridge-calling functions must be made async**: `browseInput`, `browseOutput`, `browseTesseractDataPath`, `browseDefaultOutputFolder`, `browseCustomFont`, `startConversion`, `cancelConversion`, `loadSettings`, `resetSettings`, `resetAllSettings`.

### 3.3 Loading Overlay Change

The Java bridge polling mechanism (lines 1481-1528) can be simplified. pywebview exposes the API before the page loads, so:

```javascript
// AFTER: pywebview API is available immediately (no polling needed)
document.addEventListener('DOMContentLoaded', async function() {
    addLog(t('msg_interface_loaded'));
    await loadSettings();
    hideLoadingOverlay();
});
```

### 3.4 Summary of HTML Changes

1. **Global find-replace**: `window.javaApp.` → `window.pywebview.api.` (method name mapping as above)
2. **Callback object rename**: `window.javaBridge` → `window.pythonBridge`
3. **Async conversion**: Add `async` to ~12 functions, add `await` before bridge calls
4. **Remove bridge polling**: Replace initialization block with simple `DOMContentLoaded` + `await loadSettings()`
5. **No CSS/HTML structural changes**: Layout, styling, i18n system are untouched

---

## Phase 4: Packaging with PyInstaller

### 4.1 Build Strategy

Two-artifact distribution:
1. **Java JAR** (built via Maven) — the CLI engine
2. **Python EXE** (built via PyInstaller) — the UI shell that wraps the JAR

### 4.2 Directory Structure After Build

```
dist/
├── JPEG2PDF-OFD-OCR/                  # Distributable folder
│   ├── JPEG2PDF-OFD-OCR.exe           # PyInstaller onefile EXE
│   ├── jpeg2pdf-ofd-nospring-3.0.0.jar  # Java CLI engine
│   └── start.bat                      # Simple launcher
```

### 4.3 PyInstaller Spec (`pdf-converter-ui.spec`)

```python
a = Analysis(
    ['app.py'],
    pathex=[],
    binaries=[],
    datas=[
        ('ui/index.html', 'ui'),
        ('settings/default.json', 'settings'),
    ],
    hiddenimports=[],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=['tkinter', 'matplotlib', 'numpy', 'scipy'],
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
    console=False,       # GUI app, no console window
    icon='assets/icon.ico',
    onefile=True,
)
```

### 4.4 Build Script (`build/build.py`)

```python
"""
Build pipeline:
1. Maven build Java JAR
2. PyInstaller build Python EXE
3. Combine into distributable package
"""
import subprocess, shutil, sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.parent
JAVA_PROJECT = PROJECT_ROOT.parent / 'main_new_ui'
DIST_DIR = PROJECT_ROOT / 'dist'
PACKAGE_NAME = 'JPEG2PDF-OFD-OCR'

def build_jar():
    subprocess.run(
        ['mvn', 'clean', 'package', '-DskipTests'],
        cwd=str(JAVA_PROJECT), check=True
    )

def build_exe():
    subprocess.run(
        [sys.executable, '-m', 'PyInstaller', 'build/pdf-converter-ui.spec', '--clean', '--noconfirm'],
        cwd=str(PROJECT_ROOT), check=True
    )

def package():
    dist_app = DIST_DIR / PACKAGE_NAME
    dist_app.mkdir(parents=True, exist_ok=True)

    # Copy EXE
    exe_src = DIST_DIR / 'JPEG2PDF-OFD-OCR.exe'
    shutil.copy2(exe_src, dist_app / 'JPEG2PDF-OFD-OCR.exe')

    # Copy JAR
    jar_src = JAVA_PROJECT / 'target' / 'jpeg2pdf-ofd-nospring-3.0.0.jar'
    shutil.copy2(jar_src, dist_app / 'jpeg2pdf-ofd-nospring-3.0.0.jar')

    # Create start.bat
    bat_content = '@echo off\r\ncd /d "%~dp0"\r\nstart "" "JPEG2PDF-OFD-OCR.exe"'
    (dist_app / 'start.bat').write_text(bat_content, encoding='ascii')

    print(f'Package created: {dist_app}')

if __name__ == '__main__':
    build_jar()
    build_exe()
    package()
```

### 4.5 JAR Path Resolution in Packaged Mode

The `ConversionBridge` class resolves the JAR path with this priority:
1. **Packaged mode**: JAR next to the EXE (`sys._MEIPASS` / `Path(__file__).parent`)
2. **Dev mode**: JAR at `../main_new_ui/target/...` (relative to Python project root)

---

## Phase 5: Java CLI Enhancement — Structured Progress Output

### 5.1 New `CliProgressCallback` in `Main.java`

```java
private static class CliProgressCallback implements ProcessingService.ProgressCallback {
    private final ObjectMapper mapper = new ObjectMapper();

    private void emitJson(Map<String, Object> msg) {
        try {
            System.out.println(mapper.writeValueAsString(msg));
            System.out.flush();
        } catch (Exception e) {
            System.err.println("Failed to emit progress: " + e.getMessage());
        }
    }

    @Override
    public void onProgress(int current, int total, String message) {
        Map<String, Object> msg = Map.of(
            "type", "progress",
            "current", current,
            "total", total,
            "message", message
        );
        emitJson(msg);
    }

    @Override
    public void onComplete(List<String> outputFiles) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "complete");
        msg.put("files", outputFiles);
        emitJson(msg);
    }

    @Override
    public void onError(String message) {
        Map<String, Object> msg = Map.of(
            "type", "error",
            "message", message
        );
        emitJson(msg);
    }
}
```

### 5.2 Integration Point in `Main.java`

Replace `null` callback arguments (lines 247, 249, 251) with the new `CliProgressCallback`:

```java
ProcessingService.ProgressCallback callback = new CliProgressCallback();

if ("pdf".equals(inputType)) {
    processingService.processPdfToSearchable(inputFiles, outputDir, format, language, ocrEngine, renderDpi, callback);
} else if (multiPage) {
    processingService.processMultiPage(inputFiles, outputDir, format, language, ocrEngine, callback);
} else {
    processingService.processPerPage(inputFiles, outputDir, format, language, ocrEngine, callback);
}
```

---

## Implementation Order & Dependencies

```
Phase 0.1-0.6 (Java cleanup)     ──┐
                                    ├── Phase 2-3 (Python UI) ── Phase 4 (Packaging)
Phase 0.7 (Java CLI progress)    ──┘
```

| Step | Description | Depends On | Estimated Files Changed |
|------|-------------|------------|------------------------|
| 0.1 | Delete GuiApp.java + conveyor.conf | — | 2 deleted |
| 0.2 | Clean pom.xml (remove JavaFX deps) | 0.1 | 1 file |
| 0.3 | Strip GUI launch from Main.java | 0.1 | 1 file |
| 0.4 | Add CliProgressCallback to Main.java | 0.3 | 1 file |
| 0.5 | Remove web/index.html from resources | 0.1 | 1 deleted |
| 0.6 | Test CLI build + structured output | 0.4 | 0 (verification) |
| 1.1 | Create Python project skeleton | 0.6 | ~8 new files |
| 2.1 | Implement `core/config.py` | 1.1 | 1 new file |
| 2.2 | Implement `core/bridge.py` | 1.1 | 1 new file |
| 2.3 | Implement `api/js_api.py` | 2.1, 2.2 | 1 new file |
| 2.4 | Implement `app.py` | 2.3 | 1 new file |
| 3.1 | Migrate index.html to Python project | 2.4 | 1 new file |
| 3.2 | Modify index.html bridge calls | 3.1 | 1 file (modifications) |
| 4.1 | PyInstaller spec + build script | 3.2 | 3 new files |
| 4.2 | End-to-end packaging test | 4.1 | 0 (verification) |

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| pywebview async bridge breaks sync JS patterns | High — all browse/start functions need async rewrite | Systematic find-replace + `async/await` conversion; thorough testing |
| Java CLI stdout buffering delays progress | Medium — progress lines may not appear in real-time | Use `bufsize=1` (line-buffered) + `System.out.flush()` in Java callback |
| Large JAR (~150MB) bloats distribution | Low — same as before (Conveyor bundle was similar size) | Consider `jlink` to slim JRE, but out of scope for initial migration |
| WebView2 not available on old Windows | Low — WebView2 is pre-installed on Win10 21H2+ and auto-updates | pywebview can bundle a fixed WebView2 Runtime |
| `create_file_dialog` API differs from JavaFX `FileChooser` | Medium — dialog behavior may vary | Test on target Windows versions; pywebview dialogs are native |
| Path encoding issues (Chinese paths) | Medium — common in CJK environments | Use `encoding='utf-8'` consistently in Python subprocess; Java already handles this |

---

## Testing Checklist

- [ ] Java CLI: `mvn clean package -DskipTests` succeeds without JavaFX
- [ ] Java CLI: `java -jar target/*.jar config.json` produces JSON progress lines on stdout
- [ ] Java CLI: Cancel via Ctrl+C terminates cleanly
- [ ] Python UI: pywebview window opens with correct HTML
- [ ] Python UI: Browse buttons open native file dialogs
- [ ] Python UI: Settings load/save/delete round-trips correctly
- [ ] Python UI: Start conversion invokes Java CLI subprocess
- [ ] Python UI: Progress bar updates in real-time from Java stdout
- [ ] Python UI: Cancel button terminates Java subprocess
- [ ] Python UI: Completion shows output file list
- [ ] Python UI: Error handling shows error messages
- [ ] Python UI: i18n language switching works
- [ ] Packaging: PyInstaller EXE + JAR runs as portable distribution
- [ ] Regression: Same config.json produces same output as old JavaFX version
