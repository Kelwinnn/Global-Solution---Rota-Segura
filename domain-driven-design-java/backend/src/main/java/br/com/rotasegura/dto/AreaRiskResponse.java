package br.com.rotasegura.dto;

import br.com.rotasegura.model.RiskLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Nível de risco atual de uma área (RF04).
 * Sem leitura recente, hasData é false e os campos da leitura vêm vazios (null).
 */
public record AreaRiskResponse(
        Long areaId,
        String areaName,
        boolean hasData,
        RiskLevel riskLevel,
        BigDecimal waterLevelCm,
        String sensorCode,
        OffsetDateTime measuredAt
) {

    public static AreaRiskResponse noData(Long areaId, String areaName) {
        return new AreaRiskResponse(areaId, areaName, false, null, null, null, null);
    }
}
