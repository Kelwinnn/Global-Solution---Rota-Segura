package br.com.rotasegura.dao;

import br.com.rotasegura.model.Shelter;
import br.com.rotasegura.repository.ShelterRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Acesso a dados dos abrigos. Sem regra de negócio. */
@Component
public class ShelterDAO {

    private final ShelterRepository repository;

    public ShelterDAO(ShelterRepository repository) {
        this.repository = repository;
    }

    public Shelter save(Shelter shelter) {
        return repository.save(shelter);
    }

    public Optional<Shelter> findById(Long id) {
        return repository.findById(id);
    }

    public List<Shelter> findAllOrderByName() {
        return repository.findAllByOrderByNameAsc();
    }

    public void delete(Shelter shelter) {
        repository.delete(shelter);
    }
}
