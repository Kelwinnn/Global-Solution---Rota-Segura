package br.com.rotasegura.dao;

import br.com.rotasegura.model.SensorReading;
import br.com.rotasegura.repository.SensorReadingRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Acesso a dados das leituras dos sensores. Sem regra de negócio. */
@Component
public class SensorReadingDAO {

    private final SensorReadingRepository repository;

    public SensorReadingDAO(SensorReadingRepository repository) {
        this.repository = repository;
    }

    public SensorReading save(SensorReading reading) {
        return repository.save(reading);
    }

    public Optional<SensorReading> findById(Long id) {
        return repository.findById(id);
    }

    public List<SensorReading> findBySensorBetween(Long sensorId, OffsetDateTime from, OffsetDateTime to) {
        return repository.findAllBySensorIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(sensorId, from, to);
    }

    public List<SensorReading> findBySensorUntil(Long sensorId, OffsetDateTime to) {
        return repository.findAllBySensorIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(sensorId, to);
    }

    public boolean existsBySensorId(Long sensorId) {
        return repository.existsBySensorId(sensorId);
    }

    public boolean existsBySensorAt(Long sensorId, OffsetDateTime measuredAt) {
        return repository.existsBySensorIdAndMeasuredAt(sensorId, measuredAt);
    }
}
