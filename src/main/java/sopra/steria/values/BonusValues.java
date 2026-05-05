package sopra.steria.values;

public class BonusValues {

    // -------------------------------------------------------------------------
    // Bonuses / penalties
    // -------------------------------------------------------------------------
    public static final int BISHOP_PAIR_BONUS     =  30;
    public static final int DOUBLED_PAWN_PENALTY  = -20;  // per extra pawn on a file
    public static final int ISOLATED_PAWN_PENALTY = -15;  // no friendly pawns on adjacent files
    public static final int PASSED_PAWN_BONUS     =  20;  // multiplied by ranks advanced
}
