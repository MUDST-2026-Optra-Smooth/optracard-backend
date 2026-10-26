package org.example.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Matching rules shared by public catalog searches. Lower scores are more relevant. */
final class CatalogSearchMatcher {
    static final int NO_MATCH = Integer.MAX_VALUE;

    private CatalogSearchMatcher() {}

    static String normalize(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        // Remove marks only from Latin characters, preserving Thai vowels and tone marks.
        String withoutLatinAccents = decomposed.replaceAll("(?<=\\p{IsLatin})\\p{M}+", "");
        return Normalizer.normalize(withoutLatinAccents, Normalizer.Form.NFC)
                .replaceAll("\\s+", " ").trim();
    }

    static String normalizeQuery(String text) {
        return normalize(text).replace("โปเกมอน", "pokemon").replace("วันพีซ", "one piece");
    }

    static int score(String query, String name, String game, String type, String description, String store) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isEmpty()) return 0;
        List<String> fields = Arrays.asList(name, game, type, description, store);
        if (fields.stream().map(CatalogSearchMatcher::normalize).anyMatch(field -> field.contains(normalizedQuery))) {
            return 0;
        }

        List<String> queryWords = words(normalizedQuery);
        List<String> searchableWords = words(normalize(name) + " " + normalize(game));
        if (queryWords.isEmpty() || searchableWords.isEmpty()) return NO_MATCH;
        int score = 0;
        for (String queryWord : queryWords) {
            int allowed = queryWord.length() < 4 ? 0 : queryWord.length() < 8 ? 1 : 2;
            int best = NO_MATCH;
            for (String candidate : searchableWords) {
                int distance = candidate.contains(queryWord) ? 0 : distance(queryWord, candidate, allowed);
                best = Math.min(best, distance);
            }
            if (best > allowed) return NO_MATCH;
            score += best;
        }
        return score;
    }

    private static List<String> words(String text) {
        return Arrays.stream(text.split("[^\\p{L}\\p{M}\\p{N}]+"))
                .filter(word -> !word.isEmpty()).toList();
    }

    private static int distance(String left, String right, int limit) {
        if (Math.abs(left.length() - right.length()) > limit) return NO_MATCH;
        int[] previous = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) previous[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int[] current = new int[right.length() + 1];
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1));
            }
            previous = current;
        }
        return previous[right.length()] <= limit ? previous[right.length()] : NO_MATCH;
    }
}
