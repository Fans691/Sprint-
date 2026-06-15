#!/bin/bash

# Définition des variables
APP_NAME="framework"
SRC_DIR="src/main/java"
BUILD_DIR="build"
LIB_DIR="lib"
SERVLET_API_JAR="$LIB_DIR/servlet-api.jar"

# Compilation des fichiers Java avec le JAR des Servlets
find $SRC_DIR -name "*.java" > sources.txt
javac -cp "$LIB_DIR/*" -d $BUILD_DIR/ @sources.txt
rm sources.txt

# Copier les dépendances dans /lib
mkdir -p $BUILD_DIR/lib
cp -r $LIB_DIR/* $BUILD_DIR/lib/

# Revenir vers le repertoire source du projet
# cd ..

# Générer le fichier .jar dans le dossier build
cd $BUILD_DIR || exit
jar -cvf $APP_NAME.jar *

echo ""

echo "JAR creee."

echo ""