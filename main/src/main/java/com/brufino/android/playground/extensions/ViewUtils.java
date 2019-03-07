package com.brufino.android.playground.extensions;

public class ViewUtils {
    private static final char PADDING_CHARACTER = ' ';

    public static String sizeString(int bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        bytes /= 1024;
        if (bytes < 1024) {
            return bytes + " KB";
        }
        bytes /= 1024;
        if (bytes < 1024) {
            return bytes + " MB";
        }
        bytes /= 1024;
        return bytes + " GB";
    }

    public static String sizeString(int bytes, int width) {
        String content = sizeString(bytes);
        int paddingWidth = Math.max(0, width - content.length());
        String padding = new String(new char[paddingWidth]).replace('\0', PADDING_CHARACTER);
        return padding + content;
    }

    private ViewUtils() {}
}
