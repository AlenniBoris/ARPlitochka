#!/bin/sh

COMPOSE_SRC="${PROJECT_DIR}/ComposeBundleResources/composeResources"
COMPOSE_DEST="${BUILT_PRODUCTS_DIR}/${CONTENTS_FOLDER_PATH}/compose-resources/composeResources"

if [ ! -d "${COMPOSE_SRC}" ]; then
  echo "warning: Compose resources not found at ${COMPOSE_SRC}"
  exit 0
fi

mkdir -p "${COMPOSE_DEST}"
cp -R "${COMPOSE_SRC}/." "${COMPOSE_DEST}/"
echo "Copied Compose resources to ${COMPOSE_DEST}"
