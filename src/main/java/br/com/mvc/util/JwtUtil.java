package br.com.mvc.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/** JWT HS256 implementado com APIs nativas do Java. */
public final class JwtUtil {
    private static final Gson GSON = new Gson();
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private JwtUtil() {
    }

    private static byte[] secret() {
        String value = System.getenv("JWT_SECRET");
        if (value == null || value.length() < 32)
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 caracteres.");
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret(), "HmacSHA256"));
            return ENC.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar JWT.", e);
        }
    }

    public static String issue(long userId, String login, String role) {
        long now = Instant.now().getEpochSecond();
        String header = ENC.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = ENC.encodeToString(
                GSON.toJson(Map.of("sub", userId, "login", login, "role", role, "iat", now, "exp", now + 3600))
                        .getBytes(StandardCharsets.UTF_8));
        String content = header + "." + payload;
        return content + "." + sign(content);
    }

    public static JsonObject verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !java.security.MessageDigest.isEqual(
                    sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8)))
                return null;
            JsonObject payload = GSON.fromJson(new String(DEC.decode(parts[1]), StandardCharsets.UTF_8),
                    JsonObject.class);
            if (payload.get("exp").getAsLong() < Instant.now().getEpochSecond())
                return null;
            return payload;
        } catch (Exception e) {
            return null;
        }
    }
}
