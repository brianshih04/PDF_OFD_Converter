"""Tests for core.bridge.ConversionBridge.transform_config().

Validates the frontend → Java CLI config transformation logic
without needing a running pywebview window.
"""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from core.bridge import ConversionBridge


def _transform(frontend: dict) -> dict:
    return ConversionBridge.transform_config(frontend)


class TestTransformConfigBasic:
    def test_image_input_type(self):
        result = _transform({"inputType": "image", "inputPath": "/img"})
        assert result["input"]["type"] == "image"
        assert result["input"]["folder"] == "/img"

    def test_pdf_input_type(self):
        result = _transform({"inputType": "pdf", "inputPath": "/doc.pdf"})
        assert result["input"]["type"] == "pdf"
        assert result["input"]["file"] == "/doc.pdf"

    def test_default_input_type_is_image(self):
        result = _transform({"inputPath": "/img"})
        assert result["input"]["type"] == "image"

    def test_output_folder(self):
        result = _transform({"outputPath": "/out"})
        assert result["output"]["folder"] == "/out"

    def test_output_format_string(self):
        result = _transform({"formats": "pdf,ofd"})
        assert result["output"]["format"] == "pdf,ofd"

    def test_output_format_default_pdf(self):
        result = _transform({})
        assert result["output"]["format"] == "pdf"


class TestTransformConfigOcr:
    def test_language_default(self):
        result = _transform({})
        assert result["ocr"]["language"] == "chinese_cht"

    def test_language_custom(self):
        result = _transform({"language": "japan"})
        assert result["ocr"]["language"] == "japan"

    def test_engine_default(self):
        result = _transform({})
        assert result["ocr"]["engine"] == "auto"

    def test_engine_tesseract(self):
        result = _transform({"ocrEngine": "tesseract"})
        assert result["ocr"]["engine"] == "tesseract"

    def test_tesseract_data_path_included(self):
        result = _transform({"tesseractDataPath": "/opt/tessdata"})
        assert result["ocr"]["tesseractDataPath"] == "/opt/tessdata"

    def test_tesseract_data_path_omitted_when_empty(self):
        result = _transform({"tesseractDataPath": ""})
        assert "tesseractDataPath" not in result["ocr"]


class TestTransformConfigMultiPage:
    def test_multiPage_false_by_default(self):
        result = _transform({})
        assert result["output"]["multiPage"] is False

    def test_multiPage_bool_true(self):
        result = _transform({"multiPage": True})
        assert result["output"]["multiPage"] is True

    def test_multiPage_string_true(self):
        result = _transform({"multiPage": "true"})
        assert result["output"]["multiPage"] is True

    def test_multiPage_string_false(self):
        result = _transform({"multiPage": "false"})
        assert result["output"]["multiPage"] is False


class TestTransformConfigFont:
    def test_auto_font_mode_no_font_section(self):
        result = _transform({"fontMode": "auto"})
        assert "font" not in result

    def test_custom_font_mode_with_path(self):
        result = _transform(
            {
                "fontMode": "custom",
                "customFontPath": "/fonts/my-font.ttf",
            }
        )
        assert result["font"]["path"] == "/fonts/my-font.ttf"

    def test_custom_font_mode_empty_path_no_section(self):
        result = _transform(
            {
                "fontMode": "custom",
                "customFontPath": "",
            }
        )
        assert "font" not in result


class TestTransformConfigTextLayer:
    def test_text_color(self):
        result = _transform({"textColor": "debug"})
        assert result["textLayer"]["color"] == "debug"

    def test_text_opacity(self):
        result = _transform({"textOpacity": 0.5})
        assert result["textLayer"]["opacity"] == 0.5

    def test_text_color_and_opacity(self):
        result = _transform({"textColor": "red", "textOpacity": 0.8})
        assert result["textLayer"]["color"] == "red"
        assert result["textLayer"]["opacity"] == 0.8

    def test_no_text_layer_when_unset(self):
        result = _transform({})
        assert "textLayer" not in result


class TestTransformConfigChineseConversion:
    def test_s2t(self):
        result = _transform({"chineseConversion": "s2t"})
        assert result["textConvert"] == "s2t"

    def test_t2s(self):
        result = _transform({"chineseConversion": "t2s"})
        assert result["textConvert"] == "t2s"

    def test_null_value_omitted(self):
        result = _transform({"chineseConversion": "null"})
        assert "textConvert" not in result

    def test_empty_value_omitted(self):
        result = _transform({"chineseConversion": ""})
        assert "textConvert" not in result

    def test_missing_key_omitted(self):
        result = _transform({})
        assert "textConvert" not in result
