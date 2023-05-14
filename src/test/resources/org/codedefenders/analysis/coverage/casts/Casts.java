import utils.Call;
import utils.MethodChain;

import static utils.Utils.consumeInt;
import static utils.Utils.doCall;
import static utils.Utils.doGet;

/**
 * <p>Casts
 * <p>JaCoCo coverage: Only covers the casted expression if it's coverable.
 * <p>Extended coverage: Covers the cast as well.
 */
public class Casts {

    @Call
    public void regularCast() {
        Object o = 1;

        Integer i =
                (Integer)
                o;
    }

    @Call(params = "1")
    public void implicitCastFromIntToInteger(int j) {
        Integer i
                =
                j;
    }

    @Call(params = "1")
    public void explicitCastFromIntToInteger(int j) {
        Integer i
                =
                (Integer)
                j;
    }

    @Call(params = "1")
    public void implicitCastFromIntegerToInt(Integer j) {
        int i
                =
                j
                ;
    }

    @Call(params = "1")
    public void explicitCastFromIntegerToInt(Integer j) {
        int i
                =
                (int)
                j
                ;
    }

    @Call(params = "null", exception = NullPointerException.class)
    public void implicitCastFromNullIntegerToInt(Integer j) {
        int i
                =
                j
                ;
    }

    @Call(params = "null", exception = NullPointerException.class)
    public void explicitCastFromNullIntegerToInt(Integer j) {
        int i
                =
                (int)
                j
                ;
    }

    @Call(params = "null", exception = NullPointerException.class)
    public void implicitNullUnbox1(Integer i) {
        doCall();
        consumeInt(i);

        // block: ignore_end_status
    }

    @Call(params = "null", exception = NullPointerException.class)
    public void implicitNullUnbox2(Integer i) {
        doCall();
        int[] j = {
                1,
                i
        };

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void npeFromCoveredExpr1() {
        int i =
                (int)
                doGet((Integer) null);
    }

    @Call(exception = NullPointerException.class)
    public void npeFromCoveredExpr2() {
        int i =
                (int)
                MethodChain.create()
                    .get((Integer) null);
    }
}
