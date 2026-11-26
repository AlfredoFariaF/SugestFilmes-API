package br.com.mvc.controller;

import br.com.mvc.api.ApiSupport;
import br.com.mvc.model.Perfil;
import br.com.mvc.service.PerfilService;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class PerfilController {

    private final PerfilService perfilService = new PerfilService();

    private final Gson gson = new Gson();

    // Converte o JSON recebido em um objeto Perfil
    private Perfil lerPerfil(HttpServletRequest request)
            throws IOException {

        var elemento = JsonParser.parseReader(request.getReader());

        if (elemento == null || !elemento.isJsonObject()) {
            throw new IllegalArgumentException("Corpo JSON inválido.");
        }

        return gson.fromJson(elemento, Perfil.class);
    }

    // GET /api/perfis
    public void listar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ApiSupport.json(response, 200, perfilService.listar());
    }

    // POST /api/perfis
    public void cadastrar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ApiSupport.exigirAdmin(request);

        Perfil perfil = lerPerfil(request);
        perfilService.salvar(perfil);

        ApiSupport.json(response, 201,
                Map.of("mensagem", "Perfil cadastrado."));
    }

    // PUT /api/perfis/{id}
    public void atualizar(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        ApiSupport.exigirAdmin(request);

        Perfil perfil = lerPerfil(request);
        perfil.setId(id);

        perfilService.salvar(perfil);

        ApiSupport.json(response, 200,
                Map.of("mensagem", "Perfil atualizado."));
    }

    // DELETE /api/perfis/{id}
    public void excluir(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        ApiSupport.exigirAdmin(request);

        perfilService.deletar(id);

        ApiSupport.json(response, 200,
                Map.of("mensagem", "Perfil excluído."));
    }
}