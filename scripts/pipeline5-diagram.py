#!/usr/bin/env python3
"""Export an expandable hierarchy from Chisel's emitted Pipeline5 Verilog."""
from html import escape
from pathlib import Path
import re
import sys

rtl = Path(sys.argv[1]).read_text()
modules = dict(re.findall(r"(?ms)^module\s+(\w+)\b(.*?)^endmodule\b", rtl))

def render(module, instance):
    children = [(kind, name) for kind, name in
                re.findall(r"(?m)^\s*(\w+)\s+(\w+)\s*\(", modules[module])
                if kind in modules]
    label = escape(f"{instance} ({module})")
    if not children:
        return f"<li>{label}</li>"
    return f"<li><details open><summary>{label}</summary><ul>" + "".join(
        render(kind, name) for kind, name in children
    ) + "</ul></details></li>"

Path(sys.argv[2]).write_text(
    '<!doctype html><html lang="pt-BR"><meta charset="utf-8">'
    '<title>Pipeline5</title><h1>Hierarquia RTL do Pipeline5</h1><ul>'
    + render("Pipeline5", "Pipeline5") + "</ul></html>"
)
