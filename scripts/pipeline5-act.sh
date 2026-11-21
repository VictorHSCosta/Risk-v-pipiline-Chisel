#!/usr/bin/env bash
# Runs compatible ACT4 images prepared for this DUT's memory map and halt ABI.
set -euo pipefail
if (( $# == 0 )); then
  echo "usage: bash scripts/pipeline5-act.sh image.hex [image.hex ...]" >&2
  exit 2
fi
unset PIPELINE5_TRACE
for image in "$@"; do
  test -s "$image"
  echo "ACT image: $image"
  PIPELINE5_HEX="$image" sbt 'testOnly riscv.integration.pipeline5.ExternalProgramSpec'
done
