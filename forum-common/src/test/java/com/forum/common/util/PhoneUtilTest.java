package com.forum.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneUtilTest {

    @Test
    void acceptsValidMobileNumber() {
        assertTrue(PhoneUtil.isValid("13800000001"));
    }

    @Test
    void rejectsInvalidMobileNumber() {
        assertFalse(PhoneUtil.isValid("12345"));
        assertFalse(PhoneUtil.isValid(null));
    }

    @Test
    void normalizeTrimsInput() {
        assertEqualsSafe("13800000001", PhoneUtil.normalize(" 13800000001 "));
    }

    private void assertEqualsSafe(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
