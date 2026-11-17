package br.com.rotasegura.controller;

import br.com.rotasegura.bo.RiskLevelBO;
import br.com.rotasegura.dto.AreaRiskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Nível de risco atual das áreas (RF04). Só leitura: o nível é calculado a partir das leituras dos sensores. */
@RestController
@RequestMapping("/api/areas")
@Tag(name = "Nível de risco", description = "Nível de risco atual de cada área, calculado pelo nível d'água (RF04)")
public class AreaRiskController {

    private final RiskLevelBO riskLevelBO;

    public AreaRiskController(RiskLevelBO riskLevelBO) {
        this.riskLevelBO = riskLevelBO;
    }

    @GetMapping("/risk")
    @Operation(operationId = "findAllAreaRisks", summary = "Nível de risco de todas as áreas",
            description = "Para o mapa (RF14). Em cada área vale a pior entre as leituras mais recentes (última 1 hora) "
                    + "dos sensores ativos. Sem leitura recente, hasData é false. Aberto, sem login.")
    @ApiResponse(responseCode = "200", description = "Uma linha por área, em ordem de nome")
    public List<AreaRiskResponse> findAll() {
        return riskLevelBO.findAll();
    }

    @GetMapping("/{id}/risk")
    @Operation(operationId = "findAreaRisk", summary = "Nível de risco de uma área",
            description = "Mesma regra da lista. Aberto, sem login.")
    @ApiResponse(responseCode = "200", description = "Nível da área (hasData false se não houver leitura recente)")
    @ApiResponse(responseCode = "400", description = "id não é um número")
    @ApiResponse(responseCode = "404", description = "Área não encontrada")
    public AreaRiskResponse findByArea(@PathVariable Long id) {
        return riskLevelBO.findByArea(id);
    }
}
