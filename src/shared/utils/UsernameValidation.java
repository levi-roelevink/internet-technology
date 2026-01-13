package shared.utils;

public class UsernameValidation {
    public static boolean usernameIsValid(String username) {
        if (username == null || username.isBlank()) return false;

        // Length must be between 3 and 14 characters
        int length = username.length();
        if (length < 3 || length > 14) return false;

        // Username may only consist of characters, numbers, and underscores
        return username.matches("^[a-zA-Z0-9_]+$");
    }
}
