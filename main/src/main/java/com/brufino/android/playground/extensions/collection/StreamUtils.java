package com.brufino.android.playground.extensions.collection;

import android.util.Pair;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class StreamUtils {
    public static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    public static <U, V> BiConsumer<U, V> throwingCombiner() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toOrderedMap() {
        return Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                throwingMerger(),
                LinkedHashMap::new);
    }

    public static <T> Stream<Pair<T, Long>> withIndex(List<T> list) {
        return IntStream.range(0, list.size())
                .mapToObj(i -> new Pair<>(list.get(i), (long) i));
    }

    public static <T> Stream<Pair<T, Long>> withReverseIndex(List<T> list) {
        int n = list.size();
        return IntStream.range(0, n)
                .mapToObj(i -> new Pair<>(list.get(i), (long) n - 1 - i));
    }

    private StreamUtils() {}
}
