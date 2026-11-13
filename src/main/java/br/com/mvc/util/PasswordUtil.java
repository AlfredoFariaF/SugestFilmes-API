package br.com.mvc.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Hash de senha com PBKDF2-HMAC-SHA256; aceita senha legada somente para migração no login. */
public final class PasswordUtil {
    private static final int ITERATIONS = 210_000;
    private static final int BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private PasswordUtil() {}
    public static String hash(String password) {
        try {
            byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
            byte[] derived = derive(password.toCharArray(), salt, ITERATIONS);
            return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derived);
        } catch (Exception e) { throw new IllegalStateException("Não foi possível proteger a senha.", e); }
    }
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        if (!stored.startsWith("pbkdf2$")) return MessageDigest.isEqual(password.getBytes(java.nio.charset.StandardCharsets.UTF_8), stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try {
            String[] p = stored.split("\\$"); int rounds = Integer.parseInt(p[1]);
            byte[] expected = Base64.getDecoder().decode(p[3]);
            byte[] actual = derive(password.toCharArray(), Base64.getDecoder().decode(p[2]), rounds);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) { return false; }
    }
    private static byte[] derive(char[] password, byte[] salt, int rounds) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, rounds, BITS);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
}
