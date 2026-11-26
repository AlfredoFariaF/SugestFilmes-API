package br.com.mvc.controller;

import br.com.mvc.api.ApiSupport;
import br.com.mvc.model.Avaliacao;
import br.com.mvc.service.AvaliacaoService;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService = new AvaliacaoService();

    private final Gson gson = new Gson();

    // Obtém o ID do usuário autenticado pelo JWT validado
    private Long usuarioId(HttpServletRequest request) {

        Object atributo = request.getAttribute("jwt.claims");

        if (!(atributo instanceof JsonObject claims)
                || !claims.has("sub")
                || claims.get("sub").isJsonNull()) {
            throw new SecurityException("Usuário não autenticado.");
        }

        return claims.get("sub").getAsLong();
    }

    // Lê o JSON recebido
    private JsonObject lerBody(HttpServletRequest request)
            throws IOException {

        var elemento = JsonParser.parseReader(request.getReader());

        if (elemento == null || !elemento.isJsonObject()) {
            throw new IllegalArgumentException("Corpo JSON inválido.");
        }

        return elemento.getAsJsonObject();
    }

    // GET /api/avaliacoes
    public void listar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String filmeId = request.getParameter("filmeId");
        String usuarioFiltro = request.getParameter("usuarioId");

        List<Avaliacao> avaliacoes;

        if (ApiSupport.admin(request) && usuarioFiltro != null) {
            avaliacoes = avaliacaoService.listarPorUsuario(
                    Long.valueOf(usuarioFiltro));

        } else if (filmeId != null) {
            avaliacoes = avaliacaoService.listarPorFilme(
                    Long.valueOf(filmeId));

        } else {
            avaliacoes = avaliacaoService.listarPorUsuario(
                    usuarioId(request));
        }

        ApiSupport.json(response, 200, avaliacoes);
    }

    // POST /api/avaliacoes
    public void cadastrar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        JsonObject body = lerBody(request);

        Avaliacao avaliacao = gson.fromJson(body, Avaliacao.class);

        // Ignora o usuário informado pelo cliente
        avaliacao.setUsuarioId(usuarioId(request));

        avaliacaoService.salvar(avaliacao);

        ApiSupport.json(response, 201,
                Map.of("mensagem", "Avaliação registrada."));
    }

    // PUT /api/avaliacoes/{id}
    public void atualizar(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        JsonObject body = lerBody(request);

        int nota = body.get("nota").getAsInt();

        String comentario = body.has("comentario")
                && !body.get("comentario").isJsonNull()
                        ? body.get("comentario").getAsString()
                        : null;

        avaliacaoService.atualizar(
                id,
                usuarioId(request),
                nota,
                comentario);

        ApiSupport.json(response, 200,
                Map.of("mensagem", "Avaliação atualizada."));
    }

    // DELETE /api/avaliacoes/{id}
    public void excluir(
            HttpServletRequest request,
            HttpServletResponse response,
            Long id) throws IOException {

        boolean excluiu;

        if (ApiSupport.admin(request)) {
            excluiu = avaliacaoService.deletarComoAdmin(id);
        } else {
            excluiu = avaliacaoService.deletarDoUsuario(
                    id, usuarioId(request));
        }

        if (!excluiu) {
            ApiSupport.erro(response, 404,
                    "Avaliação não encontrada ou sem permissão.");
            return;
        }

        ApiSupport.json(response, 200,
                Map.of("mensagem", "Avaliação excluída."));
    }
}