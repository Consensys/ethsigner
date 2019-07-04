#!/bin/bash

export GOSS_PATH=tests/goss-linux-amd64
export GOSS_OPTS="$GOSS_OPTS --format junit"
export GOSS_FILES_STRATEGY=cp
DOCKER_IMAGE=$1

i=0

# Test for normal startup with ports opened
GOSS_FILES_PATH=tests/01 \
bash tests/dgoss \
run $DOCKER_IMAGE \
--chain-id=2018 \
--downstream-http-port=8590 \
--http-listen-port=9000 \
file-based-signer \
--key-file \
--password-file \
> ./reports/01.xml || i=`expr $i + 1`

exit $i
