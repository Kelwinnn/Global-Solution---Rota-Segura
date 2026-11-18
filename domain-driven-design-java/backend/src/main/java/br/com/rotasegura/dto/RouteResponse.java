package br.com.rotasegura.dto;

import br.com.rotasegura.model.RouteStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rota segura de uma pessoa assistida até o abrigo (RF07). Sempre tem status e message;
 * shelter, distanceMeters e durationMin só vêm com ROUTE_FOUND, e path fica vazio nos outros casos.
 */
public record RouteResponse(
        Long assistedPersonId,
        RouteStatus status,
        ShelterInfo shelter,
        Integer distanceMeters,
        Integer durationMin,
        List<RoutePoint> path,
        String message
) {

    /** O abrigo de destino, com o endereço do nosso cadastro (o Python não o conhece). */
    public record ShelterInfo(Long id, String name, String address, BigDecimal latitude, BigDecimal longitude) {
    }

    public record RoutePoint(BigDecimal latitude, BigDecimal longitude) {
    }

    /** Resposta sem rota: só o status e a orientação. */
    public static RouteResponse withoutRoute(Long assistedPersonId, RouteStatus status, String message) {
        return new RouteResponse(assistedPersonId, status, null, null, null, List.of(), message);
    }
}
