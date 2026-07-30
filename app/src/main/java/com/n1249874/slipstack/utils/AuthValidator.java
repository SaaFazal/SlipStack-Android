package com.n1249874.slipstack.utils;

import android.util.Patterns;

public class AuthValidator {

    public static boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty()
                && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }

    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }
}
