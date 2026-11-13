package br.com.rotasegura.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;

/**
 * Acesso negado: 401 se não há login (inclusive com a sessão expirada), 403 se há login.
 * Sem isto, o token CSRF de uma sessão expirada daria 403 e o front não saberia que precisa de novo login.
 * A resposta sai em ProblemDetail, como os outros erros da API.
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityProblemWriter problemWriter;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    RestAccessDeniedHandler(SecurityProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();
        boolean loggedIn = trustResolver.isAuthenticated(authentication);

        if (!loggedIn) {
            problemWriter.writeLoginRequired(request, response);
        } else if (accessDeniedException instanceof CsrfException) {
            problemWriter.write(request, response, HttpStatus.FORBIDDEN, "Token CSRF inválido",
                    "Envie o cabeçalho X-CSRF-TOKEN com o token de GET /api/auth/csrf.");
        } else {
            problemWriter.write(request, response, HttpStatus.FORBIDDEN, "Acesso negado",
                    "Você não tem permissão para esta ação.");
        }
    }
}
