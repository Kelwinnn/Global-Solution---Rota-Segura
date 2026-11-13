package br.com.rotasegura.dao;

import br.com.rotasegura.model.Sensor;
import br.com.rotasegura.repository.SensorRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Acesso a dados dos sensores. Sem regra de negócio. */
@Component
public class SensorDAO {

    private final SensorRepository repository;

    public SensorDAO(SensorRepository repository) {
        this.repository = repository;
    }

    public Sensor save(Sensor sensor) {
        return repository.save(sensor);
    }

    public Optional<Sensor> findById(Long id) {
        return repository.findById(id);
    }

    public List<Sensor> findAllOrderByCode() {
        return repository.findAllByOrderByCodeAsc();
    }

    public List<Sensor> findByAreaIdOrderByCode(Long areaId) {
        return repository.findAllByAreaIdOrderByCodeAsc(areaId);
    }

    public void delete(Sensor sensor) {
        repository.delete(sensor);
    }

    public boolean existsByCode(String code) {
        return repository.existsByCodeIgnoreCase(code);
    }

    public boolean existsByCodeExcludingId(String code, Long id) {
        return repository.existsByCodeIgnoreCaseAndIdNot(code, id);
    }

    public boolean existsByAreaId(Long areaId) {
        return repository.existsByAreaId(areaId);
    }
}
