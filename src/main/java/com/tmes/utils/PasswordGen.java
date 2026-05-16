package com.tmes.utils;

import java.util.Random;

/**
 * A utility class responsible for generating random, cryptographically valid passwords.
 * It is used to provide user convenience in the GUI and to facilitate automated testing
 * of the topological graph generation.
 */
public class PasswordGen {

    /**
     * Generates a random password of a specified length containing only visible, typable ASCII characters.
     * * Complexity: O(L) where L is the requested password length.
     *
     * @param length The desired length of the generated password.
     * @return A randomly generated string to be used as a cryptographic seed.
     */
    public static String genPassword(int length) {
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            // ASCII 33 is '!', ASCII 126 is '~'. 127 is the exclusive upper bound.
            // This guarantees we only generate visible, typable characters.
            char generatedChar = (char) random.nextInt(33, 127);
            password.append(generatedChar);
        }
        return password.toString();
    }
}