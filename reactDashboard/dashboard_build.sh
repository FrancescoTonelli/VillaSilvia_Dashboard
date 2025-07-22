#!/bin/bash

# Exit on error
set -e

npm install

# Build the project
npm run build

# Define source and destination directories
SRC_DIR="./dist"
DEST_DIR="../mqttServer/src/main/resources/webroot"

# Remove previous content in destination directory
rm -rf "${DEST_DIR:?}/"*

# Copy new build files
cp -r "${SRC_DIR}/"* "${DEST_DIR}/"