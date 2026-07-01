package com.forum.post.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostContentNormalizerTest {

    @Test
    void trimPostContentKeepsTrailingSpaceForTerminalHashtag() {
        assertEquals("#话题 ", PostContentNormalizer.trimPostContent("  #话题  "));
    }

    @Test
    void trimPostContentTrimsWithoutExtraSpace() {
        assertEquals("hello", PostContentNormalizer.trimPostContent("  hello  "));
    }

    @Test
    void nullContentBecomesEmptyString() {
        assertEquals("", PostContentNormalizer.trimPostContent(null));
    }
}
