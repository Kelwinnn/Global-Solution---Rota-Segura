package br.com.rotasegura.dao;

import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.repository.RiskAreaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Acesso a dados das áreas de risco. Sem regra de negócio. */
@Component
public class RiskAreaDAO {

    private final RiskAreaRepository repository;

    public RiskAreaDAO(RiskAreaRepository repository) {
        this.repository = repository;
    }

    public RiskArea save(RiskArea area) {
        return repository.save(area);
    }

    public Optional<RiskArea> findById(Long id) {
        return repository.findById(id);
    }

    public List<RiskArea> findAllOrderByName() {
        return repository.findAll(Sort.by("name"));
    }

    public void delete(RiskArea area) {
        repository.delete(area);
    }

    public boolean existsByNameAndCity(String name, String city) {
        return repository.existsByNameIgnoreCaseAndCityIgnoreCase(name, city);
    }

    public boolean existsByNameAndCityExcludingId(String name, String city, Long id) {
        return repository.existsByNameIgnoreCaseAndCityIgnoreCaseAndIdNot(name, city, id);
    }
}
