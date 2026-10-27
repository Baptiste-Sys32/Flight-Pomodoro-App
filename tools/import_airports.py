#!/usr/bin/env python3
"""Import public-domain OurAirports data without copying comparison APK assets."""
import csv
import hashlib
import io
import json
from pathlib import Path
from urllib.request import urlopen

SOURCE = "https://davidmegginson.github.io/ourairports-data/airports.csv"
ROOT = Path(__file__).resolve().parents[1]


def main():
    with urlopen(SOURCE, timeout=45) as response:
        raw = response.read()
    airports = []
    for row in csv.DictReader(io.StringIO(raw.decode("utf-8-sig"))):
        if row["type"] not in {"large_airport", "medium_airport"} or len(row["iata_code"]) != 3:
            continue
        lat, lon = float(row["latitude_deg"]), float(row["longitude_deg"])
        if not (-90 <= lat <= 90 and -180 <= lon <= 180):
            raise ValueError("Airport coordinate outside valid range")
        def clean(value):
            return value.replace("\u2013", "-").replace("\u2014", "-")
        airports.append(dict(code=row["iata_code"], city=clean(row["municipality"] or row["name"]),
                             name=clean(row["name"]), country=row["iso_country"], lat=lat, lon=lon))
    codes = {a["code"]: a for a in airports}
    if len(codes) != len(airports):
        raise ValueError("Ambiguous IATA codes require manual review")
    if not (48 < codes["CDG"]["lat"] < 50 and 1 < codes["CDG"]["lon"] < 4):
        raise ValueError("Coordinate reference check failed")
    output = ROOT / "android/app/src/main/assets/airports.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(sorted(airports, key=lambda a: a["code"]), ensure_ascii=False,
                                 separators=(",", ":")) + "\n")
    provenance = dict(source=SOURCE, license="Public Domain", license_source="https://ourairports.com/data/",
                      source_sha256=hashlib.sha256(raw).hexdigest(), records=len(airports))
    (output.parent / "airports-provenance.json").write_text(json.dumps(provenance, indent=2) + "\n")
    print(f"Imported {len(airports)} airports; coordinate and unique-code checks passed.")


if __name__ == "__main__":
    main()
