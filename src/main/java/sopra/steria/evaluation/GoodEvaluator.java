package sopra.steria.evaluation;

import knight.clubbing.core.BBoard;
import sopra.steria.evaluation.pst.PstEvaluator;

public class GoodEvaluator implements Evaluator {

    private final PstEvaluator pstEvaluator = new PstEvaluator();
    //private final MaterialEvaluator materialEvaluator = new MaterialEvaluator();

    @Override
    public int evaluate(BBoard board) {
        int score = 0;
        score += pstEvaluator.evaluatePST(board);
        //score += materialEvaluator.evaluate(board);
        return score;
    }

}
