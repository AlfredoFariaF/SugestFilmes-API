package br.com.mvc.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class ApiSupport {

    private static final Gson gson =
            new GsonBuilder().serializeNulls().create();

    public static void json(
            HttpServletResponse response,
            int status,
            Object dados) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(gson.toJson(dados));
    }

    public static void erro(
            HttpServletResponse response,
            int status,
            String mensagem) throws IOException {

        json(response, status, Map.of("erro", mensagem));
    }

    public static boolean admin(HttpServletRequest request) {

        Object atributo = request.getAttribute("jwt.claims");

        if (!(atributo instanceof JsonObject claims)) {
            return false;
        }

        if (!claims.has("role") || claims.get("role").isJsonNull()) {
            return false;
        }

        return "Administrador".equalsIgnoreCase(
                claims.get("role").getAsString()
        );
    }

    public static void exigirAdmin(HttpServletRequest request) {

        if (!admin(request)) {
            throw new SecurityException(
                "Operação permitida somente para administradores."
            );
        }
    }
}