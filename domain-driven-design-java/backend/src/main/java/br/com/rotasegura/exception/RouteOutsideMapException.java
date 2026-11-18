package br.com.rotasegura.exception;

/**
 * O serviço de rota recusou a origem (400): a pessoa está a mais de 300 m de uma rua do mapa do bairro-piloto.
 * Interna: o RouteBO a transforma no status OUTSIDE_PILOT_AREA; nunca chega ao GlobalExceptionHandler.
 */
public class RouteOutsideMapException extends RuntimeException {

    public RouteOutsideMapException(String message) {
        super(message);
    }
}
