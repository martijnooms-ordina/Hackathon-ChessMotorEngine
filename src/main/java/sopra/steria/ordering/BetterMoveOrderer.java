package sopra.steria.ordering;

import knight.clubbing.core.BBoard;
import knight.clubbing.core.BMove;
import knight.clubbing.core.BPiece;
import sopra.steria.values.PieceValues;

/**
 * Improved move orderer. Good move ordering is critical for alpha-beta search:
 * trying the best moves first maximises cutoffs and allows the engine to search
 * much deeper within the same time limit.
 *
 * Priority (highest first):
 *  1. Queen promotions                      (PROMOTION_BONUS + queen value)
 *  2. Under-promotions                      (PROMOTION_BONUS + piece value)
 *  3. Captures – MVV-LVA ordered            (CAPTURE_BONUS + 10*victim - attacker)
 *     "Most Valuable Victim – Least Valuable Attacker":
 *     pawn×queen is tried before queen×queen, which is tried before queen×pawn
 *  4. En passant                            (CAPTURE_BONUS + pawn×pawn score)
 *  5. Castling                              (CASTLE_BONUS)
 *  6. All other quiet moves                 (0)
 */
public class BetterMoveOrderer implements MoveOrderer {

    // Base offsets ensure the categories never overlap.
    // Quiet-move history scores are in [0, HISTORY_MAX] < CASTLE_BONUS < CAPTURE_BONUS.
    private static final int PROMOTION_BONUS = 20_000;
    private static final int CAPTURE_BONUS   = 10_000;
    private static final int CASTLE_BONUS    =  1_000;

    /**
     * History table shared with Search. historyTable[from][to] reflects how
     * often that quiet move caused a beta-cutoff across the search tree.
     * Null when no history is available (falls back to score 0 for quiet moves).
     */
    private int[][] historyTable = null;

    /** Called once by Search to wire up the shared history table. */
    public void setHistoryTable(int[][] historyTable) {
        this.historyTable = historyTable;
    }

    @Override
    public void orderMoves(BMove[] moves, BBoard board) {
        int[] scores = new int[moves.length];
        int[] pieceBoards = board.getPieceBoards();

        for (int i = 0; i < moves.length; i++) {
            scores[i] = score(moves[i], pieceBoards);
        }

        insertionSort(moves, scores);
    }

    private int score(BMove move, int[] pieceBoards) {
        // ── Promotions ────────────────────────────────────────────────────────
        if (move.isPromotion()) {
            return PROMOTION_BONUS + pieceValue(move.promotionPieceType());
        }

        int movingPiece = pieceBoards[move.startSquare()];
        int movingType  = BPiece.getPieceType(movingPiece);

        // ── Captures (MVV-LVA) ────────────────────────────────────────────────
        int targetPiece = pieceBoards[move.targetSquare()];
        if (targetPiece != BPiece.none) {
            int victimType = BPiece.getPieceType(targetPiece);
            // 10× victim so victim value always dominates over attacker value
            return CAPTURE_BONUS
                    + 10 * pieceValue(victimType)
                    -      pieceValue(movingType);
        }

        // ── En passant (pawn captures pawn) ───────────────────────────────────
        if (move.moveFlag() == BMove.enPassantCaptureFlag) {
            return CAPTURE_BONUS
                    + 10 * PieceValues.PAWN_VALUE
                    -      PieceValues.PAWN_VALUE;
        }

        // ── Castling ──────────────────────────────────────────────────────────
        if (move.isCastle()) {
            return CASTLE_BONUS;
        }

        // ── Quiet moves: ordered by history score ─────────────────────────────
        // Moves that frequently caused beta-cutoffs get a higher score and are
        // tried earlier, maximising alpha-beta pruning.
        if (historyTable != null) {
            return historyTable[move.startSquare()][move.targetSquare()];
        }
        return 0;
    }

    private static int pieceValue(int pieceType) {
        return switch (pieceType) {
            case BPiece.pawn   -> PieceValues.PAWN_VALUE;
            case BPiece.knight -> PieceValues.KNIGHT_VALUE;
            case BPiece.bishop -> PieceValues.BISHOP_VALUE;
            case BPiece.rook   -> PieceValues.ROOK_VALUE;
            case BPiece.queen  -> PieceValues.QUEEN_VALUE;
            default            -> 0;
        };
    }

    /**
     * Insertion sort – stable, in-place, O(n²) worst case but very efficient
     * for the typically small move lists in chess (20-40 moves per position).
     * Sorts descending (highest score first).
     */
    private static void insertionSort(BMove[] moves, int[] scores) {
        for (int i = 1; i < moves.length; i++) {
            BMove move  = moves[i];
            int   score = scores[i];
            int   j     = i - 1;
            while (j >= 0 && scores[j] < score) {
                moves[j + 1]  = moves[j];
                scores[j + 1] = scores[j];
                j--;
            }
            moves[j + 1]  = move;
            scores[j + 1] = score;
        }
    }
}
