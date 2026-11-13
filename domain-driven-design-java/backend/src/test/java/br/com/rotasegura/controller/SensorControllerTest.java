package br.com.rotasegura.controller;

import br.com.rotasegura.bo.SensorBO;
import br.com.rotasegura.config.CorsConfig;
import br.com.rotasegura.dto.SensorRequest;
import br.com.rotasegura.dto.SensorResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.SensorStatus;
import br.com.rotasegura.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Dados de exemplo simulados (nenhum dado real)
@WebMvcTest(SensorController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class SensorControllerTest {

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("app.security.admin.username", () -> "user-" + UUID.randomUUID());
        registry.add("app.security.admin.password", () -> UUID.randomUUID().toString());
    }

    private static final String VALID_BODY = """
            {"areaId": 1, "code": "SEN-JH-001", "latitude": -23.4825, "longitude": -46.414, "status": "ACTIVE"}
            """;

    private static final SensorResponse SENSOR = new SensorResponse(10L, "SEN-JH-001", 1L, "Várzea do Jardim Helena",
            new BigDecimal("-23.482500"), new BigDecimal("-46.414000"), SensorStatus.ACTIVE);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorBO sensorBO;

    // ---- GET: aberto ----

    @Test
    void listIsOpenWithoutLoginAndHasNoFilterByDefault() throws Exception {
        when(sensorBO.findAll(null)).thenReturn(List.of(SENSOR));

        mockMvc.perform(get("/api/sensors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].areaId").value(1))
                .andExpect(jsonPath("$[0].areaName").value("Várzea do Jardim Helena"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void listPassesTheAreaFilterToTheBO() throws Exception {
        when(sensorBO.findAll(1L)).thenReturn(List.of(SENSOR));

        mockMvc.perform(get("/api/sensors").param("areaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SEN-JH-001"));
        verify(sensorBO).findAll(1L);
    }

    @Test
    void listWithTextAreaIdReturns400() throws Exception {
        mockMvc.perform(get("/api/sensors").param("areaId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
        verifyNoInteractions(sensorBO);
    }

    @Test
    void getByIdReturns200() throws Exception {
        when(sensorBO.findById(10L)).thenReturn(SENSOR);

        mockMvc.perform(get("/api/sensors/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SEN-JH-001"));
    }

    @Test
    void getByIdNotFoundReturns404() throws Exception {
        when(sensorBO.findById(99L)).thenThrow(new ResourceNotFoundException("Sensor 99 não encontrado."));

        mockMvc.perform(get("/api/sensors/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Sensor 99 não encontrado."));
    }

    // ---- POST ----

    @Test
    @WithMockUser
    void createReturns201WithLocation() throws Exception {
        when(sensorBO.create(any(SensorRequest.class))).thenReturn(SENSOR);

        mockMvc.perform(post("/api/sensors").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/sensors/10")))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void createWithoutLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/sensors").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Login necessário"));
        verifyNoInteractions(sensorBO);
    }

    @Test
    @WithMockUser
    void createWithoutCsrfReturns403() throws Exception {
        mockMvc.perform(post("/api/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Token CSRF inválido"));
        verifyNoInteractions(sensorBO);
    }

    @Test
    @WithMockUser
    void createWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"code": " ", "latitude": 95, "longitude": -46.414}
                """;

        mockMvc.perform(post("/api/sensors").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[*].field", contains("areaId", "code", "latitude", "status")));
        verifyNoInteractions(sensorBO);
    }

    @Test
    @WithMockUser
    void createWithOutOfRangeValuesReturns400() throws Exception {
        String invalid = VALID_BODY
                .replace("\"SEN-JH-001\"", "\"" + "S".repeat(31) + "\"")
                .replace("-23.4825", "-23.4825123")
                .replace("-46.414", "181");

        mockMvc.perform(post("/api/sensors").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].message", contains(
                        "O código pode ter até 30 caracteres",
                        "A latitude aceita até 6 casas decimais",
                        "A longitude vai de -180 a 180")));
        verifyNoInteractions(sensorBO);
    }

    @Test
    @WithMockUser
    void createWithNumberInsteadOfStatusTextReturns400() throws Exception {
        // Sem fail-on-numbers-for-enums, "status": 1 viraria INACTIVE (a posição no enum)
        mockMvc.perform(post("/api/sensors").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"ACTIVE\"", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
        verifyNoInteractions(sensorBO);
    }

    @Test
    @WithMockUser
    void createWithUnknownStatusReturns400() throws Exception {
        mockMvc.perform(post("/api/sensors").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("ACTIVE", "BROKEN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
    }

    @Test
    @WithMockUser
    void createWithUnknownAreaReturns404() throws Exception {
        when(sensorBO.create(any(SensorRequest.class)))
                .thenThrow(new ResourceNotFoundException("Área de risco 99 não encontrada."));

        mockMvc.perform(post("/api/sensors").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"areaId\": 1", "\"areaId\": 99")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Área de risco 99 não encontrada."));
    }

    @Test
    @WithMockUser
    void createDuplicatedCodeReturns409() throws Exception {
        when(sensorBO.create(any(SensorRequest.class)))
                .thenThrow(new BusinessRuleException("Já existe um sensor com o código 'SEN-JH-001'."));

        mockMvc.perform(post("/api/sensors").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    // ---- PUT ----

    @Test
    @WithMockUser
    void updateReturns200() throws Exception {
        when(sensorBO.update(eq(10L), any(SensorRequest.class))).thenReturn(SENSOR);

        mockMvc.perform(put("/api/sensors/10").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void updateWithoutLoginReturns401() throws Exception {
        mockMvc.perform(put("/api/sensors/10").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sensorBO);
    }

    @Test
    @WithMockUser
    void updateAndDeleteWithoutCsrfReturn403() throws Exception {
        mockMvc.perform(put("/api/sensors/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/sensors/10"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(sensorBO);
    }

    @Test
    @WithMockUser
    void updateWithInvalidBodyReturns400() throws Exception {
        mockMvc.perform(put("/api/sensors/10").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"SEN-JH-001\"", "\"\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", contains("code")));
        verifyNoInteractions(sensorBO);
    }

    @Test
    @WithMockUser
    void updateNotFoundReturns404() throws Exception {
        when(sensorBO.update(eq(99L), any(SensorRequest.class)))
                .thenThrow(new ResourceNotFoundException("Sensor 99 não encontrado."));

        mockMvc.perform(put("/api/sensors/99").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateDuplicatedCodeReturns409() throws Exception {
        when(sensorBO.update(eq(10L), any(SensorRequest.class)))
                .thenThrow(new BusinessRuleException("Já existe um sensor com o código 'SEN-JH-002'."));

        mockMvc.perform(put("/api/sensors/10").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    // ---- DELETE ----

    @Test
    @WithMockUser
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/sensors/10").with(csrf()))
                .andExpect(status().isNoContent());
        verify(sensorBO).delete(10L);
    }

    @Test
    void deleteWithoutLoginReturns401() throws Exception {
        mockMvc.perform(delete("/api/sensors/10").with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sensorBO);
    }

    @Test
    @WithMockUser
    void deleteNotFoundReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Sensor 99 não encontrado.")).when(sensorBO).delete(99L);

        mockMvc.perform(delete("/api/sensors/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
