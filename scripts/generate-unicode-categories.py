#!/usr/bin/env python3
"""Regenerate supplementary category ranges from the pinned Unicode Character Database."""
import hashlib
from pathlib import Path
from urllib.request import urlopen

URL = "https://www.unicode.org/Public/16.0.0/ucd/UnicodeData.txt"
SHA256 = "ff58e5823bd095166564a006e47d111130813dcf8bf234ef79fa51a870edb48f"
CATEGORIES = {
    "Cn": 0, "Lu": 1, "Ll": 2, "Lt": 3, "Lm": 4, "Lo": 5,
    "Mn": 6, "Me": 7, "Mc": 8, "Nd": 9, "Nl": 10, "No": 11,
    "Zs": 12, "Zl": 13, "Zp": 14, "Cc": 15, "Cf": 16,
    "Co": 18, "Cs": 19, "Pd": 20, "Ps": 21, "Pe": 22, "Pc": 23,
    "Po": 24, "Sm": 25, "Sc": 26, "Sk": 27, "So": 28, "Pi": 29, "Pf": 30,
}


def main():
    with urlopen(URL) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != SHA256:
        raise ValueError("Unexpected UnicodeData.txt checksum")
    ranges = []
    first = None
    for line in data.decode("utf-8").splitlines():
        point, name, category, *_ = line.split(";")
        point = int(point, 16)
        if point < 0x10000:
            continue
        if name.endswith(", First>"):
            first = point
            continue
        start = first if name.endswith(", Last>") else point
        if start is None:
            raise ValueError("Range end without a start")
        if ranges and ranges[-1][1] + 1 == start and ranges[-1][2] == category:
            ranges[-1] = (ranges[-1][0], point, category)
        else:
            ranges.append((start, point, category))
        first = None
    path = Path(__file__).resolve().parents[1] / "kotmark/src/internal/util/UnicodeCategories.kt"
    marker = "    private val SUPPLEMENTARY_RANGES = intArrayOf(\n"
    prefix = path.read_text().split(marker)[0] + marker
    rows = [f"0x{start:X}, 0x{end:X}, {CATEGORIES[category]}," for start, end, category in ranges]
    body = "".join("        " + "  ".join(rows[i:i + 3]) + "\n" for i in range(0, len(rows), 3))
    path.write_text(prefix + body + "    )\n}\n")


if __name__ == "__main__":
    main()
