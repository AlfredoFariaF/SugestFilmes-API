package br.com.mvc.api;

import br.com.mvc.controller.FilmeController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public class RouteController {

    public String identificarRota(HttpServletRequest request) {

        String caminho = request.getPathInfo();

        if (caminho == null || caminho.isBlank()) {
            return "Rota inválida";
        }

        String[] partes = caminho.split("/");

        if (partes.length < 2) {
            return "Rota inválida";
        }

        return switch (partes[1]) {
            case "filmes" -> "FilmeController";
            case "categorias" -> "CategoriaController";
            case "avaliacoes" -> "AvaliacaoController";
            case "recomendacoes" -> "RecomendacaoController";
            case "usuarios" -> "UsuarioController";
            case "perfis" -> "PerfilController";
            case "auth" -> "AuthController";
            case "health" -> "HealthController";
            default -> "Rota não encontrada";
        };
    }

    private final FilmeController filmeController = new FilmeController();

    public void encaminhar(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String caminho = request.getPathInfo();
        String metodo = request.getMethod();

        try {
            if (caminho == null) {
                ApiSupport.erro(response, 404, "Rota não encontrada.");
                return;
            }

            if ("/filmes".equals(caminho)) {
                switch (metodo) {
                    case "GET" -> filmeController.listar(request, response);
                    case "POST" -> filmeController.cadastrar(request, response);
                    default -> ApiSupport.erro(response, 404, "Rota não encontrada.");
                }
                return;
            }

            if (caminho.startsWith("/filmes/")) {

                String idTexto = caminho.substring("/filmes/".length());

                Long id = Long.valueOf(idTexto);

                switch (metodo) {
                    case "GET" -> filmeController.buscarPorId(request, response, id);
                    case "PUT" -> filmeController.atualizar(request, response, id);
                    case "DELETE" -> filmeController.excluir(request, response, id);
                    default -> ApiSupport.erro(response, 404, "Rota não encontrada.");
                }
                return;
            }

            ApiSupport.erro(response, 404, "Rota não encontrada.");

        } catch (SecurityException e) {
            ApiSupport.erro(response, 403, e.getMessage());

        } catch (NumberFormatException e) {
            ApiSupport.erro(response, 400, "ID inválido.");

        } catch (com.google.gson.JsonParseException e) {
            ApiSupport.erro(response, 400, "JSON inválido ou malformado.");

        } catch (IllegalArgumentException e) {
            ApiSupport.erro(response, 400, e.getMessage());

        } catch (java.util.NoSuchElementException e) {
            ApiSupport.erro(response, 404, "Registro não encontrado.");

        } catch (Exception e) {
            request.getServletContext().log("Erro na API", e);
            ApiSupport.erro(response, 500, "Erro interno ao processar a requisição.");
        }
    }
}