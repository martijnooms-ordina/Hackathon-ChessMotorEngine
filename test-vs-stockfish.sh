#!/bin/zsh
# ============================================================
# test-vs-stockfish.sh
#
# Bouwt de engine en speelt een match tegen Stockfish via
# cutechess-cli. Pas de variabelen hieronder aan naar wens.
#
# Vereisten:
#   brew install stockfish
#   cutechess-cli gebouwd in /tmp/cutechess-build/cutechess-cli
#   (zie README of gebruik het build-commando onderaan)
# ============================================================

set -e

# ── Configuratie ─────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/Hackathon-ChessEngine-1.0-SNAPSHOT.jar"
CUTECHESS_CLI="/tmp/cutechess-build/cutechess-cli"
ENGINE_WRAPPER="/tmp/run_engine.sh"
RESULTS_PGN="/tmp/match_results.pgn"

STOCKFISH_ELO="${1:-1500}"   # Eerste argument = Elo (standaard 1500)
NUM_GAMES="${2:-10}"          # Tweede argument = aantal potjes (standaard 10)
TIME_CONTROL="${3:-10+0.1}"  # Derde argument = tijdscontrole (standaard 10s + 0.1s inc)

# ── Controleer vereisten ──────────────────────────────────────
if ! command -v stockfish &>/dev/null; then
  echo "Stockfish niet gevonden. Installeer met: brew install stockfish"
  exit 1
fi

if [[ ! -x "$CUTECHESS_CLI" ]]; then
  echo "cutechess-cli niet gevonden op $CUTECHESS_CLI"
  echo ""
  echo "Bouw het met:"
  echo "  brew install qt@6 cmake"
  echo "  git clone --depth=1 https://github.com/cutechess/cutechess.git /tmp/cutechess"
  echo "  cmake -S /tmp/cutechess -B /tmp/cutechess-build -DCMAKE_PREFIX_PATH=\$(brew --prefix qt@6)"
  echo "  cmake --build /tmp/cutechess-build -j4"
  exit 1
fi

# ── Build de engine JAR ───────────────────────────────────────
echo "Engine bouwen..."
mvn package -DskipTests -q -f "$SCRIPT_DIR/pom.xml"

if [[ ! -f "$JAR" ]]; then
  echo "JAR niet gevonden na build: $JAR"
  exit 1
fi

# ── Maak wrapper script ───────────────────────────────────────
cat > "$ENGINE_WRAPPER" << EOF
#!/bin/zsh
exec java -jar "$JAR"
EOF
chmod +x "$ENGINE_WRAPPER"

# ── Start match ───────────────────────────────────────────────
echo ""
echo "=============================================="
echo "  ChessSteamEngine vs Stockfish (Elo $STOCKFISH_ELO)"
echo "  $NUM_GAMES potjes | tijdscontrole: $TIME_CONTROL"
echo "=============================================="
echo ""

"$CUTECHESS_CLI" \
  -engine cmd="$ENGINE_WRAPPER" name=ChessSteamEngine proto=uci \
  -engine cmd=stockfish name="Stockfish-$STOCKFISH_ELO" proto=uci \
    option.UCI_LimitStrength=true option.UCI_Elo="$STOCKFISH_ELO" \
  -each tc="$TIME_CONTROL" \
  -games "$NUM_GAMES" -rounds 1 \
  -pgnout "$RESULTS_PGN" \
  -concurrency 1

echo ""
echo "PGN opgeslagen in: $RESULTS_PGN"

# ── Gebruik ───────────────────────────────────────────────────
# Standaard (1500 Elo, 10 potjes):   ./test-vs-stockfish.sh
# Andere Elo (1800, 20 potjes):      ./test-vs-stockfish.sh 1800 20
# Met andere tijdscontrole:          ./test-vs-stockfish.sh 2000 10 5+0.05
