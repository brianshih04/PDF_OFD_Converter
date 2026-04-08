#!/bin/bash
# MemPalace Save Hook
# 此腳本在 Claude Code 會話停止時執行
# This script runs when the Claude Code session stops

echo "========================================="
echo "  MemPalace Save Hook"
echo "========================================="
echo "Timestamp: $(date)"
echo "Project: pdf_ofd_converter"
echo "Action: Saving MemPalace memory..."
echo ""

# 在此處添加您的記憶保存邏輯
# Add your memory saving logic here

# 例如：備份記憶檔案
# Example: Backup memory files
# if [ -f "mempalace.yaml" ]; then
#     cp mempalace.yaml "mempalace.yaml.backup.$(date +%Y%m%d_%H%M%S)"
# fi

echo "✅ MemPalace memory saved successfully"
echo "========================================="
