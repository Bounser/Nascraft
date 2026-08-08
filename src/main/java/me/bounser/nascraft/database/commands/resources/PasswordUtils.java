package me.bounser.nascraft.database.commands.resources;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility methods for hashing and verifying passwords using BCrypt.
 */
public final class PasswordUtils {

    private static final int LOG_ROUNDS = 12;

    private PasswordUtils() {
    }

    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }

        return BCrypt.hashpw(password, BCrypt.gensalt(LOG_ROUNDS));
    }

    public static boolean checkPassword(String password, String hashedPassword) {
        if (password == null || password.isEmpty() || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }

        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
