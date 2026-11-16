package br.com.rotasegura.repository;

import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskType;
import br.com.rotasegura.model.Sensor;
import br.com.rotasegura.model.SensorStatus;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Roda as consultas de verdade num banco H2 em memória (tabelas criadas a partir das entidades).
// Dados de exemplo simulados (nenhum dado real).
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class SensorRepositoryTest {

    private static final BigDecimal LAT = new BigDecimal("-23.482500");
    private static final BigDecimal LNG = new BigDecimal("-46.414000");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SensorRepository repository;

    private RiskArea helena;
    private RiskArea pantanal;
    private RiskArea withoutSensors;
    private Sensor firstSensor;

    private RiskArea area(String name) {
        return entityManager.persist(new RiskArea(name, "São Paulo", null, RiskType.FLOOD,
                new BigDecimal("-23.482000"), new BigDecimal("-46.415000"), 600));
    }

    private Sensor sensor(RiskArea area, String code) {
        return entityManager.persist(new Sensor(area, code, LAT, LNG, SensorStatus.ACTIVE));
    }

    @BeforeEach
    void setUp() {
        helena = area("Várzea do Jardim Helena");
        pantanal = area("Beira-Rio do Jardim Pantanal");
        withoutSensors = area("Área sem sensor");

        // Gravados fora de ordem, para o teste provar a ordenação por código
        sensor(pantanal, "SEN-JP-001");
        sensor(helena, "SEN-JH-002");
        firstSensor = sensor(helena, "SEN-JH-001");

        entityManager.flush();
        // Limpa a memória do Hibernate: daqui em diante, tudo o que aparecer veio do banco
        entityManager.clear();
    }

    @Test
    void findAllComesSortedByCodeWithTheAreaAlreadyLoaded() {
        List<Sensor> sensors = repository.findAllByOrderByCodeAsc();

        assertThat(sensors).extracting(Sensor::getCode)
                .containsExactly("SEN-JH-001", "SEN-JH-002", "SEN-JP-001");
        // @EntityGraph: a área já veio carregada, não é um proxy LAZY por carregar
        assertThat(sensors).allSatisfy(sensor -> assertThat(Hibernate.isInitialized(sensor.getArea())).isTrue());
    }

    @Test
    void findByAreaListsOnlyThatAreaWithTheAreaAlreadyLoaded() {
        List<Sensor> sensors = repository.findAllByAreaIdOrderByCodeAsc(helena.getId());

        assertThat(sensors).extracting(Sensor::getCode).containsExactly("SEN-JH-001", "SEN-JH-002");
        assertThat(sensors).allSatisfy(sensor -> {
            assertThat(Hibernate.isInitialized(sensor.getArea())).isTrue();
            assertThat(sensor.getArea().getName()).isEqualTo("Várzea do Jardim Helena");
        });
    }

    @Test
    void findByUnknownAreaGivesAnEmptyList() {
        assertThat(repository.findAllByAreaIdOrderByCodeAsc(999_999L)).isEmpty();
    }

    @Test
    void findByIdLeavesTheAreaLazy() {
        Sensor sensor = repository.findById(firstSensor.getId()).orElseThrow();

        // Sem @EntityGraph, a área só é buscada quando for usada
        assertThat(Hibernate.isInitialized(sensor.getArea())).isFalse();
        assertThat(sensor.getArea().getName()).isEqualTo("Várzea do Jardim Helena");
    }

    @Test
    void existsByCodeIgnoresCase() {
        assertThat(repository.existsByCodeIgnoreCase("sen-jh-001")).isTrue();
        assertThat(repository.existsByCodeIgnoreCase("SEN-XX-999")).isFalse();
    }

    @Test
    void existsByCodeExcludingIdIgnoresTheSensorItself() {
        assertThat(repository.existsByCodeIgnoreCaseAndIdNot("SEN-JH-001", firstSensor.getId())).isFalse();
        assertThat(repository.existsByCodeIgnoreCaseAndIdNot("sen-jh-002", firstSensor.getId())).isTrue();
    }

    @Test
    void existsByAreaTellsWhetherTheAreaHasSensors() {
        assertThat(repository.existsByAreaId(pantanal.getId())).isTrue();
        assertThat(repository.existsByAreaId(withoutSensors.getId())).isFalse();
    }
}
