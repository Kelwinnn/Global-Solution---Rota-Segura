package br.com.rotasegura.util;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/** Converte o caminho da requisição em URI para o campo "instance" do ProblemDetail. */
public final class RequestUris {

    private static final URI ROOT = URI.create("/");

    private RequestUris() {
    }

    /**
     * Nunca lança exceção e nunca devolve null. O caminho pode trazer caracteres inválidos
     * (ex.: espaço vindo de um cabeçalho X-Forwarded-Prefix forjado). Nesse caso:
     * primeiro tenta codificar os caracteres (espaço vira %20); se nem assim der, devolve "/".
     */
    public static URI toUri(String requestUri) {
        try {
            return URI.create(requestUri);
        } catch (RuntimeException invalid) {
            // segue para a tentativa com os caracteres codificados
        }
        try {
            return UriComponentsBuilder.fromPath(requestUri).build().encode().toUri();
        } catch (RuntimeException stillInvalid) {
            return ROOT;
        }
    }
}
