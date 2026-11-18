package br.com.rotasegura.exception;

/**
 * O serviço de rota (Python) não respondeu direito: fora do ar, mais de 10 s, erro 5xx ou resposta fora do contrato.
 * Interna: o RouteBO a transforma no status ROUTE_UNAVAILABLE; nunca chega ao GlobalExceptionHandler.
 */
public class RouteServiceUnavailableException extends RuntimeException {

    public RouteServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
