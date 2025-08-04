
#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

java \
  -Dglass.platform=gtk \
  -Djava.library.path="${SCRIPT_DIR}" \
  --module-path "${SCRIPT_DIR}" \
  --add-modules javafx.controls,javafx.media \
  -cp "${SCRIPT_DIR}/*" \
  museo.PlayVideo
