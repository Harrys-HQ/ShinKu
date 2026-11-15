"""Check 64-bit native libraries in APKs for 16 KB ZIP, LOAD and RELRO alignment.

Usage: python scripts/check-native-alignment.py <apk> [<apk> ...] --report <json>
No SDK binaries or third-party Python packages are required. This checks binary
layout, not runtime behavior. Android's zipalign and on-device tests complement it.
"""

import argparse
import json
import struct
import sys
import zipfile
from pathlib import Path

PAGE = 16384


def inspect_elf(data):
    if data[:6] != b"\x7fELF\x02\x01":
        raise ValueError("Expected a little-endian ELF64 library")
    offset = struct.unpack_from("<Q", data, 32)[0]
    size, count = struct.unpack_from("<HH", data, 54)
    if size < 56 or not count:
        raise ValueError("Invalid program header table")
    headers = [struct.unpack_from("<IIQQQQQQ", data, offset + i * size) for i in range(count)]
    loads = [h for h in headers if h[0] == 1]
    if not loads:
        raise ValueError("Missing LOAD segments")
    errors = []
    for h in loads:
        if h[7] < PAGE or h[2] % PAGE != h[3] % PAGE:
            errors.append(f"LOAD offset={h[2]:#x} vaddr={h[3]:#x} alignment={h[7]:#x}")
    for r in (h for h in headers if h[0] == 0x6474E552):
        end = r[3] + r[6]
        if end % PAGE and any(h[1] & 2 and h[3] <= r[3] and h[3] + h[6] > end for h in loads):
            errors.append(f"RELRO ends at {end:#x} before writable data in the same LOAD segment")
    return [h[7] for h in loads], errors


def inspect_apk(path):
    libraries = []
    with zipfile.ZipFile(path) as archive, path.open("rb") as raw:
        for info in archive.infolist():
            parts = info.filename.split("/")
            if len(parts) != 3 or parts[0] != "lib" or not parts[2].endswith(".so"):
                continue
            if parts[1] not in ("arm64-v8a", "x86_64"):
                continue
            errors = []
            try:
                alignments, errors = inspect_elf(archive.read(info))
                raw.seek(info.header_offset)
                header = raw.read(30)
                if header[:4] != b"PK\x03\x04":
                    raise ValueError("Invalid ZIP local header")
                name_size, extra_size = struct.unpack_from("<HH", header, 26)
                data_offset = info.header_offset + 30 + name_size + extra_size
                if info.compress_type == zipfile.ZIP_STORED and data_offset % PAGE:
                    errors.append(f"Uncompressed ZIP entry offset={data_offset:#x} is not 16 KB aligned")
            except (ValueError, struct.error, zipfile.BadZipFile) as error:
                alignments = []
                errors.append(str(error))
            libraries.append({"file": info.filename, "loadAlignment": alignments, "errors": errors})
    return {"apk": str(path), "libraries": libraries, "passed": all(not r["errors"] for r in libraries)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path, nargs="+")
    parser.add_argument("--report", type=Path)
    arguments = parser.parse_args()
    results = [inspect_apk(path) for path in arguments.apk]
    for result in results:
        print(f"{'PASS' if result['passed'] else 'FAIL'} {result['apk']}: {len(result['libraries'])} 64-bit libraries")
        for row in result["libraries"]:
            for error in row["errors"]:
                print(f"  {row['file']}: {error}")
    if arguments.report:
        arguments.report.parent.mkdir(parents=True, exist_ok=True)
        arguments.report.write_text(json.dumps(results, indent=2) + "\n", encoding="utf-8")
    return 0 if all(r["passed"] for r in results) else 1


if __name__ == "__main__":
    sys.exit(main())
