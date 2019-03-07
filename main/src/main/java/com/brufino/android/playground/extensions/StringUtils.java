package com.brufino.android.playground.extensions;

public class StringUtils {
    public static final String INDENTATION = "    ";

    public static String repeat(String string, int times) {
        return new String(new char[times]).replace("\0", string);
    }

    public static String indent(int times) {
        return repeat(INDENTATION, times);
    }

    private StringUtils() {}
}
