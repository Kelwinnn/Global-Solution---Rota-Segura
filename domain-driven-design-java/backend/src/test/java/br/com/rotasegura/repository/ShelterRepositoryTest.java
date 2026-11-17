package br.com.rotasegura.repository;

import br.com.rotasegura.model.Shelter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

// Roda a consulta de verdade num banco H2 em memória (tabelas criadas a partir das entidades).
// Dados de exemplo simulados (nenhum dado real).
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ShelterRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShelterRepository repository;

    private void shelter(String name, int capacity, int occupied) {
        entityManager.persist(new Shelter(name, "Rua Exemplo, 100", new BigDecimal("-23.478226"),
                new BigDecimal("-46.381098"), capacity, occupied));
    }

    @Test
    void findAllComesSortedByNameWithAvailableSpots() {
        shelter("Escola B", 200, 200);
        shelter("CEU A", 400, 100);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByOrderByNameAsc())
                .extracting(Shelter::getName, Shelter::getAvailableSpots)
                .containsExactly(tuple("CEU A", 300), tuple("Escola B", 0));
    }
}
