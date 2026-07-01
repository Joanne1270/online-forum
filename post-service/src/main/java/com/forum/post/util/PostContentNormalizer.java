package com.forum.post.util;

import java.util.regex.Pattern;

public final class PostContentNormalizer {

    /** Completed hashtag: `#name ` or `#name` at end of content (after trim ate the confirming space). */
    public static final Pattern HASHTAG_PATTERN =
            Pattern.compile("#([\\p{L}\\p{N}_\\u4e00-\\u9fff]{1,50})(?: |\\z)");

    private static final Pattern TERMINAL_HASHTAG =
            Pattern.compile("#([\\p{L}\\p{N}_\\u4e00-\\u9fff]{1,50})$");

    private PostContentNormalizer() {
    }

    public static String trimPostContent(String content) {
        if (content == null) {
            return "";
        }
        boolean hadTrailingSpace = content.matches(".*\\s");
        String trimmed = content.trim();
        if (hadTrailingSpace && TERMINAL_HASHTAG.matcher(trimmed).find()) {
            return trimmed + " ";
        }
        return trimmed;
    }
}
