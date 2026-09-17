"""Offline release checks: python scripts/audit_package.py --sdk ANDROID_SDK_PATH."""
import argparse
import hashlib
import json
import re
import struct
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("--sdk", required=True)
parser.add_argument("--apk", type=Path, default=root / "app/build/outputs/apk/preview/app-preview.apk")
args = parser.parse_args()
sdk = Path(args.sdk)
aapt = next((sdk / "build-tools/35.0.0").glob("aapt2*"))
badging = subprocess.check_output([str(aapt), "dump", "badging", str(args.apk)], text=True)
permissions = re.findall(r"uses-permission: name='([^']+)'", badging)
allowed = {"android.permission.POST_NOTIFICATIONS", "android.permission.RECEIVE_BOOT_COMPLETED", "app.candlr.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"}
assert set(permissions) <= allowed, f"Unexpected permissions: {set(permissions) - allowed}"
assert "minSdkVersion:'26'" in badging or "sdkVersion:'26'" in badging
assert "targetSdkVersion:'36'" in badging
assert "application-debuggable" not in badging
manifest = root / "app/build/intermediates/merged_manifests/preview/processPreviewManifest/AndroidManifest.xml"
ns = "{http://schemas.android.com/apk/res/android}"
app = ET.parse(manifest).getroot().find("application")
assert app.get(ns + "allowBackup") == "false"
assert app.get(ns + "debuggable", "false") == "false"
alignments = {}
with ZipFile(args.apk) as package:
    for entry in package.namelist():
        if entry.endswith(".so") and ("arm64-v8a" in entry or "x86_64" in entry):
            binary = package.read(entry)
            assert binary[:5] == b"\x7fELF\x02", entry
            order = "<" if binary[5] == 1 else ">"
            offset = struct.unpack_from(order + "Q", binary, 32)[0]
            size, count = struct.unpack_from(order + "HH", binary, 54)
            loads = []
            for i in range(count):
                header = struct.unpack_from(order + "IIQQQQQQ", binary, offset + i * size)
                if header[0] == 1:
                    assert header[7] >= 16384, f"16 KiB ELF alignment missing: {entry}"
                    loads.append(header[7])
            alignments[entry] = loads
bundle = root / "app/build/outputs/bundle/release/app-release.aab"
with ZipFile(bundle) as archive:
    signed = any(name.upper().endswith((".RSA", ".DSA", ".EC")) and name.startswith("META-INF/") for name in archive.namelist())
report = {"apk": args.apk.name, "bytes": args.apk.stat().st_size,
          "sha256": hashlib.sha256(args.apk.read_bytes()).hexdigest(), "permissions": permissions,
          "minSdk": 26, "targetSdk": 36, "automaticBackup": False, "debuggable": False,
          "native64BitLoadAlignments": alignments, "releaseBundleSigned": signed,
          "note": "Preview is debug-signed for evaluation. An unsigned bundle needs an upload key before submission."}
out = root / "artifacts/package-audit.json"
out.parent.mkdir(exist_ok=True)
out.write_text(json.dumps(report, indent=2) + "\n")
print(json.dumps(report, indent=2))
