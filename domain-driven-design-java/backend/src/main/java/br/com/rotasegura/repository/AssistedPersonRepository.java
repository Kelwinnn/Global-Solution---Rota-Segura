package br.com.rotasegura.repository;

import br.com.rotasegura.model.AssistedPerson;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssistedPersonRepository extends JpaRepository<AssistedPerson, Long> {

    /** Traz a área e os responsáveis juntos, na mesma consulta. */
    @EntityGraph(attributePaths = {"area", "guardians"})
    List<AssistedPerson> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"area", "guardians"})
    List<AssistedPerson> findAllByAreaIdOrderByNameAsc(Long areaId);

    @EntityGraph(attributePaths = {"area", "guardians"})
    Optional<AssistedPerson> findWithDetailsById(Long id);

    boolean existsByAreaId(Long areaId);
}
