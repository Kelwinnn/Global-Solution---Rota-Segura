package br.com.rotasegura.repository;

import br.com.rotasegura.model.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    List<Shelter> findAllByOrderByNameAsc();
}
