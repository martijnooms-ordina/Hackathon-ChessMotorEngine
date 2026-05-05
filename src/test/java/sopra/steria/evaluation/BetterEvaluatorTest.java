package sopra.steria.evaluation;

import knight.clubbing.core.BBoard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BetterEvaluatorTest {

    @Test
    void testScore() {
        BetterEvaluator evaluator = new BetterEvaluator();
        BBoard board = new BBoard();
        int score = evaluator.evaluate(board);
        System.out.println("Score = ["+score+"]");
    }

    @Test
    void testScoreWithFen() {
        String fen = "6k1/5ppp/5r2/8/8/8/5PPP/3R2K1 w - - 0 1";
        BetterEvaluator evaluator = new BetterEvaluator();
        BBoard board = new BBoard(fen);
        int score = evaluator.evaluate(board);
        System.out.println("Score = ["+score+"]");
    }
}
