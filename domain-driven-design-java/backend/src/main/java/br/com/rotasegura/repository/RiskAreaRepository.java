package br.com.rotasegura.repository;

import br.com.rotasegura.model.RiskArea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAreaRepository extends JpaRepository<RiskArea, Long> {

    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    boolean existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(String name, String city, Long id);
}
