package br.com.rotasegura.repository;

import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Roda as consultas de verdade num banco H2 em memória (tabelas criadas a partir das entidades).
// Dados de exemplo simulados (nenhum dado real).
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class RiskAreaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RiskAreaRepository repository;

    private RiskArea varzea;

    private static RiskArea area(String name, String city) {
        return new RiskArea(name, city, null, RiskType.FLOOD,
                new BigDecimal("-23.482000"), new BigDecimal("-46.415000"), 600);
    }

    @BeforeEach
    void setUp() {
        varzea = entityManager.persist(area("Várzea Norte", "São Paulo"));
        entityManager.persist(area("Baixada Sul", "Guarulhos"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void existsByNameAndCityIgnoresCase() {
        assertThat(repository.existsByNameIgnoreCaseAndCityIgnoreCase("VÁRZEA NORTE", "são paulo")).isTrue();
    }

    @Test
    void sameNameInAnotherCityDoesNotCount() {
        assertThat(repository.existsByNameIgnoreCaseAndCityIgnoreCase("Várzea Norte", "Guarulhos")).isFalse();
    }

    @Test
    void existsExcludingIdIgnoresTheAreaItself() {
        assertThat(repository.existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(
                "Várzea Norte", "São Paulo", varzea.getId())).isFalse();
    }

    @Test
    void existsExcludingIdFindsAnotherAreaWithTheSameName() {
        RiskArea other = entityManager.persist(area("Várzea Leste", "São Paulo"));
        entityManager.flush();

        assertThat(repository.existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(
                "várzea norte", "São Paulo", other.getId())).isTrue();
    }

    @Test
    void findAllSortedByNameUsesTheEntityFieldName() {
        assertThat(repository.findAll(Sort.by("name")))
                .extracting(RiskArea::getName)
                .containsExactly("Baixada Sul", "Várzea Norte");
    }
}
