package br.com.rotasegura.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Acesso negado: 401 se não há login (inclusive com a sessão expirada), 403 se há login.
 * Sem isto, o token CSRF de uma sessão expirada daria 403 e o front não saberia que precisa de novo login.
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {
        Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();
        boolean loggedIn = trustResolver.isAuthenticated(authentication);
        response.setStatus(loggedIn ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED);
    }
}
