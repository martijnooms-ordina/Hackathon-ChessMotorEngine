package sopra.steria.evaluation;

import knight.clubbing.core.BBoard;
import knight.clubbing.core.BPiece;

public class BadEvaluator implements Evaluator {
    private static final int PIECE_VALUE = 100;
    private static final int PAWN_PIECE_VALUE = 100;        // Pion
    private static final int KNIGHT_PIECE_VALUE = 300;      // Paard
    private static final int BISHOP_PIECE_VALUE = 310;      // Loper
    private static final int ROOK_PIECE_VALUE = 500;       // Toren
    private static final int QUEEN_PIECE_VALUE = 900;       // Dame


    @Override
    public int evaluate(BBoard board) {
        int score = 0;

        // Me take opponent material good
        int whiteMaterial = PIECE_VALUE * Long.bitCount(board.getColorBitboard(BBoard.whiteIndex));
        int blackMaterial = PIECE_VALUE * Long.bitCount(board.getColorBitboard(BBoard.blackIndex));
        score += whiteMaterial - blackMaterial;

        return board.isWhiteToMove() ? score : -score;
    }


}
