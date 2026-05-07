# LAST SHELTER

LAST SHELTER is a Java and libGDX desktop bunker-survival prototype. The player explores a compact 3D bunker in first person, repairs systems through world interactions and lightweight mini-games, follows a strict monitoring AI, manages survival resources, and tries to survive ten in-game days.

## Features

- First-person movement through five bunker rooms.
- Interactions, objectives, checkpoints, save/load, autosave, pause and settings screens.
- Generator timing repair, wire reconnect, and keypad mini-games.
- Survival resources: Health, Stress, Food Supply, Power Level, AI Trust, and Morality.
- AI monitoring messages, automatic doors, room atmosphere effects, and lightweight placeholder 3D props.

## Controls

| Input | Action |
| --- | --- |
| `W`, `A`, `S`, `D` | Move |
| Mouse | Look |
| `SHIFT` | Sprint |
| `E` | Interact |
| `N` | End day after the main objective |
| `ESC` | Pause or resume |
| `F11` | Toggle fullscreen |

Pause menu controls:

| Input | Action |
| --- | --- |
| `S` | Save |
| `L` | Load |
| `C` | Restore checkpoint |
| `O` | Open settings |

Settings menu controls:

| Input | Action |
| --- | --- |
| `LEFT`, `RIGHT` | Adjust master volume |
| `DOWN`, `UP` | Adjust effects volume |

Mini-games show their required keys on screen.

## Technologies

- Java 17 bytecode target
- libGDX 1.14.0
- LWJGL3 desktop backend
- Gradle wrapper
- Launch4j EXE wrapper configuration

## How To Run

Requirements:

- Java JDK 17 or newer on the PATH
- Windows, Linux, or macOS for the Gradle/libGDX desktop task

From the repository root on Windows:

```bat
gradlew.bat desktop:run
```

## How To Build JAR

Use the helper script on Windows:

```bat
build.bat
```

Or call Gradle directly:

```bat
gradlew.bat desktop:jar
```

The runnable JAR is created at:

```text
desktop/build/libs/LAST_SHELTER.jar
```

Run it with:

```bat
java -jar desktop\build\libs\LAST_SHELTER.jar
```

## How To Build EXE

The EXE export uses Launch4j and the included `launch4j.xml`.

1. Install Launch4j.
2. Set `LAUNCH4J_HOME` to the Launch4j installation folder.
3. Run:

```bat
export_exe.bat
```

The final EXE file name is:

```text
dist/LAST_SHELTER.exe
```

The Launch4j wrapper expects Java 17 or newer on the target machine.

## Design Patterns

- State Pattern: bunker states `NORMAL`, `EMERGENCY`, `CRITICAL`, `ISOLATION`, and `SHUTDOWN`.
- Observer Pattern: `EventBus` publishes game and bunker lifecycle events.
- Strategy Pattern: `LogicalStrategy`, `HumanisticStrategy`, and `StrictStrategy` define AI messaging policies.
- Singleton Pattern: `BunkerCore` owns shared bunker services.
- Command Pattern: repair, exploration, rest, and log-analysis commands represent player/system actions.
- Template Method Pattern: bunker events use `AbstractBunkerEvent` with `trigger()`, `evaluate()`, and `resolve()`.

## Team Responsibilities

Student 1:

- UI/screens
- HUD
- room design
- interaction system
- documentation
- GitHub management

Student 2:

- AI monitoring system
- survival system
- mini-games
- design patterns
- save/load
- testing/debugging

Both students contributed equally to the project.

