package br.com.rotasegura.dao;

import br.com.rotasegura.model.AssistedPerson;
import br.com.rotasegura.repository.AssistedPersonRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Acesso a dados das pessoas assistidas (e, em cascata, dos responsáveis). Sem regra de negócio. */
@Component
public class AssistedPersonDAO {

    private final AssistedPersonRepository repository;

    public AssistedPersonDAO(AssistedPersonRepository repository) {
        this.repository = repository;
    }

    public AssistedPerson save(AssistedPerson person) {
        return repository.save(person);
    }

    /** Com a área e os responsáveis já carregados. */
    public Optional<AssistedPerson> findById(Long id) {
        return repository.findWithDetailsById(id);
    }

    public List<AssistedPerson> findAllOrderByName() {
        return repository.findAllByOrderByNameAsc();
    }

    public List<AssistedPerson> findByAreaIdOrderByName(Long areaId) {
        return repository.findAllByAreaIdOrderByNameAsc(areaId);
    }

    public void delete(AssistedPerson person) {
        repository.delete(person);
    }

    public boolean existsByAreaId(Long areaId) {
        return repository.existsByAreaId(areaId);
    }
}
