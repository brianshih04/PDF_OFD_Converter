#!/bin/bash
# 構建中國國產 Linux 版本
# Build script for Chinese domestic Linux distributions

echo "========================================="
echo "  JPEG2PDF-OFD OCR CLI"
echo "  中國國產 Linux 構建腳本"
echo "========================================="
echo ""

# 檢查是否已安裝 Conveyor
if ! command -v conveyor &> /dev/null; then
    echo "錯誤：未找到 Conveyor"
    echo "請先安裝 Conveyor: https://www.hydraulic.software/download"
    exit 1
fi

# 構建 JAR
echo "步驟 1: 構建 JAR 文件..."
mvn clean package
if [ $? -ne 0 ]; then
    echo "錯誤：JAR 構建失敗"
    exit 1
fi
echo "✅ JAR 構建成功"
echo ""

# 選擇要構建的平台
echo "步驟 2: 選擇要構建的平台"
echo "1) 全部平台（包括 Windows, macOS, Linux）"
echo "2) 僅 Linux x86_64 (統信 UOS, Deepin, openEuler)"
echo "3) 僅 Linux ARM64 (華為鯤鵬, 飛騰)"
echo "4) 僅 Linux DEB 包"
echo "5) 僅 Linux RPM 包"
echo ""
read -p "請選擇 (1-5): " choice

case $choice in
    1)
        echo "構建所有平台..."
        conveyor make site
        ;;
    2)
        echo "構建 Linux x86_64 (DEB + RPM)..."
        conveyor make app.linux.amd64.deb
        conveyor make app.linux.amd64.rpm
        ;;
    3)
        echo "構建 Linux ARM64 (DEB + RPM)..."
        conveyor make app.linux.aarch64.deb
        conveyor make app.linux.aarch64.rpm
        ;;
    4)
        echo "構建 Linux DEB 包..."
        conveyor make app.linux.amd64.deb
        conveyor make app.linux.aarch64.deb
        ;;
    5)
        echo "構建 Linux RPM 包..."
        conveyor make app.linux.amd64.rpm
        conveyor make app.linux.aarch64.rpm
        ;;
    *)
        echo "無效選擇"
        exit 1
        ;;
esac

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "  ✅ 構建成功！"
    echo "========================================="
    echo ""
    echo "輸出文件位於：output/"
    echo ""
    echo "文件列表："
    ls -lh output/*.{deb,rpm} 2>/dev/null || ls -lh output/
    echo ""
    echo "支持的中國國產 Linux："
    echo "  - 統信 UOS (DEB)"
    echo "  - 深度 Deepin (DEB)"
    echo "  - 華為 openEuler (RPM)"
    echo "  - 銀河麒麟 (RPM)"
    echo "  - 中標麒麟 (RPM)"
    echo "  - 華為鯤鵬 (DEB/RPM ARM64)"
else
    echo ""
    echo "========================================="
    echo "  ❌ 構建失敗"
    echo "========================================="
    echo "請檢查錯誤信息並重試"
    exit 1
fi
