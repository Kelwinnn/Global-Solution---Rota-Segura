package br.com.rotasegura.bo;

import br.com.rotasegura.dao.RiskAreaDAO;
import br.com.rotasegura.dao.SensorDAO;
import br.com.rotasegura.dto.SensorRequest;
import br.com.rotasegura.dto.SensorResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.Sensor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Regras de negócio dos sensores (RF02). */
@Service
public class SensorBO {

    private final SensorDAO sensorDAO;
    private final RiskAreaDAO riskAreaDAO;

    public SensorBO(SensorDAO sensorDAO, RiskAreaDAO riskAreaDAO) {
        this.sensorDAO = sensorDAO;
        this.riskAreaDAO = riskAreaDAO;
    }

    /** Todos os sensores ou, com areaId, só os daquela área. Área inexistente devolve lista vazia. */
    @Transactional(readOnly = true)
    public List<SensorResponse> findAll(Long areaId) {
        List<Sensor> sensors = areaId == null
                ? sensorDAO.findAllOrderByCode()
                : sensorDAO.findByAreaIdOrderByCode(areaId);
        return sensors.stream()
                .map(SensorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SensorResponse findById(Long id) {
        return SensorResponse.from(findEntity(id));
    }

    @Transactional
    public SensorResponse create(SensorRequest request) {
        RiskArea area = findArea(request.areaId());
        String code = request.code().trim();
        if (sensorDAO.existsByCode(code)) {
            throw duplicated(code);
        }

        Sensor sensor = new Sensor(area, code, request.latitude(), request.longitude(), request.status());
        return SensorResponse.from(sensorDAO.save(sensor));
    }

    @Transactional
    public SensorResponse update(Long id, SensorRequest request) {
        Sensor sensor = findEntity(id);
        RiskArea area = findArea(request.areaId());
        String code = request.code().trim();
        if (sensorDAO.existsByCodeExcludingId(code, id)) {
            throw duplicated(code);
        }

        sensor.setArea(area);
        sensor.setCode(code);
        sensor.setLatitude(request.latitude());
        sensor.setLongitude(request.longitude());
        sensor.setStatus(request.status());
        return SensorResponse.from(sensorDAO.save(sensor));
    }

    @Transactional
    public void delete(Long id) {
        sensorDAO.delete(findEntity(id));
    }

    private Sensor findEntity(Long id) {
        return sensorDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor %d não encontrado.".formatted(id)));
    }

    /** Todo sensor pertence a uma área que já existe. */
    private RiskArea findArea(Long areaId) {
        return riskAreaDAO.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área de risco %d não encontrada.".formatted(areaId)));
    }

    private static BusinessRuleException duplicated(String code) {
        return new BusinessRuleException("Já existe um sensor com o código '%s'.".formatted(code));
    }
}
