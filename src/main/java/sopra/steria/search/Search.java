package sopra.steria.search;

import knight.clubbing.core.BBoard;
import knight.clubbing.core.BMove;
import knight.clubbing.core.BPiece;
import knight.clubbing.movegen.MoveGenerator;
import sopra.steria.evaluation.BetterEvaluator;
import sopra.steria.evaluation.Evaluator;
import sopra.steria.ordering.BetterMoveOrderer;
import sopra.steria.ordering.MoveOrderer;

import static sopra.steria.EngineConst.INF;
import static sopra.steria.EngineConst.MATE_SCORE;
import static sopra.steria.EngineConst.CONTEMPT;

public class Search {

    private volatile boolean stop;
    private long startTime;
    private SearchSetting setting;
    private long nodes;

    private final Evaluator evaluator;
    private final MoveOrderer moveOrderer;

    /**
     * Shared transposition table – static so it persists across iterative-deepening
     * iterations and across moves within the same game, maximising cache hits.
     */
    private static final TranspositionTable tt = new TranspositionTable();

    /** Opening book – overrides search for the first few moves. */
    private static final OpeningBook openingBook = new OpeningBook();

    /**
     * Killer moves: up to 2 quiet moves per ply that recently caused a beta-cutoff
     * in a sibling node. They are tried just after the TT move, before other quiet
     * moves, because they are likely to be good in the current node too.
     * Index: [ply][slot 0 or 1].
     */
    private final short[][] killerMoves = new short[64][2];

    public Search() {
        this.evaluator = new BetterEvaluator();
        this.moveOrderer = new BetterMoveOrderer();
    }

