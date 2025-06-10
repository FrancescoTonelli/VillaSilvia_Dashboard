#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

exec java \
  -Dglass.platform=gtk \
  -Djava.library.path="${SCRIPT_DIR}" \
  --module-path "${SCRIPT_DIR}" \
  --add-modules javafx.controls,javafx.media \
  -cp "${SCRIPT_DIR}/*" \
  com.example.audioplayer.Main
