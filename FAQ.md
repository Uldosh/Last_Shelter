# FAQ

## How do I start the game?

Run this command from the repository root on Windows:

```bat
gradlew.bat desktop:run
```

## What Java version is needed?

Use a JDK that can run Gradle and Java 17-targeted bytecode. Java 17 or newer is recommended.

## Why are the graphics simple?

LAST SHELTER is an MVP-style desktop prototype. Rooms use lightweight placeholder 3D shapes and bounded effects so it stays easy to run on laptops.

## How do I interact with bunker objects?

Walk near an interactable object until the HUD prompt appears, then press `E`.

## How do days progress?

Complete the current main objective and press `N` to end the day. Daily resource and failure checks run at that point.

## How do save, load, and checkpoints work?

Open the pause menu with `ESC`. Press `S` to save, `L` to load, or `C` to restore the latest in-memory checkpoint. Autosave also runs during gameplay and at important progression moments.

## How do I change volume?

Pause with `ESC`, press `O` for settings, then use the arrow keys shown in the settings menu.

## How do I build the EXE?

Install Launch4j, set `LAUNCH4J_HOME`, and run `export_exe.bat`. The output is `dist/LAST_SHELTER.exe`.

## Does the EXE include a Java runtime?

No. The provided Launch4j setup wraps the JAR and expects Java 17 or newer on the target system.

