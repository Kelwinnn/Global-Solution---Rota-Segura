package br.com.rotasegura.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/** Rota protegida sem login: 401 em ProblemDetail (sem redirecionar para página de login). */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityProblemWriter problemWriter;

    RestAuthenticationEntryPoint(SecurityProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        problemWriter.writeLoginRequired(request, response);
    }
}
