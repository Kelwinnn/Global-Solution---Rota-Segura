package br.com.rotasegura.exception;

import br.com.rotasegura.dto.InvalidFieldResponse;
import br.com.rotasegura.util.RequestUris;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;

/**
 * Converte as exceções em respostas ProblemDetail (RFC 7807), com textos em português.
 * Estende ResponseEntityExceptionHandler para que os erros do próprio Spring MVC (405, 415...)
 * também saiam como ProblemDetail.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- Exceções do nosso domínio ----

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Regra de negócio violada", ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        // O motivo do banco (ex.: nome da constraint) fica só no log, nunca na resposta
        log.warn("Conflito de integridade no banco: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflito com dados existentes",
                "A operação conflita com dados já cadastrados ou ligados a este registro.", request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Login não realizado", ex.getMessage(), request);
    }

    /**
     * Erro de segurança que surgir dentro de um controller volta para o Spring Security,
     * que responde 401 ou 403 do mesmo jeito que nos filtros. Sem isto, cairia no 500 abaixo.
     * Atenção: uma AuthenticationException aqui sempre vira 401 "Login necessário", mesmo com
     * sessão válida; fora do login, nenhum código do projeto deve lançá-la.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void handleSecurityException(RuntimeException ex) {
        throw ex;
    }

    /** Qualquer erro não previsto: 500 sem detalhes internos na resposta. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Algo deu errado do nosso lado. Tente de novo em instantes.", request);
    }

    // ---- Erros do Spring MVC com texto próprio ----

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<InvalidFieldResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidFieldResponse(error.getField(), error.getDefaultMessage()))
                // Ordem fixa: sem isto, a ordem dos campos mudava de uma execução para outra
                .sorted(Comparator.comparing(InvalidFieldResponse::field)
                        .thenComparing(InvalidFieldResponse::message))
                .toList();

        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Confira os campos indicados.");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido",
                "O JSON está malformado ou algum campo tem tipo ou formato inválido.");
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "O valor '%s' não é válido para '%s'.".formatted(ex.getValue(), ex.getPropertyName()));
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Todo erro tratado pela classe-mãe (inclusive os três acima) passa por aqui antes de sair.
     * Preenche o "instance" com a conversão segura; sem isto, o Spring tentaria converter sozinho
     * e falharia quando o caminho tem caractere inválido.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem && problem.getInstance() == null
                && request instanceof ServletWebRequest servletRequest) {
            setInstance(problem, servletRequest.getRequest());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    // ---- Montagem do ProblemDetail ----

    private static ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = problem(status, title, detail);
        setInstance(problem, request);
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    private static void setInstance(ProblemDetail problem, HttpServletRequest request) {
        problem.setInstance(RequestUris.toUri(request.getRequestURI()));
    }
}
