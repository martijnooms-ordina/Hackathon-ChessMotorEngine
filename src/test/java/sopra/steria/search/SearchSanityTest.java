/*
package sopra.steria.search;

import knight.clubbing.core.BBoard;
import knight.clubbing.core.BMove;
import knight.clubbing.movegen.MoveGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sopra.steria.TestTags;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SearchSanityTest {

    */
/**
     * Requirement of (baseline) engine: should always return valid move
     *//*

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    @Tag(TestTags.UNRELIABLE)
    void testSearchReturnsValidMove() {
        BBoard board = new BBoard();
        Search search = new Search();

        SearchResult result = search.bestMove(board, new SearchSetting(1, 100));

        assertNotNull(result, "Search should return a result");
        assertNotNull(result.getBestMove(), "Search should return a best move");
        assertFalse(result.getBestMove().isEmpty(), "Best move should not be empty");

        assertTrue(isLegal(board, result), "Best move should be a legal move");
    }

    private static boolean isLegal(BBoard board, SearchResult result) {
        BMove[] legalMoves = new MoveGenerator(board).generateMoves(false);
        boolean isLegal = false;
        for (BMove move : legalMoves) {
            if (move.getUci().equals(result.getBestMove())) {
                isLegal = true;
                break;
            }
        }
        return isLegal;
    }

    */
/**
     * Requirement of (baseline) engine: should find mate (in one) when available
     *//*

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    @Tag(TestTags.UNRELIABLE)
    void testSearchFindsMateInOne() {
        String fen = "6k1/5ppp/5r2/8/8/8/5PPP/3R2K1 w - - 0 1";
        BBoard board = new BBoard(fen);

        Search search = new Search();
        SearchResult result = search.bestMove(board, new SearchSetting(2, 100));

        assertNotNull(result.getBestMove(), "Should find a move");
        assertEquals("d1d8", result.getBestMove(), "Should find mate in one (Qh8#)");
    }

}*/