    public SearchResult bestMove(BBoard board, SearchSetting setting) {
        this.startTime = System.currentTimeMillis();
        this.setting = setting;
        this.stop = false;

        // Opening book: return immediately if a book move is available
        long zobristKey = board.getState().getZobristKey();
        var bookMove = openingBook.getMove(zobristKey);
        if (bookMove.isPresent()) {
            SearchResult bookResult = new SearchResult();
            bookResult.setBestMove(bookMove.get());
            bookResult.setScore(0);
            bookResult.setDepth(0);
            bookResult.setNodesSearched(0);
            bookResult.setTimeTakenMillis(0);
            System.out.println("info depth 0 score cp 0 time 0 pv " + bookMove.get());
            return bookResult;
        }

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
        moveOrderer.orderMoves(moves, board);

        // At the root, try the TT move first – it is the best move from the
        // previous iterative-deepening iteration and almost always the best.
        long rootKey = board.getState().getZobristKey();
        int  rootIdx = tt.index(rootKey);
        if (tt.matches(rootIdx, rootKey)) {
            short ttMove = tt.getMove(rootIdx);
            if (ttMove != 0) promoteTtMove(moves, ttMove);
        }

        for (BMove move : moves) {
            checkStop();

            board.makeMove(move, true);
            int score = -negamax(board, depth - 1, -beta, -alpha, 1, true);
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

    private int negamax(BBoard board, int depth, int alpha, int beta, int ply, boolean allowNullMove) {
        nodes++;

        if (isNthNode(1023)) checkStop();

        // Threefold-repetition: return a small negative score (contempt) instead
        // of 0 so the engine prefers fighting for a win over accepting a draw.
        if (board.isDrawByRepetition()) return -CONTEMPT;

        if (depth <= 0) return quiescence(board, alpha, beta);

        // ── Transposition-table probe ────────────────────────────────────────
        long key      = board.getState().getZobristKey();
        int  ttIndex  = tt.index(key);
        int  alphaOrig = alpha;
        short ttMove  = 0;

        if (tt.matches(ttIndex, key)) {
            ttMove = tt.getMove(ttIndex);
            if (tt.getDepth(ttIndex) >= depth) {
                int  ttScore = tt.getScore(ttIndex);
                byte ttFlag  = tt.getFlag(ttIndex);
                if (ttFlag == TranspositionTable.EXACT)       return ttScore;
                if (ttFlag == TranspositionTable.LOWER_BOUND) alpha = Math.max(alpha, ttScore);
                if (ttFlag == TranspositionTable.UPPER_BOUND) beta  = Math.min(beta,  ttScore);
                if (alpha >= beta) return ttScore;
            }
        }

        // ── Null move pruning ─────────────────────────────────────────────────
        // Skip a turn and search at reduced depth. If the score is still >= beta
        // even without making a move, the position is so good that we can prune.
        // Guards: not in check, not two null moves in a row, need non-pawn material
        // (to avoid zugzwang in king+pawn endings), and minimum depth.
        if (allowNullMove
                && depth >= 3
                && !board.isInCheck()
                && hasNonPawnMaterial(board)) {
            int staticEval = evaluator.evaluate(board);
            if (staticEval >= beta) {
                final int R = 2; // null-move reduction
                board.makeNullMove();
                int nullScore = -negamax(board, depth - 1 - R, -beta, -beta + 1, ply + 1, false);
                board.undoNullMove();
                if (nullScore >= beta) return beta; // cutoff: position is too good
            }
        }

        // ── Move generation & ordering ───────────────────────────────────────
        BMove[] nextMoves = new MoveGenerator(board).generateMoves(false);
        moveOrderer.orderMoves(nextMoves, board);
        if (ttMove != 0) promoteTtMove(nextMoves, ttMove);    // 1. TT move first
        promoteKillerMoves(nextMoves, ply);                    // 2. Killers next

        if (nextMoves.length == 0) {
            return board.isInCheck() ? -MATE_SCORE + ply : 0;
        }

        // ── Search ──────────────────────────────────────────────────────────
        int   bestScore     = -INF;
        short bestMoveValue = 0;
        int[] pieceBoards   = board.getPieceBoards();

        for (BMove move : nextMoves) {
            boolean isQuiet = pieceBoards[move.targetSquare()] == BPiece.none
                           && move.moveFlag() != BMove.enPassantCaptureFlag
                           && !move.isPromotion();

            board.makeMove(move, true);
            int score = -negamax(board, depth - 1, -beta, -alpha, ply + 1, true);
            board.undoMove(move, true);

            if (score > bestScore) {
                bestScore     = score;
                bestMoveValue = move.value();
            }
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                if (isQuiet) storeKiller(move.value(), ply);
                break;
            }
        }

        // ── Transposition-table store ────────────────────────────────────────
        byte flag;
        if      (bestScore <= alphaOrig) flag = TranspositionTable.UPPER_BOUND;
        else if (bestScore >= beta)      flag = TranspositionTable.LOWER_BOUND;
        else                             flag = TranspositionTable.EXACT;
        tt.store(ttIndex, key, bestScore, depth, flag, bestMoveValue);

        return bestScore;
    }

    /**
     * Quiescence search: at depth 0 we don't stop immediately but keep searching
     * captures (and check evasions) until the position is "quiet". This prevents
     * the horizon effect where the engine misses a hanging piece just past its
     * search horizon.
     *
     * Stand-pat: the side to move can always choose to not capture anything and
     * accept the static evaluation as a lower bound.
     */
    private int quiescence(BBoard board, int alpha, int beta) {
        nodes++;

        if (isNthNode(1023)) checkStop();

        boolean inCheck = board.isInCheck();

        if (!inCheck) {
            // Stand-pat score: we can always refuse to capture
            int standPat = evaluator.evaluate(board);
            if (standPat >= beta) return standPat;
            if (standPat > alpha) alpha = standPat;
        }

        // In check: generate all moves to find evasions.
        // Not in check: generate only captures (and promotions – always included).
        BMove[] moves = new MoveGenerator(board).generateMoves(!inCheck);
        moveOrderer.orderMoves(moves, board);

        if (moves.length == 0) {
            // Checkmate in quiescence (approximate – no ply distance used here)
            if (inCheck) return -MATE_SCORE;
            return alpha; // no captures available, return stand-pat
        }

        for (BMove move : moves) {
            board.makeMove(move, true);
            int score = -quiescence(board, -beta, -alpha);
            board.undoMove(move, true);

            if (score >= beta) return score;
            if (score > alpha) alpha = score;
        }

        return alpha;
    }

    /**
     * Moves the TT move to position 0 so it is tried first.
     */
    private static void promoteTtMove(BMove[] moves, short ttMoveValue) {
        for (int i = 1; i < moves.length; i++) {
            if (moves[i].value() == ttMoveValue) {
                BMove tmp = moves[0]; moves[0] = moves[i]; moves[i] = tmp;
                return;
            }
        }
    }

    /**
     * Promotes up to 2 killer moves to positions 1 and 2 (just after the TT move).
     * Killers are quiet moves that caused a beta-cutoff at this ply in a sibling
     * node, so they are worth trying before random quiet moves.
     */
    private void promoteKillerMoves(BMove[] moves, int ply) {
        int insertPos = 1; // slot 0 is the TT move
        for (int k = 0; k < 2; k++) {
            short killer = killerMoves[ply][k];
            if (killer == 0 || insertPos >= moves.length) continue;
            for (int i = insertPos; i < moves.length; i++) {
                if (moves[i].value() == killer) {
                    BMove tmp        = moves[insertPos];
                    moves[insertPos] = moves[i];
                    moves[i]         = tmp;
                    insertPos++;
                    break;
                }
            }
        }
    }

    /**
     * Returns true when the side to move has at least one non-pawn, non-king piece.
     * Used to avoid null move pruning in king+pawn endings where zugzwang is common.
     */
    private boolean hasNonPawnMaterial(BBoard board) {
        int color = board.moveColor();
        return board.getBitboard(BPiece.makePiece(BPiece.knight, color)) != 0
            || board.getBitboard(BPiece.makePiece(BPiece.bishop, color)) != 0
            || board.getBitboard(BPiece.makePiece(BPiece.rook,   color)) != 0
            || board.getBitboard(BPiece.makePiece(BPiece.queen,  color)) != 0;
    }

    private void storeKiller(short moveValue, int ply) {
        if (moveValue != killerMoves[ply][0]) {
            killerMoves[ply][1] = killerMoves[ply][0];
            killerMoves[ply][0] = moveValue;
        }
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
