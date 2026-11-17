package br.com.rotasegura.dto;

import br.com.rotasegura.model.AssistedPerson;
import br.com.rotasegura.model.SpecialNeed;

import java.math.BigDecimal;
import java.util.List;

public record AssistedPersonResponse(
        Long id,
        String name,
        String phone,
        Long areaId,
        String areaName,
        BigDecimal latitude,
        BigDecimal longitude,
        SpecialNeed specialNeed,
        String notes,
        List<GuardianResponse> guardians
) {

    /** Lê a área e os responsáveis: precisa ser chamado dentro da transação do BO (são LAZY). */
    public static AssistedPersonResponse from(AssistedPerson person) {
        return new AssistedPersonResponse(
                person.getId(),
                person.getName(),
                person.getPhone(),
                person.getArea().getId(),
                person.getArea().getName(),
                person.getLatitude(),
                person.getLongitude(),
                person.getSpecialNeed(),
                person.getNotes(),
                person.getGuardians().stream().map(GuardianResponse::from).toList());
    }
}
