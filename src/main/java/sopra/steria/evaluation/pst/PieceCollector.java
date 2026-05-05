package sopra.steria.evaluation.pst;

import knight.clubbing.core.BBoard;
import knight.clubbing.core.BPiece;

import java.util.HashMap;
import java.util.Map;

public class PieceCollector {

    public record PieceInfo(int square, int pieceCode, int pieceType, boolean isWhite) {}

    public static Map<Integer, PieceInfo> collectPieces(BBoard board) {
        Map<Integer, PieceInfo> bySquare = new HashMap<>();
        int[] pieceBoards = board.getPieceBoards();

        collectColor(board, pieceBoards, BBoard.whiteIndex, true, bySquare);
        collectColor(board, pieceBoards, BBoard.blackIndex, false, bySquare);

        return bySquare;
    }

    // Met dank aan AI, ik snap deze calculatie niet. Ik ben gaar.
    private static void collectColor(BBoard board, int[] pieceBoards, int colorIndex, boolean isWhite, Map<Integer, PieceInfo> out) {
        long bitboard = board.getColorBitboard(colorIndex);

        while (bitboard != 0L) {
            int sq = Long.numberOfTrailingZeros(bitboard);
            bitboard &= (bitboard - 1);

            int pieceCode = pieceBoards[sq];
            int pieceType = BPiece.getPieceType(pieceCode);

            out.put(sq, new PieceInfo(sq, pieceCode, pieceType, isWhite));
        }
    }
}
