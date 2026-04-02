"""JPEG2PDF-OFD OCR — Entry point with pywebview window."""

import webview
from api.js_api import JsApi

WINDOW_TITLE = 'JPEG2PDF-OFD OCR'
WINDOW_WIDTH = 900
WINDOW_HEIGHT = 750
MIN_WIDTH = 800
MIN_HEIGHT = 600


def main():
    api = JsApi()
    window = webview.create_window(
        title=WINDOW_TITLE,
        url='ui/index.html',
        width=WINDOW_WIDTH,
        height=WINDOW_HEIGHT,
        min_size=(MIN_WIDTH, MIN_HEIGHT),
        js_api=api,
        text_select=True,
    )
    webview.start(debug=False)


if __name__ == '__main__':
    main()
