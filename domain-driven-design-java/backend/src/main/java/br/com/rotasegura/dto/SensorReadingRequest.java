package br.com.rotasegura.dto;

import br.com.rotasegura.util.StripTrailingZerosDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Leitura enviada pelo aparelho do sensor. */
public record SensorReadingRequest(

        @NotBlank(message = "Informe o código do sensor")
        @Size(max = 30, message = "O código pode ter até 30 caracteres")
        String sensorCode,

        @JsonDeserialize(using = StripTrailingZerosDeserializer.class)
        @NotNull(message = "Informe a chuva em mm")
        @DecimalMin(value = "0", message = "A chuva não pode ser negativa")
        @DecimalMax(value = "9999.99", message = "A chuva vai até 9999,99 mm, com até 2 casas decimais")
        @Digits(integer = 4, fraction = 2, message = "A chuva vai até 9999,99 mm, com até 2 casas decimais")
        BigDecimal rainMm,

        @JsonDeserialize(using = StripTrailingZerosDeserializer.class)
        @NotNull(message = "Informe o nível d'água em cm")
        @DecimalMin(value = "0", message = "O nível d'água não pode ser negativo")
        @DecimalMax(value = "99999.99", message = "O nível d'água vai até 99999,99 cm, com até 2 casas decimais")
        @Digits(integer = 5, fraction = 2, message = "O nível d'água vai até 99999,99 cm, com até 2 casas decimais")
        BigDecimal waterLevelCm,

        @NotNull(message = "Informe a data e a hora da medição")
        // Folga de 5 minutos para relógio adiantado: config/ValidationConfig
        @PastOrPresent(message = "A data e a hora da medição não podem estar no futuro")
        OffsetDateTime measuredAt
) {
}
