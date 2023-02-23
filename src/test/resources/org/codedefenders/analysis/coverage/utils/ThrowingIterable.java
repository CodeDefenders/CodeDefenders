package utils;

import java.util.Iterator;

import static utils.Utils.doThrow;

public class ThrowingIterable implements Iterable<Integer> {
    private final int index;

    public ThrowingIterable(int throwAfterWhichIteration) {
        index = throwAfterWhichIteration;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                if (i++ >= index) {
                    doThrow();
                }
                return 0;
            }
        };
    }
}

