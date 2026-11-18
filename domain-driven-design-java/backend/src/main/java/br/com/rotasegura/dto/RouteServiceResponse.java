package br.com.rotasegura.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta 200 do POST /route do serviço Python (contrato v2).
 * status é ROUTE_FOUND ou NO_SAFE_ROUTE; sem rota, shelter, distanceMeters e durationMin vêm nulos e path vazio.
 */
public record RouteServiceResponse(
        String status,
        ShelterOutput shelter,
        Integer distanceMeters,
        Integer durationMin,
        List<Point> path,
        String message
) {

    public record ShelterOutput(Long id, String name, BigDecimal lat, BigDecimal lng) {
    }

    public record Point(BigDecimal lat, BigDecimal lng) {
    }
}
