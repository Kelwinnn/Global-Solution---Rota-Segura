package br.com.rotasegura.bo;

import br.com.rotasegura.dao.SensorDAO;
import br.com.rotasegura.dao.SensorReadingDAO;
import br.com.rotasegura.dto.SensorReadingRequest;
import br.com.rotasegura.dto.SensorReadingResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.Sensor;
import br.com.rotasegura.model.SensorReading;
import br.com.rotasegura.model.SensorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Regras de negócio das leituras dos sensores (RF03). */
@Service
public class SensorReadingBO {

    private final SensorReadingDAO sensorReadingDAO;
    private final SensorDAO sensorDAO;
    private final Clock clock;

    public SensorReadingBO(SensorReadingDAO sensorReadingDAO, SensorDAO sensorDAO, Clock clock) {
        this.sensorReadingDAO = sensorReadingDAO;
        this.sensorDAO = sensorDAO;
        this.clock = clock;
    }

    @Transactional
    public SensorReadingResponse create(SensorReadingRequest request) {
        String code = request.sensorCode().trim();
        Sensor sensor = sensorDAO.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor '%s' não encontrado.".formatted(code)));

        // Sensor inativo está desligado: não recebe leitura nova (o histórico dele continua consultável)
        if (sensor.getStatus() == SensorStatus.INACTIVE) {
            throw new BusinessRuleException(
                    "O sensor '%s' está inativo e não recebe leituras novas.".formatted(sensor.getCode()));
        }

        // O banco guarda até microssegundos: corta o resto antes de comparar e de gravar,
        // para a checagem de repetida e a resposta baterem com o que fica gravado
        OffsetDateTime measuredAt = request.measuredAt().truncatedTo(ChronoUnit.MICROS);
        if (sensorReadingDAO.existsBySensorAt(sensor.getId(), measuredAt)) {
            throw new BusinessRuleException("Já existe uma leitura do sensor '%s' em %s."
                    .formatted(sensor.getCode(), DateTimeFormatter.ISO_INSTANT.format(measuredAt)));
        }

        // Sempre 2 casas decimais, como no banco (12.4 vira 12.40); o @Digits já garante que não há arredondamento
        SensorReading reading = new SensorReading(sensor,
                request.rainMm().setScale(2),
                request.waterLevelCm().setScale(2),
                measuredAt);
        return SensorReadingResponse.from(sensorReadingDAO.save(reading));
    }

    /** Leituras de um sensor, mais recentes primeiro. Sem "from", desde o começo; sem "to", até agora. */
    @Transactional(readOnly = true)
    public List<SensorReadingResponse> findBySensor(Long sensorId, OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime end = to != null ? to : OffsetDateTime.now(clock);
        List<SensorReading> readings = from != null
                ? sensorReadingDAO.findBySensorBetween(sensorId, from, end)
                : sensorReadingDAO.findBySensorUntil(sensorId, end);
        return readings.stream()
                .map(SensorReadingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SensorReadingResponse findById(Long id) {
        return sensorReadingDAO.findById(id)
                .map(SensorReadingResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Leitura %d não encontrada.".formatted(id)));
    }
}
