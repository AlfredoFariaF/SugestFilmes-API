package br.com.mvc.api;

import br.com.mvc.model.*;
import br.com.mvc.service.*;
import br.com.mvc.util.JwtUtil;
import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

/** Controller REST: HTTP/JSON. Regras permanecem nos Services e SQL nos DAOs. */
@WebServlet(urlPatterns = "/api/*")
public class ApiController extends HttpServlet {
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final FilmeService filmes = new FilmeService();
    private final CategoriaService categorias = new CategoriaService();
    private final UsuarioService usuarios = new UsuarioService();
    private final PerfilService perfis = new PerfilService();
    private final AvaliacaoService avaliacoes = new AvaliacaoService();
    private final RecomendacaoService recomendacoes = new RecomendacaoService();

    public static void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status); resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(new Gson().toJson(Map.of("erro", message)));
    }
    private void json(HttpServletResponse resp, int status, Object value) throws IOException {
        resp.setStatus(status); resp.setContentType("application/json;charset=UTF-8"); resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(value));
    }
    private JsonObject body(HttpServletRequest req) throws IOException {
        JsonElement e = JsonParser.parseReader(req.getReader());
        if (e == null || !e.isJsonObject()) throw new IllegalArgumentException("Corpo JSON inválido.");
        return e.getAsJsonObject();
    }
    private JsonObject claims(HttpServletRequest req) { return (JsonObject) req.getAttribute("jwt.claims"); }
    private long userId(HttpServletRequest req) { return claims(req).get("sub").getAsLong(); }
    private String role(HttpServletRequest req) { return claims(req).get("role").getAsString(); }
    private boolean admin(HttpServletRequest req) { return "Administrador".equalsIgnoreCase(role(req)); }
    private void requireAdmin(HttpServletRequest req) { if (!admin(req)) throw new SecurityException("Operação permitida somente para administradores."); }
    private Long id(String path) { try { return Long.valueOf(path); } catch (Exception e) { return null; } }
    private List<Long> longList(JsonObject o, String key) {
        if (!o.has(key) || !o.get(key).isJsonArray()) return List.of();
        List<Long> result = new ArrayList<>(); for (JsonElement e : o.getAsJsonArray(key)) result.add(e.getAsLong()); return result;
    }
    private Map<String,Object> publicUser(Usuario u) {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("id",u.getId()); m.put("nome",u.getNome()); m.put("login",u.getLogin()); m.put("perfilId",u.getPerfilId()); m.put("perfil",u.getPerfil()); return m;
    }
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        dispatch(req, resp, "GET");
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException { dispatch(req, resp, "POST"); }
    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException { dispatch(req, resp, "PUT"); }
    @Override protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException { dispatch(req, resp, "DELETE"); }

    private void dispatch(HttpServletRequest req, HttpServletResponse resp, String method) throws IOException {
        try {
            String path = Optional.ofNullable(req.getPathInfo()).orElse("/");
            String[] p = Arrays.stream(path.split("/")).filter(s -> !s.isBlank()).toArray(String[]::new);
            if (p.length == 1 && p[0].equals("health") && method.equals("GET")) { json(resp,200,Map.of("status","ok","service","SugestFilmes API")); return; }
            if (p.length == 2 && p[0].equals("auth") && p[1].equals("login") && method.equals("POST")) {
                JsonObject b=body(req); Usuario u=usuarios.autenticar(str(b,"login"),str(b,"senha"));
                String perfil=u.getPerfil()==null?"":u.getPerfil().getNome();
                json(resp,200,Map.of("token",JwtUtil.issue(u.getId(),u.getLogin(),perfil),"tipo","Bearer","expiraEm",3600,"usuario",publicUser(u))); return;
            }
            if (p.length==1 && p[0].equals("filmes")) {
                if(method.equals("GET")) { String q=req.getParameter("q"); json(resp,200,filmes.pesquisar(q)); return; }
                if(method.equals("POST")) { requireAdmin(req); JsonObject b=body(req); Filme f=gson.fromJson(b,Filme.class); filmes.salvar(f,longList(b,"categoriasIds")); json(resp,201,Map.of("mensagem","Filme cadastrado.")); return; }
            }
            if (p.length==2 && p[0].equals("filmes")) {
                Long filmId=id(p[1]); if(filmId==null){writeError(resp,400,"ID inválido.");return;}
                if(method.equals("GET")){Filme f=filmes.buscarPorId(filmId); if(f==null){writeError(resp,404,"Filme não encontrado.");return;} json(resp,200,f);return;}
                if(method.equals("PUT")){requireAdmin(req);JsonObject b=body(req);Filme f=gson.fromJson(b,Filme.class);f.setId(filmId);filmes.salvar(f,longList(b,"categoriasIds"));json(resp,200,Map.of("mensagem","Filme atualizado."));return;}
                if(method.equals("DELETE")){requireAdmin(req);filmes.deletar(filmId);json(resp,200,Map.of("mensagem","Filme excluído."));return;}
            }
            if (p.length==1 && p[0].equals("categorias")) {
                if(method.equals("GET")){json(resp,200,categorias.listar());return;}
                if(method.equals("POST")){requireAdmin(req);Categoria c=gson.fromJson(body(req),Categoria.class);categorias.salvar(c);json(resp,201,Map.of("mensagem","Categoria cadastrada."));return;}
            }
            if (p.length==2 && p[0].equals("categorias")) {
                Long cid=id(p[1]); if(cid==null){writeError(resp,400,"ID inválido.");return;}
                if(method.equals("GET")){Categoria c=categorias.buscarPorId(cid);if(c==null){writeError(resp,404,"Categoria não encontrada.");return;}json(resp,200,c);return;}
                if(method.equals("PUT")){requireAdmin(req);Categoria c=gson.fromJson(body(req),Categoria.class);c.setId(cid);categorias.salvar(c);json(resp,200,Map.of("mensagem","Categoria atualizada."));return;}
                if(method.equals("DELETE")){requireAdmin(req);categorias.deletar(cid);json(resp,200,Map.of("mensagem","Categoria excluída."));return;}
            }
            if (p.length==1 && p[0].equals("recomendacoes") && method.equals("GET")) {
                List<Long> ids=new ArrayList<>();String raw=req.getParameter("categorias");if(raw!=null&&!raw.isBlank())for(String x:raw.split(","))ids.add(Long.valueOf(x.trim()));
                if(ids.isEmpty()){writeError(resp,400,"Informe categorias, por exemplo ?categorias=1,2.");return;} json(resp,200,recomendacoes.recomendar(ids));return;
            }
            if (p.length==1 && p[0].equals("avaliacoes")) {
                if(method.equals("GET")){String filme=req.getParameter("filmeId");String usuario=req.getParameter("usuarioId");if(admin(req)&&usuario!=null)json(resp,200,avaliacoes.listarPorUsuario(Long.valueOf(usuario)));else if(filme!=null)json(resp,200,avaliacoes.listarPorFilme(Long.valueOf(filme)));else json(resp,200,avaliacoes.listarPorUsuario(userId(req)));return;}
                if(method.equals("POST")){JsonObject b=body(req);Avaliacao a=gson.fromJson(b,Avaliacao.class);a.setUsuarioId(userId(req));avaliacoes.salvar(a);json(resp,201,Map.of("mensagem","Avaliação registrada."));return;}
            }
            if (p.length==2 && p[0].equals("avaliacoes")) {
                Long aid=id(p[1]);if(aid==null){writeError(resp,400,"ID inválido.");return;}
                if(method.equals("PUT")){JsonObject b=body(req);avaliacoes.atualizar(aid,userId(req),b.get("nota").getAsInt(),str(b,"comentario"));json(resp,200,Map.of("mensagem","Avaliação atualizada."));return;}
                if(method.equals("DELETE")){boolean ok=admin(req)?avaliacoes.deletarComoAdmin(aid):avaliacoes.deletarDoUsuario(aid,userId(req));if(!ok){writeError(resp,404,"Avaliação não encontrada ou sem permissão.");return;}json(resp,200,Map.of("mensagem","Avaliação excluída."));return;}
            }
            if (p.length==1 && p[0].equals("usuarios")) {
                if(method.equals("GET")){requireAdmin(req);json(resp,200,usuarios.listar().stream().map(this::publicUser).toList());return;}
                if(method.equals("POST")){requireAdmin(req);Usuario u=gson.fromJson(body(req),Usuario.class);usuarios.salvar(u);json(resp,201,Map.of("mensagem","Usuário cadastrado."));return;}
            }
            if (p.length==2 && p[0].equals("usuarios")) {
                requireAdmin(req);Long uid=id(p[1]);if(uid==null){writeError(resp,400,"ID inválido.");return;}
                if(method.equals("GET")){Usuario u=usuarios.buscarPorId(uid);if(u==null){writeError(resp,404,"Usuário não encontrado.");return;}json(resp,200,publicUser(u));return;}
                if(method.equals("PUT")){Usuario u=gson.fromJson(body(req),Usuario.class);u.setId(uid);usuarios.salvar(u);json(resp,200,Map.of("mensagem","Usuário atualizado."));return;}
                if(method.equals("DELETE")){usuarios.deletar(uid);json(resp,200,Map.of("mensagem","Usuário excluído."));return;}
            }
            if (p.length==1 && p[0].equals("perfis") && method.equals("GET")){json(resp,200,perfis.listar());return;}
            if (p.length==1 && p[0].equals("perfis") && method.equals("POST")){requireAdmin(req);Perfil x=gson.fromJson(body(req),Perfil.class);perfis.salvar(x);json(resp,201,Map.of("mensagem","Perfil cadastrado."));return;}
            if (p.length==2 && p[0].equals("perfis")){requireAdmin(req);Long pid=id(p[1]);if(pid==null){writeError(resp,400,"ID inválido.");return;}if(method.equals("PUT")){Perfil x=gson.fromJson(body(req),Perfil.class);x.setId(pid);perfis.salvar(x);json(resp,200,Map.of("mensagem","Perfil atualizado."));return;}if(method.equals("DELETE")){perfis.deletar(pid);json(resp,200,Map.of("mensagem","Perfil excluído."));return;}}
            writeError(resp,404,"Rota não encontrada.");
        } catch (SecurityException e) { writeError(resp,403,e.getMessage()); }
          catch (JsonParseException e) { writeError(resp,400,"JSON inválido ou malformado."); }
          catch (IllegalArgumentException e) { writeError(resp,400,e.getMessage()); }
          catch (NoSuchElementException e) { writeError(resp,404,"Registro não encontrado."); }
          catch (Exception e) { getServletContext().log("Erro na API",e); writeError(resp,500,"Erro interno ao processar a requisição."); }
    }
    private String str(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():null;}
}
