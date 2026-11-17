package br.com.rotasegura.bo;

import br.com.rotasegura.dao.AssistedPersonDAO;
import br.com.rotasegura.dao.RiskAreaDAO;
import br.com.rotasegura.dto.AssistedPersonRequest;
import br.com.rotasegura.dto.AssistedPersonResponse;
import br.com.rotasegura.dto.GuardianRequest;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.AssistedPerson;
import br.com.rotasegura.model.Guardian;
import br.com.rotasegura.model.RiskArea;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Regras de negócio das pessoas assistidas e dos responsáveis (RF08, RN03). */
@Service
public class AssistedPersonBO {

    private final AssistedPersonDAO assistedPersonDAO;
    private final RiskAreaDAO riskAreaDAO;

    public AssistedPersonBO(AssistedPersonDAO assistedPersonDAO, RiskAreaDAO riskAreaDAO) {
        this.assistedPersonDAO = assistedPersonDAO;
        this.riskAreaDAO = riskAreaDAO;
    }

    /** Todos os assistidos ou, com areaId, só os daquela área. Área inexistente devolve lista vazia. */
    @Transactional(readOnly = true)
    public List<AssistedPersonResponse> findAll(Long areaId) {
        List<AssistedPerson> people = areaId == null
                ? assistedPersonDAO.findAllOrderByName()
                : assistedPersonDAO.findByAreaIdOrderByName(areaId);
        return people.stream().map(AssistedPersonResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AssistedPersonResponse findById(Long id) {
        return AssistedPersonResponse.from(findEntity(id));
    }

    @Transactional
    public AssistedPersonResponse create(AssistedPersonRequest request) {
        RiskArea area = findArea(request.areaId());
        List<Guardian> guardians = toGuardians(request.guardians());

        AssistedPerson person = new AssistedPerson(area, request.name().trim(), request.phone().trim(),
                request.latitude(), request.longitude(), request.specialNeed(), normalizeNotes(request.notes()));
        person.replaceGuardians(guardians);
        return AssistedPersonResponse.from(assistedPersonDAO.save(person));
    }

    /** Troca todos os dados, inclusive a lista de responsáveis (os que saem da lista são excluídos). */
    @Transactional
    public AssistedPersonResponse update(Long id, AssistedPersonRequest request) {
        AssistedPerson person = findEntity(id);
        RiskArea area = findArea(request.areaId());
        List<Guardian> guardians = toGuardians(request.guardians());

        person.setArea(area);
        person.setName(request.name().trim());
        person.setPhone(request.phone().trim());
        person.setLatitude(request.latitude());
        person.setLongitude(request.longitude());
        person.setSpecialNeed(request.specialNeed());
        person.setNotes(normalizeNotes(request.notes()));
        person.replaceGuardians(guardians);
        return AssistedPersonResponse.from(assistedPersonDAO.save(person));
    }

    /** Exclui o assistido e, em cascata, os responsáveis dele. */
    @Transactional
    public void delete(Long id) {
        assistedPersonDAO.delete(findEntity(id));
    }

    /** RN03: todo assistido tem ao menos um responsável. */
    private static List<Guardian> toGuardians(List<GuardianRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessRuleException("Toda pessoa assistida precisa de ao menos um responsável (RN03).");
        }
        return requests.stream()
                .map(guardian -> new Guardian(guardian.name().trim(), guardian.phone().trim(),
                        guardian.relationship().trim()))
                .toList();
    }

    /** Observação é opcional: texto vazio vira null. */
    private static String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }

    private AssistedPerson findEntity(Long id) {
        return assistedPersonDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa assistida %d não encontrada.".formatted(id)));
    }

    /** RN03: todo assistido tem uma área que já existe. */
    private RiskArea findArea(Long areaId) {
        return riskAreaDAO.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área de risco %d não encontrada.".formatted(areaId)));
    }
}
