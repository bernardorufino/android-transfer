package com.brufino.android.playground.extensions.collection;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class IteratingUtils {
    public static <T> Iterable<T> reverse(List<T> list) {
        return () -> new ReverseIterator<>(list.listIterator(list.size()));
    }

    private IteratingUtils() {}

    private static class ReverseIterator<T> implements Iterator<T> {
        private final ListIterator<T> mIterator;

        private ReverseIterator(ListIterator<T> iterator) {
            mIterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return mIterator.hasPrevious();
        }

        @Override
        public T next() {
            return mIterator.previous();
        }

        @Override
        public void remove() {
            mIterator.remove();
        }
    }
}
