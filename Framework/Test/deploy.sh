#!/bin/bash

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$BASE_DIR/src/main/java"
WEB_DIR="$BASE_DIR"
CLASSES_DIR="$WEB_DIR/WEB-INF/classes"

TOMCAT_LIB="/opt/tomcat/lib/servlet-api.jar"
FRAMEWORK_JAR="$WEB_DIR/WEB-INF/lib/Framework.jar"

TOMCAT_WEBAPPS="/opt/tomcat/webapps"
APP_NAME="Sprint6"

echo "=== NETTOYAGE ET PRÉPARATION ==="
rm -rf "$CLASSES_DIR"
mkdir -p "$CLASSES_DIR"

echo "=== COMPILATION DES CONTROLEURS ==="
FILES=$(find "$SRC_DIR" -name "*.java" 2>/dev/null)

if [ -z "$FILES" ]; then
    echo "[Erreur] Aucun fichier .java trouvé dans : $SRC_DIR"
    exit 1
fi

javac -cp "$TOMCAT_LIB:$FRAMEWORK_JAR" -d "$CLASSES_DIR" $FILES

if [ $? -ne 0 ]; then
    echo "[Erreur] La compilation a échoué."
    exit 1
fi

echo "=== DEPLOIEMENT VERS TOMCAT ==="
rm -rf "$TOMCAT_WEBAPPS/$APP_NAME"
mkdir -p "$TOMCAT_WEBAPPS/$APP_NAME"

cp -r "$WEB_DIR/WEB-INF" "$TOMCAT_WEBAPPS/$APP_NAME/"
cp "$WEB_DIR/Test.html" "$TOMCAT_WEBAPPS/$APP_NAME/" 2>/dev/null

echo "=== REDÉMARRAGE DE TOMCAT ==="
# /home/andriantsoa/Documents/apache-tomcat-10.0.16/bin/shutdown.sh
sleep 2
# /home/andriantsoa/Documents/apache-tomcat-10.0.16/bin/startup.sh