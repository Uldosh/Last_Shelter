# LAST SHELTER Assignment Summary

## Project Goal

Create a desktop Java and libGDX 3D game prototype called LAST SHELTER. The project focuses on a small bunker survival experience that is easy to run, inspect, and extend.

## Implemented Scope

- Desktop window title: LAST SHELTER.
- Default window resolution: 1280 x 720.
- First-person camera with movement, sprinting, mouse look, collision, pause, and fullscreen toggle.
- Five bunker rooms with simple placeholder 3D design:
  - Main Hall
  - Generator Room
  - Water Sector
  - AI Core Room
  - Storage Room
- Room props, lights, interaction objects, automatic doors, cameras, screens, and lightweight atmosphere effects.
- Survival loop for ten in-game days.
- Objective tracker, AI monitor messages, mini-games, save/load, autosave, checkpoints, and ending/game-over screens.

## Development Notes

- The code uses Java only and libGDX only for the game runtime.
- Visuals use simple model boxes and small runtime effects to remain suitable for laptops.
- The project keeps gameplay logic in the existing libGDX game loop while separating utility and design-pattern classes into their own Java files.

## Required Run Command

```bat
gradlew.bat desktop:run
```

## Repository Deliverables

- Source code for `core` and `desktop`.
- Gradle wrapper files.
- GitHub documentation files.
- Windows JAR build script.
- Launch4j EXE export script and configuration.

