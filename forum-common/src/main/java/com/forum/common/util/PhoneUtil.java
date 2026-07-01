package com.forum.common.util;

import java.util.regex.Pattern;

public final class PhoneUtil {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private PhoneUtil() {
    }

    public static boolean isValid(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static String normalize(String phone) {
        return phone == null ? null : phone.trim();
    }
}
