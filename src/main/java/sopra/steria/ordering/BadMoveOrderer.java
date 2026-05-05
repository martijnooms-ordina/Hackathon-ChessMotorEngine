package sopra.steria.ordering;

import knight.clubbing.core.BBoard;
import knight.clubbing.core.BBoardHelper;
import knight.clubbing.core.BMove;
import sopra.steria.ordering.MVVLVA.GoodMVVLVA;

public class BadMoveOrderer implements MoveOrderer {

    @Override
    public void orderMoves(BMove[] moves, BBoard board) {
        int[] scores = new int[moves.length];

        for (int i = 0; i < moves.length; i++) {
            scores[i] = score(moves[i], board);
        }

        sortMovesByScore(moves, scores);
    }

    private int score(BMove move, BBoard board) {
        int score = 0;

        //what piece
        int movingPiece = board.getPieceBoards()[move.startSquare()];

        int rank = BBoardHelper.rankIndex(move.startSquare());

        //if target squaire not empty, calc MVVLVA
        int capturePiece = board.getPieceBoards()[move.targetSquare()];
        int mvvLvaScore = GoodMVVLVA.MVVLVAScore(capturePiece, movingPiece);
        score += mvvLvaScore;


        // 250 points if the move is a pawn move to the center (rank 5 for white, rank 2 for black)
        //wtf is rank
        if (board.isWhiteToMove() && rank == 5 || !board.isWhiteToMove() && rank == 2)
            score += 250;

        return score;
    }

    private void sortMovesByScore(BMove[] moves, int[] scores) {
        for (int i = 0; i < moves.length - 1; i++) {
            for (int j = i + 1; j < moves.length; j++) {
                if (scores[j] > scores[i]) {
                    // Swap moves
                    BMove tempMove = moves[i];
                    moves[i] = moves[j];
                    moves[j] = tempMove;

                    // Swap scores
                    int tempScore = scores[i];
                    scores[i] = scores[j];
                    scores[j] = tempScore;
                }
            }
        }
    }
}
