# Flight Pomodoro App

Flight Pomodoro App is, sadly, a vibe-coded Pomodoro app that simulates flights for studying and working periods. It lets you choose flight times, logs flights, and includes a window view. This is all in progress, and I may come back to it.

## Features

- Interactive 3D globe and animated flight paths
- Airport selection and generated boarding pass details
- Single-leg and multi-stop journey flows
- Flight progress, pause, reset, and ambient mode controls
- Local flight history stored in the browser
- Window-seat scene with terrain, clouds, and aircraft assets

## Tech Stack

- HTML, CSS, and JavaScript
- Three.js
- Globe.gl
- FBX assets and texture-based scene elements
- Browser localStorage

## Getting Started

Serve the folder with a local static server so browser module imports and assets resolve correctly:

```bash
python3 -m http.server 8000
```

Then open:

```text
http://localhost:8000
```

## Notes

This project references CDN-hosted JavaScript libraries and local media/model assets.
