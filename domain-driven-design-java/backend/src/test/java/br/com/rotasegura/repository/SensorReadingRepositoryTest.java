package br.com.rotasegura.repository;

import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskType;
import br.com.rotasegura.model.Sensor;
import br.com.rotasegura.model.SensorReading;
import br.com.rotasegura.model.SensorStatus;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Roda as consultas de verdade num banco H2 em memória (tabelas criadas a partir das entidades).
// Dados de exemplo simulados (nenhum dado real).
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class SensorReadingRepositoryTest {

    private static final OffsetDateTime T18 = OffsetDateTime.parse("2026-11-15T18:00:00-03:00");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SensorReadingRepository repository;

    private Sensor helena;
    private Sensor pantanal;
    private Sensor withoutReadings;

    private Sensor sensor(RiskArea area, String code) {
        return entityManager.persist(new Sensor(area, code,
                new BigDecimal("-23.481500"), new BigDecimal("-46.414200"), SensorStatus.ACTIVE));
    }

    private void reading(Sensor sensor, String water, OffsetDateTime measuredAt) {
        entityManager.persist(new SensorReading(sensor, new BigDecimal("10.00"), new BigDecimal(water), measuredAt));
    }

    @BeforeEach
    void setUp() {
        RiskArea area = entityManager.persist(new RiskArea("Várzea do Jardim Helena", "São Paulo", null,
                RiskType.FLOOD, new BigDecimal("-23.482000"), new BigDecimal("-46.415000"), 600));
        helena = sensor(area, "SEN-JH-001");
        pantanal = sensor(area, "SEN-JP-001");
        withoutReadings = sensor(area, "SEN-JH-002");

        // Gravadas fora de ordem, para o teste provar a ordenação
        reading(helena, "1.00", T18);
        reading(helena, "3.00", T18.plusMinutes(20));
        reading(helena, "2.00", T18.plusMinutes(10));
        reading(helena, "0.50", T18.minusDays(1));
        reading(pantanal, "9.00", T18);

        entityManager.flush();
        // Limpa a memória do Hibernate: daqui em diante, tudo o que aparecer veio do banco
        entityManager.clear();
    }

    @Test
    void periodIsInclusiveAndNewestComeFirst() {
        List<SensorReading> readings = repository.findAllBySensorIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                helena.getId(), T18, T18.plusMinutes(20));

        assertThat(readings).extracting(SensorReading::getWaterLevelCm)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("3.00"), new BigDecimal("2.00"), new BigDecimal("1.00"));
        // @EntityGraph: o sensor já veio carregado
        assertThat(readings).allSatisfy(reading -> assertThat(Hibernate.isInitialized(reading.getSensor())).isTrue());
    }

    @Test
    void readingsOfAnotherSensorAreNotIncluded() {
        assertThat(repository.findAllBySensorIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                helena.getId(), T18.minusYears(1), T18.plusYears(1)))
                .hasSize(4)
                .allSatisfy(reading -> assertThat(reading.getSensor().getCode()).isEqualTo("SEN-JH-001"));
    }

    @Test
    void periodComparesInstantsEvenWithDifferentTimeZones() {
        // 18:10 em -03:00 é 21:10 em UTC: o período em UTC precisa achar a mesma leitura
        List<SensorReading> readings = repository.findAllBySensorIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                helena.getId(),
                OffsetDateTime.parse("2026-11-15T21:05:00Z"),
                OffsetDateTime.parse("2026-11-15T21:15:00Z"));

        assertThat(readings).singleElement()
                .extracting(SensorReading::getWaterLevelCm)
                .satisfies(water -> assertThat(water).isEqualByComparingTo("2.00"));
    }

    @Test
    void untilWithoutStartIncludesEvenVeryOldReadings() {
        reading(helena, "0.10", OffsetDateTime.parse("1900-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        List<SensorReading> readings = repository.findAllBySensorIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(
                helena.getId(), T18.plusMinutes(10));

        assertThat(readings).extracting(SensorReading::getMeasuredAt)
                .extracting(OffsetDateTime::toInstant)
                .containsExactly(T18.plusMinutes(10).toInstant(), T18.toInstant(), T18.minusDays(1).toInstant(),
                        OffsetDateTime.parse("1900-01-01T00:00:00Z").toInstant());
    }

    @Test
    void existsBySensorAtComparesTheInstant() {
        // Mesmo instante escrito em outro fuso: conta como a mesma leitura
        assertThat(repository.existsBySensorIdAndMeasuredAt(helena.getId(), OffsetDateTime.parse("2026-11-15T21:00:00Z")))
                .isTrue();
        assertThat(repository.existsBySensorIdAndMeasuredAt(helena.getId(), T18.plusSeconds(1))).isFalse();
        assertThat(repository.existsBySensorIdAndMeasuredAt(pantanal.getId(), T18.plusMinutes(10))).isFalse();
    }

    @Test
    void existsBySensorTellsWhetherTheSensorHasReadings() {
        assertThat(repository.existsBySensorId(helena.getId())).isTrue();
        assertThat(repository.existsBySensorId(withoutReadings.getId())).isFalse();
    }
}
