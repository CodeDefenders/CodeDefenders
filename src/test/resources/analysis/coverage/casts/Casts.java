import utils.Call;

import static utils.Utils.consume;
import static utils.Utils.doCall;

public class Casts {

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

        ;
    }

    @Call(params = "null", exception = NullPointerException.class)
    public void implicitNullUnbox2(Integer i) {
        doCall();
        int[] j = {
                1,
                i
        };

        ;
    }

    public void consumeInt(int i) {
        // TODO: put in utils
    }
}
