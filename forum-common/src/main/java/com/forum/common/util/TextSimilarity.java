package com.forum.common.util;

public final class TextSimilarity {

    private static final int MAX_COMPARE_LENGTH = 2000;

    private TextSimilarity() {
    }

    public static double similarity(String a, String b) {
        String left = normalize(a);
        String right = normalize(b);
        if (left.equals(right)) {
            return 1.0;
        }
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        int maxLen = Math.max(left.length(), right.length());
        int minLen = Math.min(left.length(), right.length());
        if (minLen >= 10 && (left.contains(right) || right.contains(left))) {
            return (double) minLen / maxLen;
        }
        if (left.length() > MAX_COMPARE_LENGTH) {
            left = left.substring(0, MAX_COMPARE_LENGTH);
        }
        if (right.length() > MAX_COMPARE_LENGTH) {
            right = right.substring(0, MAX_COMPARE_LENGTH);
        }
        int distance = levenshtein(left, right);
        return 1.0 - (double) distance / Math.max(left.length(), right.length());
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                .replaceAll("<video[^>]*>.*?</video>", "")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
