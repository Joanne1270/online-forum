package com.forum.common.util;

import java.util.Locale;

public final class ContentNormalizer {

    private ContentNormalizer() {
    }

    /** 检测敏感词时忽略空格、标点及各类符号，仅保留字母与数字（含中文）。 */
    public static String forSensitiveCheck(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
