package br.com.rotasegura.repository;

import br.com.rotasegura.model.Sensor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    /** Traz a área junto, na mesma consulta (sem isto, seria uma consulta a mais por sensor). */
    @EntityGraph(attributePaths = "area")
    List<Sensor> findAllByOrderByCodeAsc();

    @EntityGraph(attributePaths = "area")
    List<Sensor> findAllByAreaIdOrderByCodeAsc(Long areaId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByAreaId(Long areaId);
}
