package br.com.rotasegura.controller;

import br.com.rotasegura.bo.RiskAreaBO;
import br.com.rotasegura.bo.RiskLevelBO;
import br.com.rotasegura.config.CorsConfig;
import br.com.rotasegura.dto.AreaRiskResponse;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.RiskLevel;
import br.com.rotasegura.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Dados de exemplo simulados (nenhum dado real)
// Os dois controllers de /api/areas juntos, para provar que /risk não é lido como /{id}
@WebMvcTest({AreaRiskController.class, RiskAreaController.class})
@Import({SecurityConfig.class, CorsConfig.class})
class AreaRiskControllerTest {

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("app.security.admin.username", () -> "user-" + UUID.randomUUID());
        registry.add("app.security.admin.password", () -> UUID.randomUUID().toString());
    }

    private static final AreaRiskResponse HELENA = new AreaRiskResponse(1L, "Várzea do Jardim Helena", true,
            RiskLevel.HIGH, new BigDecimal("4.60"), "SEN-JH-001", OffsetDateTime.parse("2026-11-15T21:30:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskLevelBO riskLevelBO;

    @MockitoBean
    private RiskAreaBO riskAreaBO;

    @Test
    void allAreasRiskIsOpenWithoutLogin() throws Exception {
        when(riskLevelBO.findAll()).thenReturn(List.of(
                HELENA, AreaRiskResponse.noData(2L, "Morro de Poá")));

        mockMvc.perform(get("/api/areas/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].areaId").value(1))
                .andExpect(jsonPath("$[0].hasData").value(true))
                .andExpect(jsonPath("$[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].waterLevelCm").value(4.6))
                .andExpect(jsonPath("$[0].sensorCode").value("SEN-JH-001"))
                .andExpect(jsonPath("$[1].hasData").value(false))
                // O campo existe e vem null (o front testa hasData, não a ausência do campo)
                .andExpect(jsonPath("$[1].riskLevel").value(nullValue()))
                .andExpect(jsonPath("$[1].waterLevelCm").value(nullValue()));
        // A rota /risk não caiu no GET /api/areas/{id}
        verifyNoInteractions(riskAreaBO);
    }

    @Test
    void oneAreaRiskIsOpenWithoutLogin() throws Exception {
        when(riskLevelBO.findByArea(1L)).thenReturn(HELENA);

        mockMvc.perform(get("/api/areas/1/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.measuredAt").value("2026-11-15T21:30:00Z"));
    }

    @Test
    void unknownAreaReturns404() throws Exception {
        when(riskLevelBO.findByArea(99L)).thenThrow(new ResourceNotFoundException("Área de risco 99 não encontrada."));

        mockMvc.perform(get("/api/areas/99/risk"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Área de risco 99 não encontrada."));
    }

    @Test
    void textIdReturns400() throws Exception {
        mockMvc.perform(get("/api/areas/abc/risk"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
        verifyNoInteractions(riskLevelBO);
    }
}
