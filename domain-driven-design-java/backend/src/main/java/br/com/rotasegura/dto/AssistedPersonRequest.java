package br.com.rotasegura.dto;

import br.com.rotasegura.model.SpecialNeed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Pessoa assistida com os responsáveis dela, cadastrados juntos (RN03). */
public record AssistedPersonRequest(

        @NotBlank(message = "Informe o nome da pessoa assistida")
        @Size(max = 150, message = "O nome pode ter até 150 caracteres")
        String name,

        @NotBlank(message = "Informe o telefone da pessoa assistida")
        @Pattern(regexp = "^[0-9 ()+-]{8,20}$",
                message = "O telefone pode ter de 8 a 20 caracteres: números, espaço, parênteses, + e -")
        String phone,

        @NotNull(message = "Informe a área de risco onde a pessoa mora")
        Long areaId,

        @NotNull(message = "Informe a latitude de onde a pessoa mora")
        @DecimalMin(value = "-90", message = "A latitude vai de -90 a 90")
        @DecimalMax(value = "90", message = "A latitude vai de -90 a 90")
        @Digits(integer = 3, fraction = 6, message = "A latitude aceita até 6 casas decimais")
        BigDecimal latitude,

        @NotNull(message = "Informe a longitude de onde a pessoa mora")
        @DecimalMin(value = "-180", message = "A longitude vai de -180 a 180")
        @DecimalMax(value = "180", message = "A longitude vai de -180 a 180")
        @Digits(integer = 3, fraction = 6, message = "A longitude aceita até 6 casas decimais")
        BigDecimal longitude,

        @NotNull(message = "Informe a necessidade (ELDERLY, BEDRIDDEN, WHEELCHAIR ou OTHER)")
        SpecialNeed specialNeed,

        @Size(max = 500, message = "As observações podem ter até 500 caracteres")
        String notes,

        /* @Valid valida também cada responsável da lista */
        @NotEmpty(message = "Informe ao menos um responsável (RN03)")
        @Size(max = 10, message = "No máximo 10 responsáveis por pessoa")
        List<@Valid @NotNull(message = "Responsável vazio na lista") GuardianRequest> guardians
) {
}
