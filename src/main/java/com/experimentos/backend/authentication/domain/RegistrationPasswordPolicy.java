package com.experimentos.backend.authentication.domain;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Enforces registration requirements before accessing persistence or hashing credentials. */
public final class RegistrationPasswordPolicy {
    private static final Pattern UPPERCASE = Pattern.compile("\\p{Lu}");
    private static final Pattern LOWERCASE = Pattern.compile("\\p{Ll}");
    private static final Pattern DIGIT = Pattern.compile("\\p{Nd}");
    private static final Pattern SPECIAL = Pattern.compile("[^\\p{L}\\p{N}\\p{Z}\\s\\p{C}]");

    private RegistrationPasswordPolicy() {}

    public static void validate(String password) {
        if (password == null
                || password.codePointCount(0, password.length()) < 8
                || !UPPERCASE.matcher(password).find()
                || !LOWERCASE.matcher(password).find()
                || !DIGIT.matcher(password).find()
                || !SPECIAL.matcher(password).find()) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters, an uppercase letter, a lowercase letter, a number and a special character");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password exceeds the maximum allowed length");
        }
    }
}
