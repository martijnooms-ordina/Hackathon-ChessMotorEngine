package sopra.steria.evaluation;

import knight.clubbing.core.BBoard;
import knight.clubbing.core.BBoardHelper;
import knight.clubbing.core.BPiece;
import sopra.steria.values.PSTValues;
import sopra.steria.values.PieceValues;

/**
 * Improved evaluator with:
 *  - Differentiated piece values (pawn=100, knight=320, bishop=330, rook=500, queen=900)
 *  - Piece-Square Tables (Simplified Evaluation Function by Michniewski)
 *  - Game phase tapering for king safety (middlegame vs endgame)
 *  - Pawn structure (doubled, isolated, passed pawns)
 *  - Bishop pair bonus
 *
 * PST convention: index 0 = a8 (rank 8, white's advanced side), index 63 = h1.
 *   For WHITE pieces at engine square sq: pstIndex = sq ^ 56
 *   For BLACK pieces at engine square sq: pstIndex = sq
 */
public class BetterEvaluator implements Evaluator {

    // -------------------------------------------------------------------------
    // Bonuses / penalties
    // -------------------------------------------------------------------------
    private static final int BISHOP_PAIR_BONUS     =  30;
    private static final int DOUBLED_PAWN_PENALTY  = -20;  // per extra pawn on a file
    private static final int ISOLATED_PAWN_PENALTY = -15;  // no friendly pawns on adjacent files
    private static final int PASSED_PAWN_BONUS     =  20;  // multiplied by ranks advanced

    /**
     * Maximum non-pawn, non-king material for both sides combined.
     * Used to compute the game phase (0 = full midgame, 1 = full endgame).
     */
    private static final int MAX_PHASE_MATERIAL =
            2 * (2 * PieceValues.KNIGHT_VALUE + 2 * PieceValues.BISHOP_VALUE + 2 * PieceValues.ROOK_VALUE + PieceValues.QUEEN_VALUE);

    // =========================================================================
    // Evaluator interface
    // =========================================================================

    @Override
    public int evaluate(BBoard board) {
        int score = evaluateSide(board, true) - evaluateSide(board, false);
        return board.isWhiteToMove() ? score : -score;
    }

    // =========================================================================
    // Per-side evaluation
    // =========================================================================

