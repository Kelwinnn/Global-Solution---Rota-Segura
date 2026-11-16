package br.com.rotasegura.repository;

import br.com.rotasegura.model.SensorReading;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    /** Leituras de um sensor entre duas datas (inclusive), mais recentes primeiro; traz o sensor junto. */
    @EntityGraph(attributePaths = "sensor")
    List<SensorReading> findAllBySensorIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
            Long sensorId, OffsetDateTime from, OffsetDateTime to);

    /** Sem data inicial: tudo até "to". */
    @EntityGraph(attributePaths = "sensor")
    List<SensorReading> findAllBySensorIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(
            Long sensorId, OffsetDateTime to);

    boolean existsBySensorId(Long sensorId);

    boolean existsBySensorIdAndMeasuredAt(Long sensorId, OffsetDateTime measuredAt);
}
