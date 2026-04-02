"""JavaScript-callable Python functions exposed via pywebview.

All methods here become window.pywebview.api.* in the frontend.
Bridge calls are async (return Promise); callbacks to JS use window.evaluate_js.
"""

import json
import os

import webview

from core.config import Settings
from core.bridge import ConversionBridge


class JsApi:
    def __init__(self):
        self.settings = Settings()
        self.bridge = ConversionBridge()

    # ================================================================
    # File Dialogs
    # ================================================================

    def open_directory_chooser(self, current_path: str = '') -> str:
        """Open native directory picker dialog."""
        folder = webview.windows[0].create_file_dialog(
            dialog_type=webview.FOLDER_DIALOG,
            directory=current_path or os.path.expanduser('~'),
        )
        return folder[0] if folder else ''

    def open_file_chooser(self) -> str:
        """Open native file picker for PDF files."""
        result = webview.windows[0].create_file_dialog(
            dialog_type=webview.OPEN_DIALOG,
            file_types=('PDF Files (*.pdf)',),
        )
        return result[0] if result else ''

    def open_font_file_chooser(self) -> str:
        """Open native file picker for TTF font files."""
        result = webview.windows[0].create_file_dialog(
            dialog_type=webview.OPEN_DIALOG,
            file_types=('Font Files (*.ttf;*.ttc)',),
        )
        return result[0] if result else ''

    # ================================================================
    # Conversion
    # ================================================================

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

    # ================================================================
    # Settings
    # ================================================================

    def load_settings(self) -> str:
        return json.dumps(self.settings.load(), ensure_ascii=False)

    def save_settings(self, settings_json: str):
        self.settings.save(json.loads(settings_json))

    def load_default_settings(self) -> str:
        return json.dumps(self.settings.load_defaults(), ensure_ascii=False)

    def delete_settings(self):
        self.settings.delete()

    # ================================================================
    # Version
    # ================================================================

    def get_version(self) -> str:
        return '3.0.0 (pywebview)'

    # ================================================================
    # Callbacks to JavaScript via window.evaluate_js
    # ================================================================

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
