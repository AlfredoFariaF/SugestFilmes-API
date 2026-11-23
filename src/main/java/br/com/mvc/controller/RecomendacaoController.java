package br.com.mvc.controller;

import br.com.mvc.api.ApiSupport;
import br.com.mvc.model.Filme;
import br.com.mvc.service.RecomendacaoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecomendacaoController {

    private final RecomendacaoService recomendacaoService =
            new RecomendacaoService();

    // GET /api/recomendacoes?categorias=1,2
    public void listar(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // Recebe os IDs das categorias
        String parametro = request.getParameter("categorias");

        List<Long> categorias = new ArrayList<>();

        // Separa os IDs enviados por vírgula
        if (parametro != null && !parametro.isBlank()) {

            String[] partes = parametro.split(",");

            for (String parte : partes) {
                categorias.add(Long.valueOf(parte.trim()));
            }
        }

        // Exige ao menos uma categoria
        if (categorias.isEmpty()) {
            ApiSupport.erro(
                    response,
                    400,
                    "Informe categorias, por exemplo ?categorias=1,2."
            );
            return;
        }

        // Consulta as recomendações
        List<Filme> filmes =
                recomendacaoService.recomendar(categorias);

        // Retorna os filmes em JSON
        ApiSupport.json(response, 200, filmes);
    }
}