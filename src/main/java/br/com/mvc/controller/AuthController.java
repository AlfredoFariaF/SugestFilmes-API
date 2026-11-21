package br.com.mvc.controller;

import br.com.mvc.api.ApiSupport;
import br.com.mvc.model.Usuario;
import br.com.mvc.service.UsuarioService;
import br.com.mvc.util.JwtUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthController {

    private final UsuarioService usuarioService =
            new UsuarioService();

    // POST /api/auth/login
    public void login(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // 1. Ler o JSON recebido
        var elemento = JsonParser.parseReader(request.getReader());

        if (elemento == null || !elemento.isJsonObject()) {
            throw new IllegalArgumentException("Corpo JSON inválido.");
        }

        JsonObject body = elemento.getAsJsonObject();

        // 2. Extrair as credenciais
        String login = texto(body, "login");
        String senha = texto(body, "senha");

        // 3. Autenticar utilizando o Service
        Usuario usuario = usuarioService.autenticar(login, senha);

        // 4. Identificar o perfil do usuário
        String perfil = usuario.getPerfil() == null
                ? ""
                : usuario.getPerfil().getNome();

        // 5. Gerar o JWT
        String token = JwtUtil.issue(
                usuario.getId(),
                usuario.getLogin(),
                perfil
        );

        // 6. Preparar os dados públicos do usuário
        Map<String, Object> dadosUsuario = new LinkedHashMap<>();

        dadosUsuario.put("id", usuario.getId());
        dadosUsuario.put("nome", usuario.getNome());
        dadosUsuario.put("login", usuario.getLogin());
        dadosUsuario.put("perfilId", usuario.getPerfilId());
        dadosUsuario.put("perfil", usuario.getPerfil());

        // 7. Preparar a resposta de autenticação
        Map<String, Object> resultado = Map.of(
                "token", token,
                "tipo", "Bearer",
                "expiraEm", 3600,
                "usuario", dadosUsuario
        );

        ApiSupport.json(response, 200, resultado);
    }

    // Obtém um campo textual do JSON
    private String texto(JsonObject body, String campo) {

        if (!body.has(campo) || body.get(campo).isJsonNull()) {
            return null;
        }

        return body.get(campo).getAsString();
    }
}