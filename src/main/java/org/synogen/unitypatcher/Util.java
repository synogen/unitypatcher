package org.synogen.unitypatcher;

import java.util.Arrays;
import java.util.List;

public class Util {

    private static final List<Integer> TEXT_TYPES = Arrays.asList(4,5);

    public static boolean isTextType(UnityIndex index) {
        for (Integer textType : TEXT_TYPES) {
            if (textType.compareTo(index.getType()) == 0) {
                return true;
            }
        }
        return false;
    }
}
