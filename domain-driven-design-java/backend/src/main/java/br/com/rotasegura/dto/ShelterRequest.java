package br.com.rotasegura.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ShelterRequest(

        @NotBlank(message = "Informe o nome do abrigo")
        @Size(max = 150, message = "O nome pode ter até 150 caracteres")
        String name,

        @NotBlank(message = "Informe o endereço do abrigo")
        @Size(max = 200, message = "O endereço pode ter até 200 caracteres")
        String address,

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

        @NotNull(message = "Informe a capacidade")
        @Positive(message = "A capacidade deve ser maior que zero")
        Integer capacity,

        @NotNull(message = "Informe as vagas ocupadas (0 se o abrigo estiver vazio)")
        @PositiveOrZero(message = "As vagas ocupadas não podem ser negativas")
        Integer occupiedSpots
) {
}
