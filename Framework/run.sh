#!/usr/bin/env bash
# ============================================================
#  BUILD FRAMEWORK
# ============================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

APP_NAME="Framework"
SRC_DIR="$ROOT_DIR/src/java"
WEB_DIR="$ROOT_DIR/src/webapps"
BUILD_DIR="$ROOT_DIR/build"
CLASSES_DIR="$BUILD_DIR/WEB-INF/classes"
LIB_DIR="$BUILD_DIR/WEB-INF/lib"
CP="$ROOT_DIR/lib/*"

echo
echo "==================================="
echo "Nettoyage..."
echo "==================================="

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$LIB_DIR"

echo
echo "==================================="
echo "Compilation Java..."
echo "==================================="

mapfile -d '' sources < <(find "$SRC_DIR" -type f -name '*.java' -print0)
if ((${#sources[@]} == 0)); then
    echo "ERREUR : Aucun fichier Java trouve dans $SRC_DIR." >&2
    exit 1
fi
javac -cp "$CP" -d "$CLASSES_DIR" "${sources[@]}"

echo
echo "==================================="
echo "Copie des ressources Web..."
echo "==================================="
if [ -d "$WEB_DIR" ]; then
    cp -a "$WEB_DIR"/. "$BUILD_DIR/"
fi

echo
echo "==================================="
echo "Extraction des JARs..."
echo "==================================="
for jar in "$ROOT_DIR/lib/"*.jar; do
    if [ -f "$jar" ]; then
        echo "Extraction de $(basename "$jar")"
        (cd "$CLASSES_DIR" && jar xf "$jar")
    fi
done

echo
echo "==================================="
echo "Fusion des fichiers SPI Spring..."
echo "==================================="
merge_spring_file() {
    local resource="$1"
    local output="$CLASSES_DIR/META-INF/$resource"
    local found=false

    rm -f "$output"
    for jar in "$ROOT_DIR/lib/"*.jar; do
        if [ -f "$jar" ] && jar tf "$jar" | grep -qx "META-INF/$resource"; then
            mkdir -p "$ROOT_DIR/.spring-merge"
            rm -rf "$ROOT_DIR/.spring-merge"/*
            (cd "$ROOT_DIR/.spring-merge" && jar xf "$jar" "META-INF/$resource")
            cat "$ROOT_DIR/.spring-merge/META-INF/$resource" >> "$output"
            printf '\n' >> "$output"
            found=true
        fi
    done

    if [ "$found" = false ]; then
        rm -f "$output"
    else
        echo "Fichier META-INF/$resource fusionne."
    fi
    rm -rf "$ROOT_DIR/.spring-merge"
}

merge_spring_file "spring.handlers"
merge_spring_file "spring.schemas"

echo
echo "==================================="
echo "Creation du Framework.jar..."
echo "==================================="
JAR_PATH="$LIB_DIR/$APP_NAME.jar"
jar cf "$JAR_PATH" -C "$CLASSES_DIR" .
echo
echo "JAR cree :"
echo "$JAR_PATH"

echo
echo "==================================="
echo "BUILD TERMINE"
echo "==================================="