package sopra.steria.search;

import knight.clubbing.core.BBoard;
import knight.clubbing.core.BMove;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Small hardcoded opening book.
 *
 * The engine's PST evaluation prefers 1.Nc3 over 1.e4 because the knight
 * gains more PST-score by jumping from b1 to c3 (+55 cp) than a pawn moving
 * from e2 to e4 (+40 cp). The opening book overrides the search for the first
 * few moves with well-known, principled responses.
 *
 * Keys are Zobrist hashes of the position BEFORE the book move is played.
 * Works for both colours: white entries fire when the engine plays white,
 * black entries fire when the engine plays black.
 */
public class OpeningBook {

    private final Map<Long, String> book = new HashMap<>();

    public OpeningBook() {

        // ── White: move 1 ─────────────────────────────────────────────────
        add(new String[]{},          "e2e4");   // 1. e4  (instead of 1.Nc3)

        // ── White: move 2 responses ───────────────────────────────────────
        add(new String[]{"e2e4", "e7e5"},  "g1f3");   // 1.e4 e5  → 2.Nf3
        add(new String[]{"e2e4", "c7c5"},  "g1f3");   // 1.e4 c5  → 2.Nf3 (Sicilian)
        add(new String[]{"e2e4", "e7e6"},  "d2d4");   // 1.e4 e6  → 2.d4  (French)
        add(new String[]{"e2e4", "c7c6"},  "d2d4");   // 1.e4 c6  → 2.d4  (Caro-Kann)
        add(new String[]{"e2e4", "d7d5"},  "e4d5");   // 1.e4 d5  → 2.exd5 (Scandinavian)
        add(new String[]{"e2e4", "g8f6"},  "e4e5");   // 1.e4 Nf6 → 2.e5  (Alekhine)
        add(new String[]{"e2e4", "b8c6"},  "d2d4");   // 1.e4 Nc6 → 2.d4
        add(new String[]{"e2e4", "d7d6"},  "d2d4");   // 1.e4 d6  → 2.d4  (Pirc)
        add(new String[]{"e2e4", "g7g6"},  "d2d4");   // 1.e4 g6  → 2.d4  (Modern)

        // ── White: move 3 (after 1.e4 e5 2.Nf3) ─────────────────────────
        add(new String[]{"e2e4", "e7e5", "g1f3", "b8c6"}, "f1c4"); // Italian Game
        add(new String[]{"e2e4", "e7e5", "g1f3", "g8f6"}, "d2d4"); // Petrov → 3.d4
        add(new String[]{"e2e4", "e7e5", "g1f3", "d7d6"}, "d2d4"); // Philidor → 3.d4
        add(new String[]{"e2e4", "e7e5", "g1f3", "f7f5"}, "g1e5"); // Latvian → Nxe5

        // ── White: move 4 (Italian follow-up) ────────────────────────────
        add(new String[]{"e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6"}, "d2d3"); // Giuoco Pianissimo
        add(new String[]{"e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5"}, "c2c3"); // Italian → c3

        // ── Black: response to 1.e4 ───────────────────────────────────────
        // Play 1...e5 instead of 1...Nc6 (Nimzowitsch)
        add(new String[]{"e2e4"}, "e7e5");

        // ── Black: response to 1.d4 ───────────────────────────────────────
        add(new String[]{"d2d4"},          "d7d5");   // 1.d4 → d5
        add(new String[]{"d2d4", "d7d5", "c2c4"},       "e7e6"); // QGD
        add(new String[]{"d2d4", "d7d5", "g1f3"},        "g8f6"); // 1.d4 d5 2.Nf3 → Nf6

        // ── Black: response to 1.Nf3 ─────────────────────────────────────
        add(new String[]{"g1f3"}, "g8f6");  // 1.Nf3 → Nf6

        // ── Black: response to 1.c4 ──────────────────────────────────────
        add(new String[]{"c2c4"}, "e7e6");  // 1.c4 → e6
    }

    /**
     * Registers a book move for the position reached after playing
     * {@code precedingMoves} from the starting position.
     */
    private void add(String[] precedingMoves, String bookMove) {
        BBoard board = new BBoard();
        for (String uci : precedingMoves) {
            board.makeMove(BMove.fromUci(uci, board), false);
        }
        book.put(board.getState().getZobristKey(), bookMove);
    }

    /**
     * Returns the book move for the given position, or empty if the position
     * is not in the book.
     */
    public Optional<String> getMove(long zobristKey) {
        return Optional.ofNullable(book.get(zobristKey));
    }
}
