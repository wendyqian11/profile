#!/usr/bin/env bash
# Regenerates the website from the files in content/.
# Usage:  ./build.sh
set -e
cd "$(dirname "$0")"

echo "Compiling the Java generator..."
mkdir -p generator/out
javac -d generator/out generator/SiteGenerator.java

echo "Generating the site..."
java -cp generator/out SiteGenerator

echo "Done. Open index.html in your browser to preview."
