#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
act="${ACT4_DIR:-$HOME/src/riscv-arch-test}"
config="$root/config/act4/pipeline5-rv32i/test_config.yaml"
export PATH="$HOME/.local/bin:$HOME/.local/sail-riscv/bin:$PATH"

for tool in mise sail_riscv_sim riscv32-unknown-elf-gcc riscv32-unknown-elf-nm sbt; do
  command -v "$tool" >/dev/null || { echo "Missing ACT4 dependency: $tool" >&2; exit 1; }
done

mkdir -p "$root/target"
work="$(mktemp -d "$root/target/pipeline5-act4.XXXXXX")"
echo "ACT4 artifacts: $work"

cd "$act"
mise exec -- make CONFIG_FILES="$config" WORKDIR="$work" EXTENSIONS=I
manifest="$work/manifest.tsv"
touch "$manifest"
while IFS= read -r -d '' elf; do
  tohost="$(riscv32-unknown-elf-nm --defined-only "$elf" | awk '$3 == "tohost" { print $1 }')"
  [[ "$tohost" =~ ^[[:xdigit:]]+$ ]] || { echo "Missing tohost in $elf" >&2; exit 1; }
  printf '%s\t%s\n' "$tohost" "$elf" >> "$manifest"
done < <(find "$work/pipeline5-rv32i/elfs" -type f -name '*.elf' -print0 | sort -z)
[[ -s "$manifest" ]] || { echo 'ACT4 generated no test ELFs' >&2; exit 1; }

cd "$root"
PIPELINE5_ACT4_MANIFEST="$manifest" sbt 'testOnly riscv.integration.pipeline5.Act4Spec'
