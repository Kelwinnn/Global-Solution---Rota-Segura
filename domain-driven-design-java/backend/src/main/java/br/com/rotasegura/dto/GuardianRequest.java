package br.com.rotasegura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuardianRequest(

        @NotBlank(message = "Informe o nome do responsável")
        @Size(max = 150, message = "O nome do responsável pode ter até 150 caracteres")
        String name,

        @NotBlank(message = "Informe o telefone do responsável")
        @Pattern(regexp = "^[0-9 ()+-]{8,20}$",
                message = "O telefone pode ter de 8 a 20 caracteres: números, espaço, parênteses, + e -")
        String phone,

        @NotBlank(message = "Informe o parentesco ou a relação (ex.: filha, vizinho)")
        @Size(max = 50, message = "O parentesco pode ter até 50 caracteres")
        String relationship
) {
}
