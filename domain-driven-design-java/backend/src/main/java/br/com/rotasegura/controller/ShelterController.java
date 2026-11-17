package br.com.rotasegura.controller;

import br.com.rotasegura.bo.ShelterBO;
import br.com.rotasegura.dto.ShelterRequest;
import br.com.rotasegura.dto.ShelterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/shelters")
@Tag(name = "Abrigos", description = "Cadastro dos abrigos, com endereço, capacidade e vagas (RF06)")
public class ShelterController {

    private final ShelterBO shelterBO;

    public ShelterController(ShelterBO shelterBO) {
        this.shelterBO = shelterBO;
    }

    @GetMapping
    @Operation(operationId = "findAllShelters", summary = "Listar abrigos",
            description = "Todos os abrigos, em ordem de nome, com as vagas livres. Aberto, sem login.")
    @ApiResponse(responseCode = "200", description = "Lista de abrigos")
    public List<ShelterResponse> findAll() {
        return shelterBO.findAll();
    }

    @GetMapping("/{id}")
    @Operation(operationId = "findShelter", summary = "Buscar abrigo", description = "Um abrigo pelo id. Aberto, sem login.")
    @ApiResponse(responseCode = "200", description = "Abrigo encontrado")
    @ApiResponse(responseCode = "404", description = "Abrigo não encontrado")
    public ShelterResponse findById(@PathVariable Long id) {
        return shelterBO.findById(id);
    }

    @PostMapping
    @Operation(operationId = "createShelter", summary = "Cadastrar abrigo", description = "Exige login e token CSRF.")
    @ApiResponse(responseCode = "201", description = "Abrigo criado; o endereço dele vem no cabeçalho Location")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "409", description = "Vagas ocupadas passam da capacidade")
    public ResponseEntity<ShelterResponse> create(@Valid @RequestBody ShelterRequest request) {
        ShelterResponse created = shelterBO.create(request);
        // pathSegment (e não "/{id}"): chaves vindas de fora no caminho não são tratadas como variável
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .pathSegment(String.valueOf(created.id()))
                .build()
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateShelter", summary = "Atualizar abrigo",
            description = "Troca todos os dados, inclusive as vagas ocupadas (é assim que o painel atualiza a ocupação). "
                    + "Exige login e token CSRF.")
    @ApiResponse(responseCode = "200", description = "Abrigo atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Abrigo não encontrado")
    @ApiResponse(responseCode = "409", description = "Vagas ocupadas passam da capacidade")
    public ShelterResponse update(@PathVariable Long id, @Valid @RequestBody ShelterRequest request) {
        return shelterBO.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteShelter", summary = "Excluir abrigo", description = "Exige login e token CSRF.")
    @ApiResponse(responseCode = "204", description = "Abrigo excluído")
    @ApiResponse(responseCode = "404", description = "Abrigo não encontrado")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shelterBO.delete(id);
        return ResponseEntity.noContent().build();
    }
}
