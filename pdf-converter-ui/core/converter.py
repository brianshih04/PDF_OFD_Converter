"""Config JSON builder + subprocess invocation utilities.

This module provides helper functions for building Java CLI config JSON
and invoking the conversion subprocess. The heavy lifting lives in bridge.py;
converter.py offers a convenience layer for simple use-cases.
"""

import json
from pathlib import Path

from core.bridge import ConversionBridge, _transform_config


def build_config_json(frontend_settings: dict) -> str:
    """Build a Java CLI config JSON string from frontend settings dict.

    This is a convenience wrapper around ConversionBridge._transform_config().
    """
    java_config = _transform_config(frontend_settings)
    return json.dumps(java_config, ensure_ascii=False, indent=2)


def get_jar_path() -> Path:
    """Return the resolved JAR path (for diagnostics / logging)."""
    bridge = ConversionBridge()
    return bridge.jar_path
