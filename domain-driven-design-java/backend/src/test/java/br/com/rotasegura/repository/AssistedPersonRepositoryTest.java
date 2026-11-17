package br.com.rotasegura.repository;

import br.com.rotasegura.model.AssistedPerson;
import br.com.rotasegura.model.Guardian;
import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskType;
import br.com.rotasegura.model.SpecialNeed;
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
// Dados fictícios: nomes inventados e telefones com DDD 00 (não existe).
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class AssistedPersonRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AssistedPersonRepository repository;

    private RiskArea helena;
    private RiskArea empty;
    private AssistedPerson benedita;

    private RiskArea area(String name) {
        return entityManager.persist(new RiskArea(name, "São Paulo", null, RiskType.FLOOD,
                new BigDecimal("-23.482000"), new BigDecimal("-46.415000"), 550));
    }

    private AssistedPerson person(RiskArea area, String name, String... guardianNames) {
        AssistedPerson person = new AssistedPerson(area, name, "(00) 00000-0001",
                new BigDecimal("-23.480500"), new BigDecimal("-46.413800"), SpecialNeed.ELDERLY, null);
        person.replaceGuardians(java.util.Arrays.stream(guardianNames)
                .map(guardianName -> new Guardian(guardianName, "(00) 00000-0002", "filha")).toList());
        return entityManager.persist(person);
    }

    @BeforeEach
    void setUp() {
        helena = area("Várzea do Jardim Helena");
        RiskArea other = area("Baixada de São Miguel");
        empty = area("Área sem assistidos");
        person(other, "Zuleica Exemplo", "Pedro");
        benedita = person(helena, "Benedita Exemplo", "Ana", "Bruno");
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void savingThePersonAlsoSavesTheGuardians() {
        Long guardianCount = entityManager.getEntityManager()
                .createQuery("select count(g) from Guardian g", Long.class).getSingleResult();
        assertThat(guardianCount).isEqualTo(3);
    }

    @Test
    void findAllComesSortedByNameWithAreaAndGuardiansLoaded() {
        List<AssistedPerson> people = repository.findAllByOrderByNameAsc();

        assertThat(people).extracting(AssistedPerson::getName).containsExactly("Benedita Exemplo", "Zuleica Exemplo");
        assertThat(people).allSatisfy(person -> {
            assertThat(Hibernate.isInitialized(person.getArea())).isTrue();
            assertThat(Hibernate.isInitialized(person.getGuardians())).isTrue();
        });
        assertThat(people.get(0).getGuardians()).extracting(Guardian::getName).containsExactly("Ana", "Bruno");
    }

    @Test
    void findByAreaListsOnlyThatArea() {
        assertThat(repository.findAllByAreaIdOrderByNameAsc(helena.getId()))
                .extracting(AssistedPerson::getName).containsExactly("Benedita Exemplo");
        assertThat(repository.findAllByAreaIdOrderByNameAsc(empty.getId())).isEmpty();
    }

    @Test
    void findWithDetailsByIdLoadsAreaAndGuardians() {
        AssistedPerson person = repository.findWithDetailsById(benedita.getId()).orElseThrow();

        assertThat(Hibernate.isInitialized(person.getArea())).isTrue();
        assertThat(Hibernate.isInitialized(person.getGuardians())).isTrue();
        assertThat(person.getGuardians()).hasSize(2);
    }

    @Test
    void replacingTheListDeletesTheGuardiansThatLeft() {
        AssistedPerson person = repository.findWithDetailsById(benedita.getId()).orElseThrow();
        person.replaceGuardians(List.of(new Guardian("Carla", "(00) 00000-0003", "vizinha")));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findWithDetailsById(benedita.getId()).orElseThrow().getGuardians())
                .extracting(Guardian::getName).containsExactly("Carla");
        Long guardianCount = entityManager.getEntityManager()
                .createQuery("select count(g) from Guardian g", Long.class).getSingleResult();
        assertThat(guardianCount).isEqualTo(2);
    }

    @Test
    void deletingThePersonDeletesTheGuardians() {
        repository.delete(repository.findWithDetailsById(benedita.getId()).orElseThrow());
        entityManager.flush();

        Long guardianCount = entityManager.getEntityManager()
                .createQuery("select count(g) from Guardian g", Long.class).getSingleResult();
        assertThat(guardianCount).isEqualTo(1);
    }

    @Test
    void existsByAreaTellsWhetherTheAreaHasPeople() {
        assertThat(repository.existsByAreaId(helena.getId())).isTrue();
        assertThat(repository.existsByAreaId(empty.getId())).isFalse();
    }
}
