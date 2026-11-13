package br.com.mvc.api;

import br.com.mvc.util.JwtUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class JwtFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if ("/api/auth/login".equals(path) || "/api/health".equals(path)) { chain.doFilter(request, response); return; }
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) { ApiController.writeError(resp, 401, "Token Bearer obrigatório."); return; }
        JsonObject claims = JwtUtil.verify(auth.substring(7).trim());
        if (claims == null) { ApiController.writeError(resp, 401, "Token inválido ou expirado."); return; }
        req.setAttribute("jwt.claims", claims);
        chain.doFilter(request, response);
    }
}
