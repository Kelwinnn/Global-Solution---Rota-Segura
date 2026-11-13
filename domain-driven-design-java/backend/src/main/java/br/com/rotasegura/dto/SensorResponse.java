package br.com.rotasegura.dto;

import br.com.rotasegura.model.Sensor;
import br.com.rotasegura.model.SensorStatus;

import java.math.BigDecimal;

public record SensorResponse(
        Long id,
        String code,
        Long areaId,
        String areaName,
        BigDecimal latitude,
        BigDecimal longitude,
        SensorStatus status
) {

    /** Lê a área do sensor: precisa ser chamado dentro da transação do BO (o relacionamento é LAZY). */
    public static SensorResponse from(Sensor sensor) {
        return new SensorResponse(
                sensor.getId(),
                sensor.getCode(),
                sensor.getArea().getId(),
                sensor.getArea().getName(),
                sensor.getLatitude(),
                sensor.getLongitude(),
                sensor.getStatus());
    }
}
