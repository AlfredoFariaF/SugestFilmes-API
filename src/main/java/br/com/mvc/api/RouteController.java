package br.com.mvc.api;

import br.com.mvc.controller.AuthController;
import br.com.mvc.controller.AvaliacaoController;
import br.com.mvc.controller.CategoriaController;
import br.com.mvc.controller.FilmeController;
import br.com.mvc.controller.PerfilController;
import br.com.mvc.controller.RecomendacaoController;
import br.com.mvc.controller.UsuarioController;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = "/api/*")
public class RouteController extends HttpServlet {

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
    private final CategoriaController categoriaController = new CategoriaController();
    private final AuthController authController = new AuthController();
    private final AvaliacaoController avaliacaoController = new AvaliacaoController();
    private final RecomendacaoController recomendacaoController = new RecomendacaoController();
    private final UsuarioController usuarioController = new UsuarioController();
    private final PerfilController perfilController = new PerfilController();

    private boolean rotaComId(String caminho, String recurso) {

        String prefixo = "/" + recurso + "/";

        if (!caminho.startsWith(prefixo)) {
            return false;
        }

        String id = caminho.substring(prefixo.length());

        return !id.isBlank() && !id.contains("/");
    }

    public void encaminhar(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String caminho = request.getPathInfo();
        String metodo = request.getMethod();

        try {
            if (caminho == null) {
                ApiSupport.erro(response, 404, "Rota não encontrada.");
                return;
            }

            // ROTA DE AUTENTICAÇÃO

            if ("/auth/login".equals(caminho)) {

                if ("POST".equals(metodo)) {
                    authController.login(request, response);
                } else {
                    ApiSupport.erro(
                            response,
                            404,
                            "Rota não encontrada.");
                }

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

            if (rotaComId(caminho, "filmes")) {

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

            // ROTA DE SAÚDE DA API

            if ("/health".equals(caminho) && "GET".equals(metodo)) {

                ApiSupport.json(
                        response,
                        200,
                        java.util.Map.of(
                                "status", "ok",
                                "service", "SugestFilmes API"));

                return;
            }

            // ROTAS DE CATEGORIAS

            if ("/categorias".equals(caminho)) {

                switch (metodo) {
                    case "GET" -> categoriaController.listar(request, response);
                    case "POST" -> categoriaController.cadastrar(request, response);
                    default -> ApiSupport.erro(response, 404,
                            "Rota não encontrada.");
                }
                return;
            }

            if (rotaComId(caminho, "categorias")) {

                String idTexto = caminho.substring("/categorias/".length());
                Long id = Long.valueOf(idTexto);

                switch (metodo) {
                    case "GET" -> categoriaController.buscarPorId(
                            request, response, id);

                    case "PUT" -> categoriaController.atualizar(
                            request, response, id);

                    case "DELETE" -> categoriaController.excluir(
                            request, response, id);

                    default -> ApiSupport.erro(response, 404,
                            "Rota não encontrada.");
                }
                return;
            }

            // ROTAS DE AVALIAÇÕES

            if ("/avaliacoes".equals(caminho)) {

                switch (metodo) {
                    case "GET" -> avaliacaoController.listar(request, response);
                    case "POST" -> avaliacaoController.cadastrar(request, response);
                    default -> ApiSupport.erro(response, 404,
                            "Rota não encontrada.");
                }

                return;
            }

            if (rotaComId(caminho, "avaliacoes")) {

                String idTexto = caminho.substring("/avaliacoes/".length());
                Long id = Long.valueOf(idTexto);

                switch (metodo) {
                    case "PUT" -> avaliacaoController.atualizar(
                            request, response, id);

                    case "DELETE" -> avaliacaoController.excluir(
                            request, response, id);

                    default -> ApiSupport.erro(response, 404,
                            "Rota não encontrada.");
                }

                return;
            }

            // ROTA DE RECOMENDAÇÕES

            if ("/recomendacoes".equals(caminho)) {

                if ("GET".equals(metodo)) {
                    recomendacaoController.listar(request, response);
                } else {
                    ApiSupport.erro(
                            response,
                            404,
                            "Rota não encontrada.");
                }

                return;
            }

            // ROTAS DE USUÁRIOS

            if ("/usuarios".equals(caminho)) {

                switch (metodo) {
                    case "GET" -> usuarioController.listar(request, response);
                    case "POST" -> usuarioController.cadastrar(request, response);
                    default -> ApiSupport.erro(response, 404,
                            "Rota não encontrada.");
                }

                return;
            }

            if (rotaComId(caminho, "usuarios")) {

                // Preserva a exigência de administrador do controller original
                ApiSupport.exigirAdmin(request);

                String idTexto = caminho.substring("/usuarios/".length());
                Long id = Long.valueOf(idTexto);

                switch (metodo) {
                    case "GET" -> usuarioController.buscarPorId(
                            request, response, id);

                    case "PUT" -> usuarioController.atualizar(
                            request, response, id);

                    case "DELETE" -> usuarioController.excluir(
                            request, response, id);

                    default -> ApiSupport.erro(response, 404,
                            "Rota não encontrada.");
                }

                return;
            }

            // ROTAS DE PERFIS

            if ("/perfis".equals(caminho)) {

                switch (metodo) {
                    case "GET" -> perfilController.listar(request, response);
                    case "POST" -> perfilController.cadastrar(request, response);
                    default -> ApiSupport.erro(
                            response, 404, "Rota não encontrada.");
                }

                return;
            }

            if (rotaComId(caminho, "perfis")) {

                // Preserva a exigência de administrador do código original
                ApiSupport.exigirAdmin(request);

                String idTexto = caminho.substring("/perfis/".length());
                Long id = Long.valueOf(idTexto);

                switch (metodo) {
                    case "PUT" -> perfilController.atualizar(
                            request, response, id);

                    case "DELETE" -> perfilController.excluir(
                            request, response, id);

                    default -> ApiSupport.erro(
                            response, 404, "Rota não encontrada.");
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

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        encaminhar(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        encaminhar(request, response);
    }

    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        encaminhar(request, response);
    }

    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        encaminhar(request, response);
    }
}