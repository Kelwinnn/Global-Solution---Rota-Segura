package br.com.rotasegura.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Corpo do POST /route do serviço Python (contrato v2, dynamic-programming/docs/contrato.md).
 * Os nomes seguem o contrato (lat, lng, availableSpots, radiusM), e não o padrão da nossa API.
 */
public record RouteServiceRequest(
        Point origin,
        List<ShelterInput> shelters,
        List<FloodedArea> floodedAreas
) {

    public record Point(BigDecimal lat, BigDecimal lng) {
    }

    /** O Python ignora abrigo com availableSpots 0 (RN06): mandamos todos. */
    public record ShelterInput(Long id, String name, BigDecimal lat, BigDecimal lng, Integer availableSpots) {
    }

    /** Círculo de área alagada: o Python tira do mapa as ruas dentro dele (RN05). */
    public record FloodedArea(Long areaId, BigDecimal lat, BigDecimal lng, Integer radiusM) {
    }
}
