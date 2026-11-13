package br.com.rotasegura.security;

import br.com.rotasegura.util.RequestUris;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escreve um ProblemDetail (o mesmo formato de erro do resto da API) direto na resposta.
 * Os filtros de segurança rodam antes do Spring MVC, então o GlobalExceptionHandler não alcança estes erros.
 */
class SecurityProblemWriter {

    private final ObjectMapper objectMapper;

    SecurityProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 401: o mesmo texto para "sem login" e "sessão expirada". */
    void writeLoginRequired(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "Login necessário", "Faça login para continuar.");
    }

    void write(HttpServletRequest request, HttpServletResponse response,
               HttpStatus status, String title, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(RequestUris.toUri(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
