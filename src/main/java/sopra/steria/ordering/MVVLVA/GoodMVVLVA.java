package sopra.steria.ordering.MVVLVA;
import knight.clubbing.core.BPiece;

public class GoodMVVLVA{

    public static int MVVLVAScore(int victim, int attacker) {
        victim = BPiece.getPieceType(victim);
        attacker = BPiece.getPieceType(attacker);

        return MVV_LVA[victim][attacker];
    }

    // MVV_LVA[victim][aggressor]
    private static final int[][] MVV_LVA = new int[][]{
            //         none  pawn  knight bishop rook  queen king
            /* none   */ { 0,    0,    0,    0,    0,    0,    0 },
            /* pawn   */ { 0,  900,  700,  700,  500,  100,  900 },
            /* knight */ { 0, 1100,  900,  900,  700,  300, 1100 },
            /* bishop */ { 0, 1100,  900,  900,  700,  300, 1100 },
            /* rook   */ { 0, 1300, 1100, 1100,  900,  500, 1300 },
            /* queen  */ { 0, 1700, 1500, 1500, 1300,  900, 1700 },
            /* king   */ { 0,  900,  700,  700,  500,  100,  900 },
    };

}
