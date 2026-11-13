package br.com.rotasegura.controller;

import br.com.rotasegura.bo.RiskAreaBO;
import br.com.rotasegura.dto.RiskAreaRequest;
import br.com.rotasegura.dto.RiskAreaResponse;
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
@RequestMapping("/api/areas")
@Tag(name = "Áreas de risco", description = "Cadastro das áreas de risco de alagamento ou deslizamento (RF01)")
public class RiskAreaController {

    private final RiskAreaBO riskAreaBO;

    public RiskAreaController(RiskAreaBO riskAreaBO) {
        this.riskAreaBO = riskAreaBO;
    }

    @GetMapping
    @Operation(summary = "Listar áreas de risco", description = "Todas as áreas, em ordem de nome. Aberto, sem login.")
    @ApiResponse(responseCode = "200", description = "Lista de áreas")
    public List<RiskAreaResponse> findAll() {
        return riskAreaBO.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar área de risco", description = "Uma área pelo id. Aberto, sem login.")
    @ApiResponse(responseCode = "200", description = "Área encontrada")
    @ApiResponse(responseCode = "404", description = "Área não encontrada")
    public RiskAreaResponse findById(@PathVariable Long id) {
        return riskAreaBO.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar área de risco", description = "Exige login e token CSRF.")
    @ApiResponse(responseCode = "201", description = "Área criada; o endereço dela vem no cabeçalho Location")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "401", description = "Sem login")
    @ApiResponse(responseCode = "409", description = "Já existe área com esse nome na cidade")
    public ResponseEntity<RiskAreaResponse> create(@Valid @RequestBody RiskAreaRequest request) {
        RiskAreaResponse created = riskAreaBO.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar área de risco", description = "Troca todos os dados da área. Exige login e token CSRF.")
    @ApiResponse(responseCode = "200", description = "Área atualizada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "401", description = "Sem login")
    @ApiResponse(responseCode = "404", description = "Área não encontrada")
    @ApiResponse(responseCode = "409", description = "Já existe área com esse nome na cidade")
    public RiskAreaResponse update(@PathVariable Long id, @Valid @RequestBody RiskAreaRequest request) {
        return riskAreaBO.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir área de risco", description = "Exige login e token CSRF.")
    @ApiResponse(responseCode = "204", description = "Área excluída")
    @ApiResponse(responseCode = "401", description = "Sem login")
    @ApiResponse(responseCode = "404", description = "Área não encontrada")
    @ApiResponse(responseCode = "409", description = "Área ligada a outros registros")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        riskAreaBO.delete(id);
        return ResponseEntity.noContent().build();
    }
}
