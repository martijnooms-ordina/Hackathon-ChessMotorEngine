<h1>Hackathon-ChessEngine</h1>

A chess engine to be developed during a hackathon.
<br/>
For competing, fork this repository. Please refer to the [Repository use for hackathon](#repository-use-for-hackathon) section.

<!-- TOC -->
* [Scope of Hackathon](#scope-of-hackathon)
* [Use of Repository](#use-of-repository)
  * [Setup](#setup)
  * [Workflow engine development](#workflow-engine-development)
  * [Repository structure](#repository-structure)
    * [Engine code](#engine-code)
    * [SPRT folder](#sprt-folder)
* [Development](#development)
  * [Guidelines](#guidelines)
    * [Fundamental](#fundamental)
    * [Intermediate](#intermediate)
    * [Advanced](#advanced)
  * [Sources & help](#sources--help)
  * [SPRT testing](#sprt-testing)
    * [Using SPRT tests](#using-sprt-tests)
      * [test_sprt.sh parameters](#test_sprtsh-parameters)
* [Bonus](#bonus)
  * [Profiles](#profiles)
<!-- TOC -->

# Scope of Hackathon
You have absolute freedom to modify and improve the chess engine in any way you see fit, but there are some restrictions.

Restrictions:
- Must be based on this codebase (java 17, maven).
- Must be single threaded: no threads created or parallelism.
- No external sources allowed, including API's, pre-trained models, developed engines, etc. 
 
Have fun and be creative!

# Use of Repository
Everything to develop this chess engine.
Prerequisites:
- Java 17 or higher
- Maven
- Git

## Setup
1. Fork this repository and clone it to your local machine.
2. Clone the [KnightClubbingLogic repository](https://github.com/rpjvanaert/KnightClubbingLogic#)
3. Build KnightClubbingLogic and install it to your local maven repository:
```
mvn clean install
```
4. Build the base version of this repository:
```
mvn clean package
```
5. Copy the jar twice to test-sprt/engines/:
  - One as `Hackathon-ChessEngine-1.0-SNAPSHOT.jar` (your development version)
  - One as `Hackathon-ChessEngine-1.0.jar` (your base version)
6. Run the SPRT tests to make sure everything is working:
```shell
# first go into the folder, 'cd test-sprt'
docker compose up --build
# or
podman compose up --build
```
7. Once you see matches running and results, you can stop the containers. You are ready to begin!

## Workflow engine development
1. Make sure you have a base version built and working with SPRT tests. (see above)
2. Make your changes to the engine.
3. Build the new version and copy the jar to the `test-sprt/engines` directory. 
4. Make sure the docker-compose.yml points to the correct version-tags, first one to the base- and second to change version, see [sprt test paramaters](#test_sprtsh-parameters).
5. Run the SPRT tests to see if it improved the engine. See [Using SPRT tests](#using-sprt-tests) for instructions.
6. If it improved, commit the changes.

## Repository structure
Repository can be divided into two main parts: engine code and sprt folder.
### Engine code
The engine code is in the `src` folder. Within is a standard maven structure, it contains:
- `App.java`: Application start point.*
- `Uci.java`: UCI protocol implementation run by App.*
- `EngineConst.java`: Constants used in the engine.
- `search`: folder for search
- `evaluation`: folder for evaluation
- `ordering`: folder for move ordering

\* = For hackathon, you should probably not change these.

### SPRT folder
The `test-sprt` folder contains everything related to the SPRT testing of the engine. For use see [SPRT testing](#sprt-testing) section.
- `engines`: folder for testing engine jars, copy yours here.
- `test_sprt.sh`: script to run the SPRT tests.*
- `Dockerfile`: Dockerfile to build the image for testing.*
- `docker-compose.yml`: Compose file to spin up testing environment. You can adjust [parameters](#test_sprtsh-parameters) when testing different versions.
- `sprt_presests.ini`: Presets for SPRT test configs.*
- `output/games.pgn`: Output file for games played after running SPRT tests.

# Development
The goal of this hackathon is to learn chess engine development and have fun improving the engine.
## Guidelines
In order to decide what to implement in your engine, I recommend you do it step by step and slowly challenge yourself more.
<br/>
To categorize the steps there are: fundamental, intermediate and advanced steps.
### Fundamental
- Material evaluation
- Piece-square tables (PST)
- MVV-LVA ordering
- Promotions ordering

### Intermediate
- Pawn structure evaluation*
- Killer moves
- Quiescence search
- Mobility
- Other evaluation *
  - Bishop pair
  - King safety
  - etc.

\* = Can become too much or too complex, do basic implementation.

### Advanced
- Principal Variation (PV) move ordering
- Transposition table
- History heuristic
- Null move pruning
- Late move reductions (LMR)

## Sources & help
- [Chess Programming Wiki](https://www.chessprogramming.org/Main_Page)
- [Sebasian Lague's Chess Engine Video's (Episode 1 & 2)](https://www.youtube.com/watch?v=U4ogK0MIzqk&list=PLFt_AvWsXl0cvHyu32ajwh2qU1i6hl77c)
- Google for topics you want to understand better
- LLM's can be helpful to understand concepts. Don't copy or trust them blindly.

## SPRT testing
SPRT tests are used to see if your changes improved the engine or not.
It runs games between base and new version until conclusion is reached.

### Using SPRT tests
To run the SPRT tests, first build the engine jar you want to test. 
Make sure both base and new version jars are in the `test-sprt/engines` directory.
Then run the docker compose setup to execute the tests. 
By default, it will run the test_sprt.sh script with '1.0' as base version and '1.0-SNAPSHOT' as new version.
If you built different versions, change the parameters accordingly in the `docker-compose.yml` file. Also see `test-sprt/test_sprt.sh` for details.

Rebuild image to pick up the engines:
```shell
# first go into the folder, 'cd test-sprt'
docker compose up --build
# or podman:
podman compose up --build
```
- H0 accepted: The new version is not better than the base version.
- H1 accepted: The new version is better than the base version.
- LOS: Likelihood of superiority of new over base version.

#### test_sprt.sh parameters
The docker image runs the `test_sprt.sh` as entrypoint with 3 parameters:
- $1: Base engine version (tag of the jar without .jar, f.e. '1.0')
- $2: New engine version (see above)
- $3: sprt_presets.ini preset name (default: 'default')

In the docker-compose.yml file you can change these parameters.

# Performance Improvements

Starting from the `BadEvaluator` (all pieces worth 100 cp, no positional knowledge), the following
improvements were implemented during the hackathon. Each step was validated by playing matches
against the previous version or against Stockfish at fixed Elo.

## Implemented features

### Evaluation (`BetterEvaluator`)
| Feature | Description |
|---|---|
| Piece values | Pawn 100, Knight 320, Bishop 330, Rook 500, Queen 900 cp |
| Piece-Square Tables | Michniewski simplified evaluation tables for all piece types |
| Game-phase tapering | King PST interpolated between middlegame and endgame tables |
| Pawn structure | Doubled pawn penalty, isolated pawn penalty, passed pawn bonus |
| Bishop pair bonus | +30 cp when both bishops are present |

### Move ordering (`BetterMoveOrderer`)
| Feature | Description |
|---|---|
| MVV-LVA | Most Valuable Victim – Least Valuable Attacker for captures |
| Promotion bonus | Queen promotions tried before other moves |
| Castle bonus | Castling moves prioritised over random quiet moves |
| Insertion sort | Replaces the original bubble sort |

### Search (`Search`)
| Feature | Description |
|---|---|
| Quiescence search | Captures-only search at leaf nodes to eliminate horizon effect |
| Transposition table | 16 MB hash table (1M entries) with always-replace strategy |
| Killer moves | 2 quiet cutoff moves stored per ply, tried before other quiet moves |
| Null move pruning | R=2 reduction; skipped in check, endgame, and after another null move |
| Contempt factor | Draw by repetition scored as -10 cp instead of 0 |
| Opening book | Hardcoded first 2-4 moves for common openings (1.e4, Italian, etc.) |
| Search depth | Increased from 6 to 7 |

## Performance vs Stockfish (10 games, 10s+0.1s increment)

| Stockfish Elo | Score | Elo difference |
|---|---|---|
| 1500 | 90% (9W/1L/0D) | +382 |
| 1800 | 75% (approx.) | +107 |
| 2000 | 80% (7W/1L/2D) | +241 |

Estimated engine strength: **~2200 Elo**

## Testing vs Stockfish

A convenience script is included to run matches against Stockfish:

```shell
# Requires: brew install stockfish
# Requires: cutechess-cli built in /tmp/cutechess-build/

# 10 games vs Stockfish 1500 (default)
./test-vs-stockfish.sh

# 20 games vs Stockfish 1800
./test-vs-stockfish.sh 1800 20

# 10 games vs Stockfish 2000 with shorter time control
./test-vs-stockfish.sh 2000 10 5+0.05
```

Build cutechess-cli (one-time setup):
```shell
brew install qt@6 cmake
git clone --depth=1 https://github.com/cutechess/cutechess.git /tmp/cutechess
cmake -S /tmp/cutechess -B /tmp/cutechess-build -DCMAKE_PREFIX_PATH=$(brew --prefix qt@6)
cmake --build /tmp/cutechess-build -j4
```

# Bonus

## Profiles
There are 2 additional profiles defined:
- `ci`: for consistent CI builds.
- `strength`: for testing strength of the engine with bestmove puzzles.
- default: doesn't run strength tests.

Running strength tests:
```shell
mvn test -Pstrength
```
Note: they don't have to pass, more an indication of strength.
