#!/bin/bash
set -euo pipefail

VERSION=${1:?Must specify version}
DIST=${2:?Must specify path to distributions}

DIST_IDENTIFIER="ethsigner"
CLOUDSMITH_REPO="consensys/ethsigner"
SUMMARY="EthSigner - ${VERSION}"
ZIP_DIST="${DIST}/${DIST_IDENTIFIER}-${VERSION}.zip"
ZIP_NAME="${DIST_IDENTIFIER}.zip"
TAR_DIST="${DIST}/${DIST_IDENTIFIER}-${VERSION}.tar.gz"
TAR_NAME="${DIST_IDENTIFIER}.tar.gz"

REPUBLISH=""
if [[ $VERSION == *"develop"* ]]; then
  REPUBLISH="--republish"
fi

if [ -z ${CLOUDSMITH_USER+x} ]; then echo "CLOUDSMITH_USER is unset."; else echo "CLOUDSMITH_USER is set."; fi

# cloudsmith cli setup
ENV_DIR=./build/tmp/cloudsmith-env
if [[ -d ${ENV_DIR} ]] ; then
    source ${ENV_DIR}/bin/activate
else
    python3 -m venv ${ENV_DIR}
    source ${ENV_DIR}/bin/activate
fi

python3 -m pip install --upgrade cloudsmith-cli

# upload
cloudsmith push raw $CLOUDSMITH_REPO $ZIP_DIST $REPUBLISH --name "${ZIP_NAME}" --version "${VERSION}" --summary "${SUMMARY} binary distribution" --description "${SUMMARY} binary distribution in zip format" --content-type 'application/zip'
cloudsmith push raw $CLOUDSMITH_REPO $TAR_DIST $REPUBLISH --name "${TAR_NAME}" --version "${VERSION}" --summary "${SUMMARY} binary distribution" --description "${SUMMARY} binary distribution in tar gzipped format" --content-type 'application/tar+gzip'


