package com.forum.user.util;

import com.forum.common.constant.ForumConstants;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

public final class ProfileDisplayUtil {

    private ProfileDisplayUtil() {
    }

    public static String genderLabel(String gender) {
        if (ForumConstants.GENDER_MALE.equals(gender)) {
            return "男";
        }
        if (ForumConstants.GENDER_FEMALE.equals(gender)) {
            return "女";
        }
        return null;
    }

    public static int calcAge(LocalDate birthMonth) {
        if (birthMonth == null) {
            return -1;
        }
        LocalDate today = LocalDate.now();
        if (birthMonth.isAfter(today)) {
            return -1;
        }
        return Period.between(birthMonth, today).getYears();
    }

    public static String buildBrief(String gender, LocalDate birthMonth) {
        List<String> parts = new ArrayList<>();
        String genderText = genderLabel(gender);
        if (genderText != null) {
            parts.add(genderText);
        }
        int age = calcAge(birthMonth);
        if (age >= 0) {
            parts.add(age + "岁");
        }
        return String.join(" ", parts);
    }
}
