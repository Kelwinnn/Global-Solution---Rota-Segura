package br.com.rotasegura.bo;

import br.com.rotasegura.config.ValidationConfig;
import br.com.rotasegura.dao.RiskAreaDAO;
import br.com.rotasegura.dao.SensorReadingDAO;
import br.com.rotasegura.dto.AreaRiskResponse;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskLevel;
import br.com.rotasegura.model.SensorReading;
import br.com.rotasegura.model.SensorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/** Nível de risco das áreas (RF04, RN01), calculado na hora a partir das leituras. */
@Service
public class RiskLevelBO {

    /** Leitura mais velha que isto não vale para o nível atual (sensor que parou de enviar). */
    static final Duration READING_MAX_AGE = Duration.ofHours(1);

    // Faixas de RN01 (09/11), com as bordas de 10/11: o valor fica no nível cujo número inicial já alcançou
    private static final BigDecimal MEDIUM_FROM = new BigDecimal("1.10");
    private static final BigDecimal HIGH_FROM = new BigDecimal("3.00");
    private static final BigDecimal VERY_HIGH_FROM = new BigDecimal("5.10");
    private static final BigDecimal VERY_URGENT_ABOVE = new BigDecimal("10.00");

    /** Entre duas leituras, fica a mais recente (empate impossível no mesmo sensor: UX_RS_READING_SENSOR_TIME). */
    private static final BinaryOperator<SensorReading> LATEST =
            (a, b) -> a.getMeasuredAt().isAfter(b.getMeasuredAt()) ? a : b;

    /**
     * A pior leitura: maior nível d'água; no empate, a mais recente; se ainda empatar, a do sensor de código
     * "maior" na ordem alfabética (só para o resultado nunca depender da ordem em que as leituras chegaram).
     */
    private static final Comparator<SensorReading> WORST =
            Comparator.comparing(SensorReading::getWaterLevelCm)
                    .thenComparing(SensorReading::getMeasuredAt)
                    .thenComparing(reading -> reading.getSensor().getCode());

    private final RiskAreaDAO riskAreaDAO;
    private final SensorReadingDAO sensorReadingDAO;
    private final Clock clock;

    public RiskLevelBO(RiskAreaDAO riskAreaDAO, SensorReadingDAO sensorReadingDAO, Clock clock) {
        this.riskAreaDAO = riskAreaDAO;
        this.sensorReadingDAO = sensorReadingDAO;
        this.clock = clock;
    }

    /** RN01: nível pelo nível d'água em cm, igual para todos os sensores. */
    public RiskLevel classify(BigDecimal waterLevelCm) {
        if (waterLevelCm.compareTo(MEDIUM_FROM) < 0) {
            return RiskLevel.LOW;
        }
        if (waterLevelCm.compareTo(HIGH_FROM) < 0) {
            return RiskLevel.MEDIUM;
        }
        if (waterLevelCm.compareTo(VERY_HIGH_FROM) < 0) {
            return RiskLevel.HIGH;
        }
        if (waterLevelCm.compareTo(VERY_URGENT_ABOVE) <= 0) {
            return RiskLevel.VERY_HIGH;
        }
        return RiskLevel.VERY_URGENT;
    }

    /** Nível de todas as áreas, em ordem de nome (as sem leitura recente vêm com hasData false). */
    @Transactional(readOnly = true)
    public List<AreaRiskResponse> findAll() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        // Uma consulta só para todas as áreas; o agrupamento é feito aqui
        // Só sensores ativos: um sensor desligado não diz nada sobre a água agora
        Map<Long, List<SensorReading>> readingsByArea = sensorReadingDAO
                .findBySensorStatusBetween(SensorStatus.ACTIVE,
                        now.minus(READING_MAX_AGE), now.plus(ValidationConfig.FUTURE_TOLERANCE))
                .stream()
                .collect(Collectors.groupingBy(reading -> reading.getSensor().getArea().getId()));

        return riskAreaDAO.findAllOrderByName().stream()
                .map(area -> riskOf(area, readingsByArea.getOrDefault(area.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AreaRiskResponse findByArea(Long areaId) {
        RiskArea area = riskAreaDAO.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área de risco %d não encontrada.".formatted(areaId)));
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<SensorReading> readings = sensorReadingDAO.findByAreaAndSensorStatusBetween(areaId, SensorStatus.ACTIVE,
                now.minus(READING_MAX_AGE), now.plus(ValidationConfig.FUTURE_TOLERANCE));
        return riskOf(area, readings);
    }

    /** Regra da área: a leitura mais recente de cada sensor, e dessas, a pior. */
    private AreaRiskResponse riskOf(RiskArea area, List<SensorReading> recentReadings) {
        Collection<SensorReading> latestPerSensor = recentReadings.stream()
                .collect(Collectors.toMap(reading -> reading.getSensor().getId(), reading -> reading, LATEST))
                .values();

        Optional<SensorReading> worst = latestPerSensor.stream().max(WORST);
        if (worst.isEmpty()) {
            return AreaRiskResponse.noData(area.getId(), area.getName());
        }

        SensorReading reading = worst.get();
        return new AreaRiskResponse(
                area.getId(),
                area.getName(),
                true,
                classify(reading.getWaterLevelCm()),
                reading.getWaterLevelCm(),
                reading.getSensor().getCode(),
                reading.getMeasuredAt());
    }
}
