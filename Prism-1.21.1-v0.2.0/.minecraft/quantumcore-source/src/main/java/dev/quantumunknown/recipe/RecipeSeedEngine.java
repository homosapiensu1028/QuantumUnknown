package dev.quantumunknown.recipe;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class RecipeSeedEngine {
    private RecipeSeedEngine() {}

    public static long deriveSeed(String recipeSeed, String generatorVersion, String domain) {
        String input = safe(recipeSeed) + "|" + safe(generatorVersion) + "|" + safe(domain);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            long result = 0L;
            for (int i = 0; i < 8; i++) result = (result << 8) | (digest[i] & 0xffL);
            return result;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by Java 21", ex);
        }
    }

    public static <T> void deterministicShuffle(List<T> values, String recipeSeed, String generatorVersion, String domain) {
        Collections.shuffle(values, new Random(deriveSeed(recipeSeed, generatorVersion, domain)));
    }

    public static String mappingHash(Map<String, String> mapping) {
        StringBuilder canonical = new StringBuilder();
        mapping.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .forEach(e -> canonical.append(e.getKey()).append("->").append(e.getValue()).append('\n'));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(digest.length * 2);
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static <T> Map<String, T> pairSortedKeysToValues(List<String> keys, List<T> values) {
        if (keys.size() != values.size()) throw new IllegalArgumentException("Key/value size mismatch");
        List<String> sortedKeys = new ArrayList<>(keys);
        sortedKeys.sort(String::compareTo);
        Map<String, T> result = new LinkedHashMap<>();
        for (int i = 0; i < sortedKeys.size(); i++) result.put(sortedKeys.get(i), values.get(i));
        return result;
    }

    private static String safe(String input) { return input == null ? "" : input; }
}
