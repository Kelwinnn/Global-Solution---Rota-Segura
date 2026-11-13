package br.com.rotasegura.dto;

import br.com.rotasegura.model.SensorStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SensorRequest(

        @NotNull(message = "Informe a área do sensor")
        Long areaId,

        @NotBlank(message = "Informe o código do sensor")
        @Size(max = 30, message = "O código pode ter até 30 caracteres")
        String code,

        @NotNull(message = "Informe a latitude")
        @DecimalMin(value = "-90", message = "A latitude vai de -90 a 90")
        @DecimalMax(value = "90", message = "A latitude vai de -90 a 90")
        @Digits(integer = 3, fraction = 6, message = "A latitude aceita até 6 casas decimais")
        BigDecimal latitude,

        @NotNull(message = "Informe a longitude")
        @DecimalMin(value = "-180", message = "A longitude vai de -180 a 180")
        @DecimalMax(value = "180", message = "A longitude vai de -180 a 180")
        @Digits(integer = 3, fraction = 6, message = "A longitude aceita até 6 casas decimais")
        BigDecimal longitude,

        @NotNull(message = "Informe a situação do sensor (ACTIVE ou INACTIVE)")
        SensorStatus status
) {
}
