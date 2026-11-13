package br.com.rotasegura.dto;

import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskType;

import java.math.BigDecimal;

public record RiskAreaResponse(
        Long id,
        String name,
        String city,
        String district,
        RiskType riskType,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer radiusM
) {

    public static RiskAreaResponse from(RiskArea area) {
        return new RiskAreaResponse(
                area.getId(),
                area.getName(),
                area.getCity(),
                area.getDistrict(),
                area.getRiskType(),
                area.getLatitude(),
                area.getLongitude(),
                area.getRadiusM());
    }
}
