package sopra.steria.search;

/**
 * Fixed-size transposition table (hash table) for previously evaluated positions.
 *
 * When the same position is reached via different move orders (a "transposition"),
 * we can look up the cached result instead of re-searching the subtree.
 * This effectively gives the engine 1-2 extra plies of search depth for free.
 *
 * Uses parallel primitive arrays to avoid Java object overhead and GC pressure.
 * Replacement strategy: always overwrite (simple, works well for iterative deepening).
 *
 * Flag meanings (from the perspective of the node that stored the entry):
 *   EXACT       – the stored score is the true minimax value
 *   LOWER_BOUND – we had a beta cutoff; real score >= stored score
 *   UPPER_BOUND – we failed low; real score <= stored score
 */
public class TranspositionTable {

    public static final byte EXACT       = 0;
    public static final byte LOWER_BOUND = 1; // fail-high / cut-node
    public static final byte UPPER_BOUND = 2; // fail-low  / all-node

    /** 1 048 576 entries ≈ 16 MB total (8+4+1+1+2 bytes per slot). */
    private static final int SIZE = 1 << 20;
    private static final int MASK = SIZE - 1;

    private final long[]  keys;   // Zobrist hash of the position
    private final int[]   scores; // Stored score
    private final byte[]  depths; // Search depth at which this was computed
    private final byte[]  flags;  // EXACT / LOWER_BOUND / UPPER_BOUND
    private final short[] moves;  // Best move found (BMove.value()), 0 = none

    public TranspositionTable() {
        keys   = new long [SIZE];
        scores = new int  [SIZE];
        depths = new byte [SIZE];
        flags  = new byte [SIZE];
        moves  = new short[SIZE];
    }

    /**
     * Maps a 64-bit Zobrist key to a table index.
     * XOR-folding mixes the upper and lower 32 bits for better distribution.
     */
    public int index(long key) {
        return (int) ((key ^ (key >>> 32)) & MASK);
    }

    /** Returns true when the entry at {@code index} belongs to {@code key}. */
    public boolean matches(int index, long key) {
        return keys[index] == key;
    }

    /** Store a search result; always overwrites the existing entry. */
    public void store(int index, long key, int score, int depth, byte flag, short move) {
        keys  [index] = key;
        scores[index] = score;
        depths[index] = (byte) depth;
        flags [index] = flag;
        moves [index] = move;
    }

    public int   getScore(int i) { return scores[i]; }
    public int   getDepth(int i) { return Byte.toUnsignedInt(depths[i]); }
    public byte  getFlag (int i) { return flags[i]; }
    public short getMove (int i) { return moves[i]; }
}
