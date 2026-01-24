#!/bin/bash
# Simple shell script to pre-process JSON assets to SQLite
# This requires Java and SQLite JDBC driver

ASSETS_DIR="$(dirname "$0")/../app/src/main/assets"
OUTPUT_DIR="$(dirname "$0")/../app/src/main/assets"

echo "Asset Preprocessing Script"
echo "==========================="
echo ""
echo "Assets directory: $ASSETS_DIR"
echo "Output directory: $OUTPUT_DIR"
echo ""

# Check if assets directory exists
if [ ! -d "$ASSETS_DIR" ]; then
    echo "Error: Assets directory not found: $ASSETS_DIR"
    exit 1
fi

echo "Note: This script requires a Java tool to convert JSON to SQLite."
echo "The PreprocessAssets.java file is provided but needs to be compiled."
echo ""
echo "For now, you can:"
echo "1. Use the Gradle task (recommended)"
echo "2. Manually convert using a tool like sqlite3 or a Python script"
echo "3. Leave JSON files as-is - the app will use them directly"
echo ""
echo "The app automatically prefers .db files over .json if both exist."
echo ""
