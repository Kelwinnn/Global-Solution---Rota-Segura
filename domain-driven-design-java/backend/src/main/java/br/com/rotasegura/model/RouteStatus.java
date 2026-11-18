package br.com.rotasegura.model;

/**
 * Resultado do pedido de rota até o abrigo (RF07). Não é gravado.
 * Os dois primeiros vêm do serviço Python (contrato v2); os dois últimos são da API Java (decidido em 18/11).
 */
public enum RouteStatus {
    /** Achou rota até o abrigo com vaga mais próximo (RN06). */
    ROUTE_FOUND,
    /** Não há caminho sem rua alagada (RN05): a orientação é ir para um local alto. */
    NO_SAFE_ROUTE,
    /** O serviço de rota não respondeu (fora do ar, lento ou com erro). */
    ROUTE_UNAVAILABLE,
    /** A pessoa mora fora do mapa do bairro-piloto (Jardim Helena). */
    OUTSIDE_PILOT_AREA
}
