package br.com.mvc.controller;

import br.com.mvc.model.Filme;
import br.com.mvc.service.FilmeService;
import br.com.mvc.api.ApiSupport;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class FilmeController {

    private final FilmeService filmeService = new FilmeService();
    private final Gson gson = new Gson();

    // GET /api/filmes
    public void listar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String pesquisa = request.getParameter("q");

        List<Filme> filmes = filmeService.pesquisar(pesquisa);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");

        response.getWriter().write(gson.toJson(filmes));
    }

    // GET /api/filmes/{id}
    public void buscarPorId(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        Filme filme = filmeService.buscarPorId(id);

        response.setContentType("application/json;charset=UTF-8");

        if (filme == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

            response.getWriter().write(
                    gson.toJson(Map.of(
                            "erro", "Filme não encontrado.")));
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(filme));
    }

    // POST /api/filmes
    public void cadastrar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ApiSupport.exigirAdmin(request);

        JsonObject body = lerBody(request);
        Filme filme = gson.fromJson(body, Filme.class);

        filmeService.salvar(filme, categoriasIds(body));

        ApiSupport.json(response, 201,
                java.util.Map.of("mensagem", "Filme cadastrado."));
    }

    // PUT /api/filmes/{id}
    public void atualizar(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        ApiSupport.exigirAdmin(request);

        JsonObject body = lerBody(request);
        Filme filme = gson.fromJson(body, Filme.class);

        filme.setId(id);
        filmeService.salvar(filme, categoriasIds(body));

        ApiSupport.json(response, 200,
                java.util.Map.of("mensagem", "Filme atualizado."));
    }

    // DELETE /api/filmes/{id}
    public void excluir(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        ApiSupport.exigirAdmin(request);

        filmeService.deletar(id);

        ApiSupport.json(response, 200,
                java.util.Map.of("mensagem", "Filme excluído."));
    }

    // Lê o JSON enviado pelo cliente
    private JsonObject lerBody(HttpServletRequest request)
            throws IOException {

        JsonElement elemento = JsonParser.parseReader(request.getReader());

        if (elemento == null || !elemento.isJsonObject()) {
            throw new IllegalArgumentException("Corpo JSON inválido.");
        }

        return elemento.getAsJsonObject();
    }

    // Converte categoriasIds do JSON para uma lista de IDs
    private List<Long> categoriasIds(JsonObject body) {

        if (!body.has("categoriasIds")
                || !body.get("categoriasIds").isJsonArray()) {
            return List.of();
        }

        List<Long> ids = new ArrayList<>();

        for (JsonElement elemento : body.getAsJsonArray("categoriasIds")) {
            ids.add(elemento.getAsLong());
        }

        return ids;
    }
}