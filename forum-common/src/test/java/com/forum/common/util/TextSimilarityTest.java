package com.forum.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextSimilarityTest {

    @Test
    void identicalTextHasSimilarityOne() {
        assertEquals(1.0, TextSimilarity.similarity("hello world", "hello world"));
    }

    @Test
    void normalizeStripsMarkdownAndWhitespace() {
        String normalized = TextSimilarity.normalize("  Hello [link](http://x.com)  World!  ");
        assertEquals("hellolinkworld!", normalized);
    }

    @Test
    void emptyTextHasZeroSimilarity() {
        assertEquals(0.0, TextSimilarity.similarity("", "hello"));
    }

    @Test
    void similarTextScoresHigh() {
        double score = TextSimilarity.similarity("社区规范请大家遵守", "社区规范请大家共同遵守");
        assertTrue(score > 0.7);
    }
}
