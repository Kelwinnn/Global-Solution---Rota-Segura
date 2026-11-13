package br.com.rotasegura.controller;

import br.com.rotasegura.bo.SensorBO;
import br.com.rotasegura.dto.SensorRequest;
import br.com.rotasegura.dto.SensorResponse;
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

@RestController
@RequestMapping("/api/sensors")
@Tag(name = "Sensores", description = "Cadastro dos sensores de rua, cada um ligado a uma área de risco (RF02)")
public class SensorController {

    private final SensorBO sensorBO;

    public SensorController(SensorBO sensorBO) {
        this.sensorBO = sensorBO;
    }

    @GetMapping
    @Operation(summary = "Listar sensores",
            description = "Todos os sensores, em ordem de código, ou só os de uma área. Aberto, sem login.")
    @ApiResponse(responseCode = "200", description = "Lista de sensores (vazia se a área não tiver sensores ou não existir)")
    @ApiResponse(responseCode = "400", description = "areaId não é um número")
    public List<SensorResponse> findAll(
            @Parameter(description = "Id da área, para listar só os sensores dela")
            @RequestParam(required = false) Long areaId) {
        return sensorBO.findAll(areaId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar sensor", description = "Um sensor pelo id. Aberto, sem login.")
    @ApiResponse(responseCode = "200", description = "Sensor encontrado")
    @ApiResponse(responseCode = "404", description = "Sensor não encontrado")
    public SensorResponse findById(@PathVariable Long id) {
        return sensorBO.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar sensor", description = "Liga o sensor a uma área que já existe. Exige login e token CSRF.")
    @ApiResponse(responseCode = "201", description = "Sensor criado; o endereço dele vem no cabeçalho Location")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Área não encontrada")
    @ApiResponse(responseCode = "409", description = "Já existe sensor com esse código")
    public ResponseEntity<SensorResponse> create(@Valid @RequestBody SensorRequest request) {
        SensorResponse created = sensorBO.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .pathSegment(String.valueOf(created.id()))
                .build()
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar sensor",
            description = "Troca todos os dados do sensor, inclusive a área. Exige login e token CSRF.")
    @ApiResponse(responseCode = "200", description = "Sensor atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Sensor ou área não encontrados")
    @ApiResponse(responseCode = "409", description = "Já existe sensor com esse código")
    public SensorResponse update(@PathVariable Long id, @Valid @RequestBody SensorRequest request) {
        return sensorBO.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir sensor", description = "Exige login e token CSRF.")
    @ApiResponse(responseCode = "204", description = "Sensor excluído")
    @ApiResponse(responseCode = "404", description = "Sensor não encontrado")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sensorBO.delete(id);
        return ResponseEntity.noContent().build();
    }
}
