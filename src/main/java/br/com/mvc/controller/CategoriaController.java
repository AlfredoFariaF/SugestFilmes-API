package br.com.mvc.controller;

import br.com.mvc.api.ApiSupport;
import br.com.mvc.model.Categoria;
import br.com.mvc.service.CategoriaService;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class CategoriaController {

    private final CategoriaService categoriaService = new CategoriaService();

    private final Gson gson = new Gson();

    // GET /api/categorias
    public void listar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ApiSupport.json(response, 200, categoriaService.listar());
    }

    // GET /api/categorias/{id}
    public void buscarPorId(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        Categoria categoria = categoriaService.buscarPorId(id);

        if (categoria == null) {
            ApiSupport.erro(response, 404, "Categoria não encontrada.");
            return;
        }

        ApiSupport.json(response, 200, categoria);
    }

    // POST /api/categorias
    public void cadastrar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ApiSupport.exigirAdmin(request);

        Categoria categoria = lerCategoria(request);

        categoriaService.salvar(categoria);

        ApiSupport.json(response, 201,
                Map.of("mensagem", "Categoria cadastrada."));
    }

    // PUT /api/categorias/{id}
    public void atualizar(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        ApiSupport.exigirAdmin(request);

        Categoria categoria = lerCategoria(request);
        categoria.setId(id);

        categoriaService.salvar(categoria);

        ApiSupport.json(response, 200,
                Map.of("mensagem", "Categoria atualizada."));
    }

    // DELETE /api/categorias/{id}
    public void excluir(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        ApiSupport.exigirAdmin(request);

        categoriaService.deletar(id);

        ApiSupport.json(response, 200,
                Map.of("mensagem", "Categoria excluída."));
    }

    // Converte o JSON recebido em Categoria
    private Categoria lerCategoria(HttpServletRequest request)
            throws IOException {

        var elemento = JsonParser.parseReader(request.getReader());

        if (elemento == null || !elemento.isJsonObject()) {
            throw new IllegalArgumentException("Corpo JSON inválido.");
        }

        JsonObject body = elemento.getAsJsonObject();

        return gson.fromJson(body, Categoria.class);
    }
}
