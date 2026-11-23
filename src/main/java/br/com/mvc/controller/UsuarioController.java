package br.com.mvc.controller;

import br.com.mvc.api.ApiSupport;
import br.com.mvc.model.Usuario;
import br.com.mvc.service.UsuarioService;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class UsuarioController {

    private final UsuarioService usuarioService =
            new UsuarioService();

    private final Gson gson = new Gson();

    // Converte Usuario em dados públicos, sem senha
    private Map<String, Object> usuarioPublico(Usuario usuario) {

        Map<String, Object> dados = new LinkedHashMap<>();

        dados.put("id", usuario.getId());
        dados.put("nome", usuario.getNome());
        dados.put("login", usuario.getLogin());
        dados.put("perfilId", usuario.getPerfilId());
        dados.put("perfil", usuario.getPerfil());

        return dados;
    }

    // Lê o corpo JSON da requisição
    private Usuario lerUsuario(HttpServletRequest request)
            throws IOException {

        var elemento = JsonParser.parseReader(request.getReader());

        if (elemento == null || !elemento.isJsonObject()) {
            throw new IllegalArgumentException("Corpo JSON inválido.");
        }

        return gson.fromJson(elemento, Usuario.class);
    }

    // GET /api/usuarios
    public void listar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ApiSupport.exigirAdmin(request);

        var usuarios = usuarioService.listar()
                .stream()
                .map(this::usuarioPublico)
                .toList();

        ApiSupport.json(response, 200, usuarios);
    }

    // GET /api/usuarios/{id}
    public void buscarPorId(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        ApiSupport.exigirAdmin(request);

        Usuario usuario = usuarioService.buscarPorId(id);

        if (usuario == null) {
            ApiSupport.erro(response, 404,
                    "Usuário não encontrado.");
            return;
        }

        ApiSupport.json(response, 200, usuarioPublico(usuario));
    }

    // POST /api/usuarios
    public void cadastrar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ApiSupport.exigirAdmin(request);

        Usuario usuario = lerUsuario(request);

        usuarioService.salvar(usuario);

        ApiSupport.json(response, 201,
                Map.of("mensagem", "Usuário cadastrado."));
    }

    // PUT /api/usuarios/{id}
    public void atualizar(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        ApiSupport.exigirAdmin(request);

        Usuario usuario = lerUsuario(request);

        usuario.setId(id);
        usuarioService.salvar(usuario);

        ApiSupport.json(response, 200,
                Map.of("mensagem", "Usuário atualizado."));
    }

    // DELETE /api/usuarios/{id}
    public void excluir(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        ApiSupport.exigirAdmin(request);

        usuarioService.deletar(id);

        ApiSupport.json(response, 200,
                Map.of("mensagem", "Usuário excluído."));
    }
}