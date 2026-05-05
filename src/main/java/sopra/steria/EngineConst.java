package sopra.steria;

public class EngineConst {
    private EngineConst() {
        // Don't initialize
    }

    public static final int DEFAULT_DEPTH = 7;
    public static final int INF = 10_000_000;
    public static final int MATE_SCORE = 1_000_000;

    /**
     * Contempt factor: returned instead of 0 for draws by repetition.
     * A negative value makes the engine prefer fighting for a win
     * over accepting a draw. Tune down if the engine loses too many
     * games it could have drawn.
     */
    public static final int CONTEMPT = 10;
}
