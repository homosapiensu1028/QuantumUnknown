package dev.quantumunknown.recipe;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RecipeSeedEngineTest {
    @Test
    void seedDerivationIsStableAndScoped() {
        long first = RecipeSeedEngine.deriveSeed("alpha", "v1", "tools");
        assertEquals(first, RecipeSeedEngine.deriveSeed("alpha", "v1", "tools"));
        assertNotEquals(first, RecipeSeedEngine.deriveSeed("beta", "v1", "tools"));
        assertNotEquals(first, RecipeSeedEngine.deriveSeed("alpha", "v1", "armor"));
    }

    @Test
    void shuffleIsDeterministicAndPreservesAllEntries() {
        List<String> first = new ArrayList<>(List.of("a", "b", "c", "d", "e"));
        List<String> second = new ArrayList<>(first);
        RecipeSeedEngine.deterministicShuffle(first, "seed", "v1", "tools");
        RecipeSeedEngine.deterministicShuffle(second, "seed", "v1", "tools");
        assertEquals(first, second);
        assertEquals(List.of("a", "b", "c", "d", "e"), first.stream().sorted().toList());
    }

    @Test
    void hashIsIndependentOfInsertionOrder() {
        Map<String, String> a = Map.of("a", "one", "b", "two");
        Map<String, String> b = Map.of("b", "two", "a", "one");
        assertEquals(RecipeSeedEngine.mappingHash(a), RecipeSeedEngine.mappingHash(b));
        assertTrue(RecipeSeedEngine.mappingHash(a).matches("[0-9a-f]{64}"));
    }
}
