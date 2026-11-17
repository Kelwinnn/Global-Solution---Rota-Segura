package br.com.rotasegura.controller;

import br.com.rotasegura.bo.AssistedPersonBO;
import br.com.rotasegura.dto.AssistedPersonRequest;
import br.com.rotasegura.dto.AssistedPersonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Dados pessoais de gente vulnerável: TODAS as rotas daqui exigem login, inclusive GET (decidido em 16/11). */
@RestController
@RequestMapping("/api/assisted-people")
@Tag(name = "Pessoas assistidas", description = "Cadastro das pessoas assistidas e dos responsáveis delas (RF08). "
        + "Todas as rotas exigem login, por privacidade.")
public class AssistedPersonController {

    private final AssistedPersonBO assistedPersonBO;

    public AssistedPersonController(AssistedPersonBO assistedPersonBO) {
        this.assistedPersonBO = assistedPersonBO;
    }

    @GetMapping
    @Operation(operationId = "findAllAssistedPeople", summary = "Listar pessoas assistidas",
            description = "Em ordem de nome, com os responsáveis. Exige login.")
    @ApiResponse(responseCode = "200", description = "Lista de pessoas assistidas")
    @ApiResponse(responseCode = "400", description = "areaId não é um número")
    @ApiResponse(responseCode = "401", description = "Sem login")
    public List<AssistedPersonResponse> findAll(
            @Parameter(description = "Id da área, para listar só as pessoas dela")
            @RequestParam(required = false) Long areaId) {
        return assistedPersonBO.findAll(areaId);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "findAssistedPerson", summary = "Buscar pessoa assistida", description = "Exige login.")
    @ApiResponse(responseCode = "200", description = "Pessoa encontrada")
    @ApiResponse(responseCode = "401", description = "Sem login")
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
    public AssistedPersonResponse findById(@PathVariable Long id) {
        return assistedPersonBO.findById(id);
    }

    @PostMapping
    @Operation(operationId = "createAssistedPerson", summary = "Cadastrar pessoa assistida",
            description = "Junto com a lista de responsáveis (ao menos um, RN03). Exige login e token CSRF.")
    @ApiResponse(responseCode = "201", description = "Pessoa criada; o endereço dela vem no cabeçalho Location")
    @ApiResponse(responseCode = "400", description = "Dados inválidos (inclusive lista de responsáveis vazia)")
    @ApiResponse(responseCode = "404", description = "Área não encontrada")
    public ResponseEntity<AssistedPersonResponse> create(@Valid @RequestBody AssistedPersonRequest request) {
        AssistedPersonResponse created = assistedPersonBO.create(request);
        // pathSegment (e não "/{id}"): chaves vindas de fora no caminho não são tratadas como variável
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .pathSegment(String.valueOf(created.id()))
                .build()
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateAssistedPerson", summary = "Atualizar pessoa assistida",
            description = "Troca todos os dados, inclusive a lista de responsáveis inteira. Exige login e token CSRF.")
    @ApiResponse(responseCode = "200", description = "Pessoa atualizada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Pessoa ou área não encontradas")
    public AssistedPersonResponse update(@PathVariable Long id, @Valid @RequestBody AssistedPersonRequest request) {
        return assistedPersonBO.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteAssistedPerson", summary = "Excluir pessoa assistida",
            description = "Exclui também os responsáveis dela. Exige login e token CSRF.")
    @ApiResponse(responseCode = "204", description = "Pessoa excluída")
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assistedPersonBO.delete(id);
        return ResponseEntity.noContent().build();
    }
}
