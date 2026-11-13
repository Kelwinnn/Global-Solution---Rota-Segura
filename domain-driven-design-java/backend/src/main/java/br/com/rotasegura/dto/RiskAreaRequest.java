package br.com.rotasegura.dto;

import br.com.rotasegura.model.RiskType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RiskAreaRequest(

        @NotBlank(message = "Informe o nome da área")
        @Size(max = 100, message = "O nome pode ter até 100 caracteres")
        String name,

        @NotBlank(message = "Informe a cidade")
        @Size(max = 60, message = "A cidade pode ter até 60 caracteres")
        String city,

        @Size(max = 60, message = "O bairro pode ter até 60 caracteres")
        String district,

        @NotNull(message = "Informe o tipo de risco (FLOOD ou LANDSLIDE)")
        RiskType riskType,

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

        @NotNull(message = "Informe o raio em metros")
        @Positive(message = "O raio deve ser maior que zero")
        Integer radiusM
) {
}
