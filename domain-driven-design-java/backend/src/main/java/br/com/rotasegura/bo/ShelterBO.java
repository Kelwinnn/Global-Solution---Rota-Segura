package br.com.rotasegura.bo;

import br.com.rotasegura.dao.ShelterDAO;
import br.com.rotasegura.dto.ShelterRequest;
import br.com.rotasegura.dto.ShelterResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.Shelter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Regras de negócio dos abrigos (RF06). */
@Service
public class ShelterBO {

    private final ShelterDAO shelterDAO;

    public ShelterBO(ShelterDAO shelterDAO) {
        this.shelterDAO = shelterDAO;
    }

    @Transactional(readOnly = true)
    public List<ShelterResponse> findAll() {
        return shelterDAO.findAllOrderByName().stream()
                .map(ShelterResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShelterResponse findById(Long id) {
        return ShelterResponse.from(findEntity(id));
    }

    @Transactional
    public ShelterResponse create(ShelterRequest request) {
        checkOccupancy(request);
        Shelter shelter = new Shelter(request.name().trim(), request.address().trim(),
                request.latitude(), request.longitude(), request.capacity(), request.occupiedSpots());
        return ShelterResponse.from(shelterDAO.save(shelter));
    }

    @Transactional
    public ShelterResponse update(Long id, ShelterRequest request) {
        Shelter shelter = findEntity(id);
        checkOccupancy(request);

        shelter.setName(request.name().trim());
        shelter.setAddress(request.address().trim());
        shelter.setLatitude(request.latitude());
        shelter.setLongitude(request.longitude());
        shelter.setCapacity(request.capacity());
        shelter.setOccupiedSpots(request.occupiedSpots());
        return ShelterResponse.from(shelterDAO.save(shelter));
    }

    @Transactional
    public void delete(Long id) {
        shelterDAO.delete(findEntity(id));
    }

    /** Um abrigo não pode ter mais gente do que cabe nele. */
    private static void checkOccupancy(ShelterRequest request) {
        if (request.occupiedSpots() > request.capacity()) {
            throw new BusinessRuleException("As vagas ocupadas (%d) passam da capacidade do abrigo (%d)."
                    .formatted(request.occupiedSpots(), request.capacity()));
        }
    }

    private Shelter findEntity(Long id) {
        return shelterDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abrigo %d não encontrado.".formatted(id)));
    }
}
