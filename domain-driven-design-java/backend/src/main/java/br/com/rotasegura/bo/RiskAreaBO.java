package br.com.rotasegura.bo;

import br.com.rotasegura.dao.RiskAreaDAO;
import br.com.rotasegura.dao.SensorDAO;
import br.com.rotasegura.dto.RiskAreaRequest;
import br.com.rotasegura.dto.RiskAreaResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.RiskArea;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Regras de negócio das áreas de risco (RF01). */
@Service
public class RiskAreaBO {

    private final RiskAreaDAO riskAreaDAO;
    private final SensorDAO sensorDAO;

    public RiskAreaBO(RiskAreaDAO riskAreaDAO, SensorDAO sensorDAO) {
        this.riskAreaDAO = riskAreaDAO;
        this.sensorDAO = sensorDAO;
    }

    @Transactional(readOnly = true)
    public List<RiskAreaResponse> findAll() {
        return riskAreaDAO.findAllOrderByName().stream()
                .map(RiskAreaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RiskAreaResponse findById(Long id) {
        return RiskAreaResponse.from(findEntity(id));
    }

    @Transactional
    public RiskAreaResponse create(RiskAreaRequest request) {
        String name = request.name().trim();
        String city = request.city().trim();
        if (riskAreaDAO.existsByNameAndCity(name, city)) {
            throw duplicated(name, city);
        }

        RiskArea area = new RiskArea(name, city, normalizeDistrict(request.district()), request.riskType(),
                request.latitude(), request.longitude(), request.radiusM());
        return RiskAreaResponse.from(riskAreaDAO.save(area));
    }

    @Transactional
    public RiskAreaResponse update(Long id, RiskAreaRequest request) {
        RiskArea area = findEntity(id);

        String name = request.name().trim();
        String city = request.city().trim();
        if (riskAreaDAO.existsByNameAndCityExcludingId(name, city, id)) {
            throw duplicated(name, city);
        }

        area.setName(name);
        area.setCity(city);
        area.setDistrict(normalizeDistrict(request.district()));
        area.setRiskType(request.riskType());
        area.setLatitude(request.latitude());
        area.setLongitude(request.longitude());
        area.setRadiusM(request.radiusM());
        return RiskAreaResponse.from(riskAreaDAO.save(area));
    }

    @Transactional
    public void delete(Long id) {
        RiskArea area = findEntity(id);
        // Sem cascata: ninguém apaga sensores sem querer ao excluir uma área
        if (sensorDAO.existsByAreaId(id)) {
            throw new BusinessRuleException(
                    "A área '%s' tem sensores vinculados. Exclua ou mova os sensores antes.".formatted(area.getName()));
        }
        riskAreaDAO.delete(area);
    }

    private RiskArea findEntity(Long id) {
        return riskAreaDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Área de risco %d não encontrada.".formatted(id)));
    }

    /** Bairro é opcional: texto vazio vira null. */
    private static String normalizeDistrict(String district) {
        return district == null || district.isBlank() ? null : district.trim();
    }

    private static BusinessRuleException duplicated(String name, String city) {
        return new BusinessRuleException("Já existe uma área de risco chamada '%s' em %s.".formatted(name, city));
    }
}
