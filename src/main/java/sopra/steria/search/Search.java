package sopra.steria.search;

import knight.clubbing.core.BBoard;
import knight.clubbing.core.BMove;
import knight.clubbing.movegen.MoveGenerator;
import sopra.steria.evaluation.BetterEvaluator;
import sopra.steria.evaluation.Evaluator;
import sopra.steria.ordering.BadMoveOrderer;
import sopra.steria.ordering.MoveOrderer;

import static sopra.steria.EngineConst.INF;
import static sopra.steria.EngineConst.MATE_SCORE;

public class Search {

    private volatile boolean stop;
    private long startTime;
    private SearchSetting setting;
    private long nodes;

    private final Evaluator evaluator;
    private final MoveOrderer moveOrderer;

    public Search() {
        this.evaluator = new BetterEvaluator();
        this.moveOrderer = new BadMoveOrderer();
    }

    public SearchResult bestMove(BBoard board, SearchSetting setting) {
        this.startTime = System.currentTimeMillis();
        this.setting = setting;
        this.stop = false;

        SearchResult bestResult = new SearchResult();
        bestResult.setScore(-INF);


        for (int depth = 1; depth <= setting.maxDepth(); depth++) {
            try {
                SearchResult result = searchDepth(board, depth);
                bestResult = result;

                checkStop();

                long elapsed = getTimeTakenMillis();
                String pv = result.getBestMove() != null ? result.getBestMove() : "";
                System.out.println("info depth " + depth + " score cp " + result.getScore() + " time " + elapsed + " pv " + pv);

                if (isDecisive(result)) break;
            } catch (SearchInterruptedException e) {
                break;
            }

        }

        bestResult.setTimeTakenMillis(getTimeTakenMillis());
        return bestResult;
    }

    private SearchResult searchDepth(BBoard board, int depth) {
        SearchResult bestResult = new SearchResult();
        bestResult.setScore(-INF);
        bestResult.setDepth(depth);
        int alpha = -INF;
        int beta = INF;
        this.nodes = 0;

        BMove[] moves = new MoveGenerator(board).generateMoves(false);

        for (BMove move : moves) {
            checkStop();

            board.makeMove(move, true);
            int score = -negamax(board, depth - 1, -beta, -alpha, 1);
            board.undoMove(move, true);

            if (score > bestResult.getScore()) {
                bestResult.setScore(score);
                bestResult.setBestMove(move.getUci());
            }

            alpha = Math.max(alpha, score);
        }

        bestResult.setNodesSearched(this.nodes);
        return bestResult;
    }

    private int negamax(BBoard board, int depth, int alpha, int beta, int ply) {
        nodes++;

        if (isNthNode(1023))
            checkStop();

        if (depth <= 0) return evaluator.evaluate(board);

        int bestScore = -INF;

        BMove[] nextMoves = new MoveGenerator(board).generateMoves(false);

        moveOrderer.orderMoves(nextMoves, board);

        if (nextMoves.length == 0) {
            if (board.isInCheck())
                return -MATE_SCORE + ply;
            else
                return 0;
        }

        if (board.isDrawByRepetition()) {
            return 0;
        }

        for (BMove move : nextMoves) {
            board.makeMove(move, true);
            int score = -negamax(board, depth - 1, -beta, -alpha, ply + 1);
            board.undoMove(move, true);

            bestScore = Math.max(bestScore, score);
            alpha = Math.max(alpha, score);

            if (alpha >= beta) {
                break;
            }
        }

        return bestScore;
    }

    private boolean isNthNode(int n) {
        return (nodes & n) == 0;
    }

    private boolean isDecisive(SearchResult result) {
        return Math.abs(result.getScore()) >= MATE_SCORE - result.getDepth();
    }

    private long getTimeTakenMillis() {
        return System.currentTimeMillis() - startTime;
    }

    private void checkStop() {
        if (stop) throw new SearchInterruptedException();
        if (Thread.currentThread().isInterrupted()) {
            stop = true;
            throw new SearchInterruptedException();
        }

        if (setting.timeLimit() > 0 && getTimeTakenMillis() >= setting.timeLimit()) {
            stop = true;
            throw new SearchInterruptedException();
        }
    }
}
