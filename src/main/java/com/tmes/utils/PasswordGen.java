package com.tmes.utils;

import java.util.Random;

public class PasswordGen {
    public static String genPassword(int length)
    {
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
