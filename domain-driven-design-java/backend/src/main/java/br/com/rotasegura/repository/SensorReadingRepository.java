package br.com.rotasegura.repository;

import br.com.rotasegura.model.SensorReading;
import br.com.rotasegura.model.SensorStatus;
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

    /** Leituras de sensores numa situação (ex.: ACTIVE) num período; traz sensor e área juntos. */
    @EntityGraph(attributePaths = {"sensor", "sensor.area"})
    List<SensorReading> findAllBySensorStatusAndMeasuredAtBetween(
            SensorStatus status, OffsetDateTime from, OffsetDateTime to);

    /** O mesmo, só para os sensores de uma área. */
    @EntityGraph(attributePaths = {"sensor", "sensor.area"})
    List<SensorReading> findAllBySensorAreaIdAndSensorStatusAndMeasuredAtBetween(
            Long areaId, SensorStatus status, OffsetDateTime from, OffsetDateTime to);

    boolean existsBySensorId(Long sensorId);

    boolean existsBySensorIdAndMeasuredAt(Long sensorId, OffsetDateTime measuredAt);
}
