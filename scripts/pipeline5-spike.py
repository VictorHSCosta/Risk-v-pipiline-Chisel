#!/usr/bin/env python3
"""Run a bounded RV32IC differential smoke test with Spike on PATH."""
from pathlib import Path
import os
import re
import struct
import subprocess
import tempfile

# c.li x9,3; addi x10,x9,4; addi x31,x10,-6; jal x0,0.
program = struct.pack("<HIII", 0x448d, 0x00448513, 0xffa50f93, 0x0000006f)
base = 0x80000000
offset = 0x1000
header = struct.pack("<16sHHIIIIIHHHHHH", b"\x7fELF\x01\x01\x01", 2, 243, 1,
                     base, 52, 0, 1, 52, 32, 1, 0, 0, 0)
segment = struct.pack("<8I", 1, offset, base, base, len(program), len(program), 5, offset)
with tempfile.TemporaryDirectory() as directory:
    root = Path(directory)
    elf = root / "program.elf"
    elf.write_bytes((header + segment).ljust(offset, b"\0") + program)
    hexfile = root / "program.hex"
    padded = program.ljust((len(program) + 3) // 4 * 4, b"\0")
    hexfile.write_text("".join(f"{word:08x}\n" for (word,) in struct.iter_unpack("<I", padded)))
    run = subprocess.run(["spike", "--isa=rv32ic", "--instructions=64", "--log-commits",
                          "-m1", str(elf)], capture_output=True, text=True, timeout=30, check=True)
    rows = []
    for line in run.stderr.splitlines():
        match = re.match(r"core\s+0:\s+\d+\s+0x([0-9a-f]+)\s+\(0x[0-9a-f]+\)(.*)", line)
        if not match:
            continue
        pc = int(match[1], 16) - base
        if not 0 <= pc < len(program):
            continue
        write = re.search(r"\bx\s*(\d+)\s+0x([0-9a-f]+)", match[2])
        rd, data = (int(write[1]), int(write[2], 16)) if write else (0, 0)
        rows.append(f"{pc:08x} {rd} {data & 0xffffffff:08x}\n")
        if rd == 31:
            break
    if not rows or rd != 31:
        raise SystemExit("Spike did not produce the completion commit")
    trace = root / "trace.txt"
    trace.write_text("".join(rows))
    subprocess.run(["sbt", "testOnly riscv.integration.pipeline5.ExternalProgramSpec"],
                   env=dict(os.environ, PIPELINE5_HEX=str(hexfile), PIPELINE5_TRACE=str(trace)),
                   check=True)