    private int evaluateSide(BBoard board, boolean isWhite) {
        int color      = isWhite ? BPiece.white : BPiece.black;
        int colorIndex = isWhite ? BBoard.whiteIndex : BBoard.blackIndex;

        long pawns   = board.getBitboard(BPiece.makePiece(BPiece.pawn,   color));
        long knights = board.getBitboard(BPiece.makePiece(BPiece.knight, color));
        long bishops = board.getBitboard(BPiece.makePiece(BPiece.bishop, color));
        long rooks   = board.getBitboard(BPiece.makePiece(BPiece.rook,   color));
        long queens  = board.getBitboard(BPiece.makePiece(BPiece.queen,  color));

        float phase = getGamePhase(board);
        int score = 0;

        // Material + Piece-Square Tables
        score += sumPieces(pawns,   PieceValues.PAWN_VALUE,   PSTValues.PAWN_PST,   isWhite);
        score += sumPieces(knights, PieceValues.KNIGHT_VALUE, PSTValues.KNIGHT_PST, isWhite);
        score += sumPieces(bishops, PieceValues.BISHOP_VALUE, PSTValues.BISHOP_PST, isWhite);
        score += sumPieces(rooks,   PieceValues.ROOK_VALUE,   PSTValues.ROOK_PST,   isWhite);
        score += sumPieces(queens,  PieceValues.QUEEN_VALUE,  PSTValues.QUEEN_PST,  isWhite);

        // King safety: taper between middlegame and endgame PST
        int kingSq    = board.getKingSquare(colorIndex);
        int kingPstIdx = isWhite ? (kingSq ^ 56) : kingSq;
        score += (int) (PSTValues.KING_MG_PST[kingPstIdx] * (1 - phase) + PSTValues.KING_EG_PST[kingPstIdx] * phase);

        // Bishop pair bonus
        if (Long.bitCount(bishops) >= 2) score += BISHOP_PAIR_BONUS;

        // Pawn structure
        score += evaluatePawnStructure(pawns, isWhite, board);

        return score;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Sums material value + PST bonus for all pieces on the given bitboard.
     */
    private int sumPieces(long bb, int pieceValue, int[] pst, boolean isWhite) {
        int score = 0;
        while (bb != 0) {
            int sq  = Long.numberOfTrailingZeros(bb);
            bb     &= bb - 1; // clear lowest set bit
            int idx = isWhite ? (sq ^ 56) : sq;
            score  += pieceValue + pst[idx];
        }
        return score;
    }

    /**
     * Evaluates pawn structure: doubled, isolated and passed pawns.
     */
    private int evaluatePawnStructure(long pawns, boolean isWhite, BBoard board) {
        int score = 0;
        long opponentPawns = board.getBitboard(
                BPiece.makePiece(BPiece.pawn, isWhite ? BPiece.black : BPiece.white));

        // Doubled pawns: for each file with more than one pawn apply a penalty
        // per extra pawn (e.g. 2 pawns on same file → 1 penalty).
        for (int file = 0; file < 8; file++) {
            int count = Long.bitCount(pawns & BBoardHelper.FILE_MASKS[file]);
            if (count > 1) score += (count - 1) * DOUBLED_PAWN_PENALTY;
        }

        // Isolated and passed pawns (iterate over individual pawns)
        long temp = pawns;
        while (temp != 0) {
            int sq   = Long.numberOfTrailingZeros(temp);
            temp    &= temp - 1;
            int file = BBoardHelper.fileIndex(sq);
            int rank = BBoardHelper.rankIndex(sq);

            // Isolated pawn: no friendly pawns on adjacent files
            long adjacentFiles = 0L;
            if (file > 0) adjacentFiles |= BBoardHelper.FILE_MASKS[file - 1];
            if (file < 7) adjacentFiles |= BBoardHelper.FILE_MASKS[file + 1];
            if ((pawns & adjacentFiles) == 0) score += ISOLATED_PAWN_PENALTY;

            // Passed pawn: no opponent pawns on same or adjacent files ahead
            if (isPassedPawn(file, rank, isWhite, opponentPawns)) {
                int ranksAdvanced = isWhite ? rank : (7 - rank);
                score += PASSED_PAWN_BONUS * ranksAdvanced;
            }
        }

        return score;
    }

    /**
     * Returns true when no opponent pawn can ever block or capture this pawn.
     */
    private boolean isPassedPawn(int file, int rank, boolean isWhite, long opponentPawns) {
        long passedMask = 0L;
        for (int f = Math.max(0, file - 1); f <= Math.min(7, file + 1); f++) {
            long fileMask = BBoardHelper.FILE_MASKS[f];
            if (isWhite) {
                // All squares on this file with rank > current rank
                long aheadMask = (rank < 7) ? (~0L << ((rank + 1) * 8)) : 0L;
                passedMask |= fileMask & aheadMask;
            } else {
                // All squares on this file with rank < current rank
                long aheadMask = (rank > 0) ? (~0L >>> (64 - rank * 8)) : 0L;
                passedMask |= fileMask & aheadMask;
            }
        }
        return (opponentPawns & passedMask) == 0;
    }

    /**
     * Returns 0.0 for full middlegame, 1.0 for full endgame,
     * based on the total non-pawn, non-king material still on the board.
     */
    private float getGamePhase(BBoard board) {
        int material = 0;
        for (int color : new int[]{BPiece.white, BPiece.black}) {
            material += PieceValues.KNIGHT_VALUE * Long.bitCount(board.getBitboard(BPiece.makePiece(BPiece.knight, color)));
            material += PieceValues.BISHOP_VALUE * Long.bitCount(board.getBitboard(BPiece.makePiece(BPiece.bishop, color)));
            material += PieceValues.ROOK_VALUE   * Long.bitCount(board.getBitboard(BPiece.makePiece(BPiece.rook,   color)));
            material += PieceValues.QUEEN_VALUE  * Long.bitCount(board.getBitboard(BPiece.makePiece(BPiece.queen,  color)));
        }
        return 1.0f - Math.min(1.0f, (float) material / MAX_PHASE_MATERIAL);
    }
}
