"""Settings management — load/save/default/reset.

Uses ~/.jpeg2pdf-ofd/settings.json for user settings, backward compatible
with the JavaFX version.
"""

import json
from pathlib import Path

SETTINGS_DIR = Path.home() / '.jpeg2pdf-ofd'
SETTINGS_FILE = SETTINGS_DIR / 'settings.json'
DEFAULTS_FILE = Path(__file__).parent.parent / 'settings' / 'default.json'


class Settings:
    def __init__(self):
        SETTINGS_DIR.mkdir(parents=True, exist_ok=True)
        self._defaults = self._load_json(DEFAULTS_FILE)

    def load(self) -> dict:
        """Load user settings, falling back to defaults for missing keys."""
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
