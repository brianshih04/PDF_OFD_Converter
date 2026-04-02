"""Java CLI subprocess bridge with structured JSON stdout parsing.

Launches the Java JAR as a subprocess and parses structured JSON progress
lines from stdout. Also handles config transform from the frontend format
to the Java CLI config.json format.
"""

import json
import subprocess
import sys
import tempfile
import threading
from pathlib import Path
from typing import Callable, Optional


def _get_base_path() -> Path:
    """Return the project root, accounting for PyInstaller bundling."""
    if getattr(sys, 'frozen', False):
        # Packaged (PyInstaller): base dir is next to the EXE
        return Path(sys.executable).parent
    # Dev mode: project root is two levels up from this file
    return Path(__file__).parent.parent.parent


JAR_NAME = 'jpeg2pdf-ofd-nospring-3.0.0.jar'
JAR_DEV_PATH = _get_base_path() / 'target' / JAR_NAME
JAR_PACKAGED_PATH = _get_base_path() / JAR_NAME


class ConversionBridge:
    def __init__(self):
        self._process: Optional[subprocess.Popen] = None
        self._cancelled = False
        self._jar_path = self._resolve_jar()

    @property
    def jar_path(self) -> str:
        """JAR path as string (avoid pywebview Path serialization issues)."""
        return str(self._jar_path)

    def _resolve_jar(self) -> Path:
        """Resolve JAR path: packaged mode (next to EXE) or dev mode."""
        if JAR_PACKAGED_PATH.exists():
            return JAR_PACKAGED_PATH
        if JAR_DEV_PATH.exists():
            return JAR_DEV_PATH
        # Return packaged path as default even if not found yet (may appear later)
        return JAR_PACKAGED_PATH

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
            daemon=True,
        )
        thread.start()

    def _run(self, config_json, on_progress, on_complete, on_error, on_log):
        config_path = self._write_temp_config(config_json)

        try:
            self._process = subprocess.Popen(
                ['java', '-Xmx2G', '-jar', str(self.jar_path), str(config_path)],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                encoding='utf-8',
                bufsize=1,  # Line-buffered
                cwd=str(self.jar_path.parent),
            )

            output_files = []
            for raw_line in self._process.stdout:
                line = raw_line.strip()
                if not line:
                    continue

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
                    on_log(line)

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

    # ------------------------------------------------------------------
    # Config transform
    # ------------------------------------------------------------------

    def _write_temp_config(self, config_json: str) -> Path:
        """Write the transformed Java CLI config JSON to a temp file."""
        config = json.loads(config_json)
        java_config = self._transform_config(config)

        tmp = Path(tempfile.gettempdir()) / 'jpeg2pdf_conversion_config.json'
        with open(tmp, 'w', encoding='utf-8') as f:
            json.dump(java_config, f, ensure_ascii=False, indent=2)
        return tmp

    @staticmethod
    def _transform_config(frontend_config: dict) -> dict:
        """Transform frontend config to Java CLI config.json format.

        Frontend sends (UI settings shape):
          {inputType, inputPath, outputPath, language, formats, multiPage,
           ocrEngine, tesseractDataPath, fontMode, customFontPath,
           textColor, textOpacity, chineseConversion}

        Java CLI expects:
          {input: {type, folder|file}, output: {folder, format(s), multiPage},
           ocr: {language, engine, tesseractDataPath},
           font: {path}, textLayer: {color, opacity}, textConvert}
        """
        # --- input ---
        input_section: dict = {}
        if frontend_config.get('inputType') == 'pdf':
            input_section['type'] = 'pdf'
            input_section['file'] = frontend_config.get('inputPath')
        else:
            input_section['type'] = 'image'
            input_section['folder'] = frontend_config.get('inputPath')

        # --- output ---
        output_section = {
            'folder': frontend_config.get('outputPath'),
            'format': frontend_config.get('formats', 'pdf'),
            'multiPage': str(frontend_config.get('multiPage', False)).lower() == 'true'
                if isinstance(frontend_config.get('multiPage'), str)
                else bool(frontend_config.get('multiPage', False)),
        }

        # --- ocr ---
        ocr_section: dict = {
            'language': frontend_config.get('language', 'chinese_cht'),
            'engine': frontend_config.get('ocrEngine', 'auto'),
        }
        tesseract_data = frontend_config.get('tesseractDataPath')
        if tesseract_data:
            ocr_section['tesseractDataPath'] = tesseract_data

        java_config: dict = {
            'input': input_section,
            'output': output_section,
            'ocr': ocr_section,
        }

        # --- optional: font ---
        font_mode = frontend_config.get('fontMode', 'auto')
        if font_mode == 'custom' and frontend_config.get('customFontPath'):
            java_config['font'] = {'path': frontend_config['customFontPath']}

        # --- optional: textLayer ---
        text_color = frontend_config.get('textColor')
        text_opacity = frontend_config.get('textOpacity')

        if text_color or text_opacity is not None:
            text_layer: dict = {}
            if text_color:
                text_layer['color'] = text_color
            if text_opacity is not None:
                text_layer['opacity'] = text_opacity
            java_config['textLayer'] = text_layer

        # --- optional: textConvert (s2t / t2s) ---
        chinese_conversion = frontend_config.get('chineseConversion')
        if chinese_conversion and chinese_conversion not in ('null', ''):
            java_config['textConvert'] = chinese_conversion

        return java_config
