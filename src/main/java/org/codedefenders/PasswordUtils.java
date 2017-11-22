package org.codedefenders;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by thomas on 20/01/2017.
 */
public class PasswordUtils {

    public static final long padding = 172636374748392l;

    public static String getReference() {
        SecureRandom r = new SecureRandom();

        long identifier = ((long) (r.nextLong() * (1f - (padding / (float) Long
                .MAX_VALUE))) + padding);

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String text = identifier + "";

        try {
            md.update(text.getBytes("UTF-8")); // Change this to "UTF-16" if
            // needed
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] digest = md.digest();

        String reference = String.format("%064x", new java.math
                .BigInteger(1, digest));

        return reference;
    }

}
