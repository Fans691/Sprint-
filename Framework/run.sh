#!/bin/bash

# ============================================================
# BUILD FRAMEWORK
# ============================================================

set -e

# Se placer dans le dossier du script
cd "$(dirname "$0")"

# --------------------------
# VARIABLES
# --------------------------
APP_NAME="Framework"
SRC_DIR="src/java"
WEB_DIR="src/webapps"
BUILD_DIR="build"
CLASSES_DIR="$BUILD_DIR/WEB-INF/classes"
LIB_DIR="$BUILD_DIR/WEB-INF/lib"
LIB="lib"

echo
echo "==================================="
echo "Nettoyage..."
echo "==================================="

rm -rf "$BUILD_DIR"

mkdir -p "$CLASSES_DIR"
mkdir -p "$LIB_DIR"

echo
echo "==================================="
echo "Compilation Java..."
echo "==================================="

find "$SRC_DIR" -name "*.java" > /tmp/sources.txt

if ls "$LIB"/*.jar >/dev/null 2>&1; then
    CLASSPATH=$(printf "%s:" "$LIB"/*.jar)
    CLASSPATH=${CLASSPATH%:}

    javac \
        -cp "$CLASSPATH" \
        -d "$CLASSES_DIR" \
        @/tmp/sources.txt
else
    javac \
        -d "$CLASSES_DIR" \
        @/tmp/sources.txt
fi

RESULT=$?

rm -f /tmp/sources.txt

if [ $RESULT -ne 0 ]; then
    echo
    echo "ERREUR : Compilation échouée."
    exit 1
fi

echo
echo "==================================="
echo "Copie des ressources Web..."
echo "==================================="

if [ -d "$WEB_DIR" ]; then
    cp -R "$WEB_DIR"/. "$BUILD_DIR"/
fi

echo
echo "==================================="
echo "Extraction des JARs..."
echo "==================================="

for jarfile in "$LIB"/*.jar
do
    [ -f "$jarfile" ] || continue

    echo "Extraction de $(basename "$jarfile")"
    jarpath="$(realpath "$jarfile")"

    # Pour ignorer servlet-api.jar :
    # if [ "$(basename "$jarfile")" != "servlet-api.jar" ]; then

    (
        cd "$CLASSES_DIR"
        jar xf "$jarpath"
    )

    # fi
done

echo
echo "==================================="
echo "Création du Framework.jar..."
echo "==================================="

jar cf "$LIB_DIR/$APP_NAME.jar" -C "$CLASSES_DIR" .

if [ $? -ne 0 ]; then
    echo "ERREUR : Création du JAR impossible."
    exit 1
fi

echo
echo "JAR créé :"
echo "$LIB_DIR/$APP_NAME.jar"

echo
echo "==================================="
echo "BUILD TERMINÉ"
echo "==================================="
echo
