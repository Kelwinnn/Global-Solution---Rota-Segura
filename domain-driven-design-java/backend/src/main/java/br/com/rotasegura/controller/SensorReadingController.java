package br.com.rotasegura.controller;

import br.com.rotasegura.bo.SensorReadingBO;
import br.com.rotasegura.dto.SensorReadingRequest;
import br.com.rotasegura.dto.SensorReadingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

/** Leituras são histórico: só envio e consulta (sem PUT nem DELETE). */
@RestController
@RequestMapping("/api/sensor-readings")
@Tag(name = "Leituras dos sensores", description = "Chuva e nível d'água medidos pelos sensores (RF03)")
public class SensorReadingController {

    private final SensorReadingBO sensorReadingBO;

    public SensorReadingController(SensorReadingBO sensorReadingBO) {
        this.sensorReadingBO = sensorReadingBO;
    }

    @PostMapping
    @Operation(summary = "Enviar leitura",
            description = "Usado pelo aparelho do sensor. Aberto, sem login e sem token CSRF (decidido em 09/11).")
    @ApiResponse(responseCode = "201", description = "Leitura registrada; o endereço dela vem no cabeçalho Location")
    @ApiResponse(responseCode = "400", description = "Dados inválidos (inclusive data no futuro)")
    @ApiResponse(responseCode = "404", description = "Nenhum sensor com esse código")
    @ApiResponse(responseCode = "409", description = "Sensor inativo, ou já existe leitura desse sensor nesse instante")
    public ResponseEntity<SensorReadingResponse> create(@Valid @RequestBody SensorReadingRequest request) {
        SensorReadingResponse created = sensorReadingBO.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .pathSegment(String.valueOf(created.id()))
                .build()
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Histórico de leituras de um sensor",
            description = "Mais recentes primeiro. Aberto, sem login. Datas em ISO 8601 com fuso; "
                    + "na URL, o sinal + do fuso precisa ser escrito como %2B (ex.: 2026-11-19T00:00:00Z ou -03:00).")
    @ApiResponse(responseCode = "200", description = "Lista de leituras (vazia se o sensor não existir ou não tiver leituras no período)")
    @ApiResponse(responseCode = "400", description = "sensorId ausente, ou parâmetro em formato inválido")
    public List<SensorReadingResponse> findBySensor(
            @Parameter(description = "Id do sensor") @RequestParam Long sensorId,
            @Parameter(description = "Início do período (inclusive). Sem ele, desde a primeira leitura")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @Parameter(description = "Fim do período (inclusive). Sem ele, até agora")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return sensorReadingBO.findBySensor(sensorId, from, to);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar leitura", description = "Uma leitura pelo id. Aberto, sem login.")
    @ApiResponse(responseCode = "200", description = "Leitura encontrada")
    @ApiResponse(responseCode = "404", description = "Leitura não encontrada")
    public SensorReadingResponse findById(@PathVariable Long id) {
        return sensorReadingBO.findById(id);
    }
}
