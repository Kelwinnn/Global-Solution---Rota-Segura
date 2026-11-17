package br.com.rotasegura.dto;

import br.com.rotasegura.model.Shelter;

import java.math.BigDecimal;

public record ShelterResponse(
        Long id,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer capacity,
        Integer occupiedSpots,
        Integer availableSpots
) {

    public static ShelterResponse from(Shelter shelter) {
        return new ShelterResponse(
                shelter.getId(),
                shelter.getName(),
                shelter.getAddress(),
                shelter.getLatitude(),
                shelter.getLongitude(),
                shelter.getCapacity(),
                shelter.getOccupiedSpots(),
                shelter.getAvailableSpots());
    }
}
