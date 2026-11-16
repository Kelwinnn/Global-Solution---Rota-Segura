package br.com.rotasegura.dto;

import br.com.rotasegura.model.SensorReading;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SensorReadingResponse(
        Long id,
        Long sensorId,
        String sensorCode,
        Long areaId,
        BigDecimal rainMm,
        BigDecimal waterLevelCm,
        OffsetDateTime measuredAt
) {

    /** Lê o sensor da leitura: precisa ser chamado dentro da transação do BO (o relacionamento é LAZY). */
    public static SensorReadingResponse from(SensorReading reading) {
        return new SensorReadingResponse(
                reading.getId(),
                reading.getSensor().getId(),
                reading.getSensor().getCode(),
                reading.getSensor().getArea().getId(),
                reading.getRainMm(),
                reading.getWaterLevelCm(),
                reading.getMeasuredAt());
    }
}
