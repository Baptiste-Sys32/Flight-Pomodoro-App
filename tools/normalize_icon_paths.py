#!/usr/bin/env python3
"""Normalize imported SVG arc flags into Android-compatible absolute cubic paths.

Requires fontTools (build tool only). Input geometry and license are from the
pinned Lucide revision recorded in docs/icon-provenance.json.
"""
from pathlib import Path
import re
from fontTools.svgLib.path import parse_path
from fontTools.pens.recordingPen import RecordingPen

root = Path(__file__).resolve().parents[1]

def normalize(match):
    pen = RecordingPen()
    parse_path(match.group(1), pen)
    result = []
    commands = {'moveTo': 'M', 'lineTo': 'L', 'curveTo': 'C', 'qCurveTo': 'Q', 'closePath': 'Z'}
    for operation, points in pen.value:
        if operation == 'endPath':
            continue
        result.append(commands[operation] + ' '.join(f'{x:.6g},{y:.6g}' for x, y in points))
    return 'android:pathData="' + ' '.join(result) + '"'

for path in (root / 'android/app/src/main/res/drawable').glob('ic_lucide_*.xml'):
    path.write_text(re.sub(r'android:pathData="([^"]+)"', normalize, path.read_text()))
print('Normalized Lucide paths to absolute Android geometry.')
