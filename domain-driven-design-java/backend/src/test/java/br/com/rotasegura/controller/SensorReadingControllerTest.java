package br.com.rotasegura.controller;

import br.com.rotasegura.bo.SensorReadingBO;
import br.com.rotasegura.config.CorsConfig;
import br.com.rotasegura.dto.SensorReadingRequest;
import br.com.rotasegura.dto.SensorReadingResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
@WebMvcTest(SensorReadingController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class SensorReadingControllerTest {

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("app.security.admin.username", () -> "user-" + UUID.randomUUID());
        registry.add("app.security.admin.password", () -> UUID.randomUUID().toString());
    }

    private static final String VALID_BODY = """
            {"sensorCode": "SEN-JH-001", "rainMm": 12.4, "waterLevelCm": 2.3,
             "measuredAt": "2026-11-15T18:00:00-03:00"}
            """;

    private static final SensorReadingResponse READING = new SensorReadingResponse(100L, 10L, "SEN-JH-001", 1L,
            new BigDecimal("12.40"), new BigDecimal("2.30"), OffsetDateTime.parse("2026-11-15T21:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorReadingBO sensorReadingBO;

    // ---- POST: aberto, sem login e sem CSRF (o sensor envia) ----

    @Test
    void sensorSendsReadingWithoutLoginAndWithoutCsrf() throws Exception {
        when(sensorReadingBO.create(any(SensorReadingRequest.class))).thenReturn(READING);

        mockMvc.perform(post("/api/sensor-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/sensor-readings/100")))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.sensorCode").value("SEN-JH-001"))
                .andExpect(jsonPath("$.rainMm").value(12.4))
                .andExpect(jsonPath("$.waterLevelCm").value(2.3))
                .andExpect(jsonPath("$.measuredAt").value("2026-11-15T21:00:00Z"));

        // O instante que chega ao BO é o mesmo que o sensor enviou (18:00 em -03:00 = 21:00 em UTC)
        ArgumentCaptor<SensorReadingRequest> sent = ArgumentCaptor.forClass(SensorReadingRequest.class);
        verify(sensorReadingBO).create(sent.capture());
        assertThat(sent.getValue().measuredAt().toInstant()).isEqualTo(Instant.parse("2026-11-15T21:00:00Z"));
        assertThat(sent.getValue().sensorCode()).isEqualTo("SEN-JH-001");
    }

    @Test
    void repeatedReadingReturns409() throws Exception {
        when(sensorReadingBO.create(any(SensorReadingRequest.class)))
                .thenThrow(new BusinessRuleException("Já existe uma leitura do sensor 'SEN-JH-001' em 2026-11-15T21:00Z."));

        mockMvc.perform(post("/api/sensor-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma leitura do sensor 'SEN-JH-001' em 2026-11-15T21:00Z."));
    }

    @Test
    void readingWithInvalidValuesReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"sensorCode": " ", "rainMm": -1, "waterLevelCm": 2.345, "measuredAt": "2999-01-01T00:00:00Z"}
                """;

        mockMvc.perform(post("/api/sensor-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[*].field",
                        contains("measuredAt", "rainMm", "sensorCode", "waterLevelCm")))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("A data e a hora da medição não podem estar no futuro"));
        verifyNoInteractions(sensorReadingBO);
    }

    @Test
    void readingWithoutFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/sensor-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        contains("measuredAt", "rainMm", "sensorCode", "waterLevelCm")));
        verifyNoInteractions(sensorReadingBO);
    }

    @Test
    void readingWithInvalidDateFormatReturns400() throws Exception {
        mockMvc.perform(post("/api/sensor-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("2026-11-15T18:00:00-03:00", "15/11/2026 18:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
        verifyNoInteractions(sensorReadingBO);
    }

    @Test
    void readingFromUnknownSensorReturns404() throws Exception {
        when(sensorReadingBO.create(any(SensorReadingRequest.class)))
                .thenThrow(new ResourceNotFoundException("Sensor 'SEN-XX-999' não encontrado."));

        mockMvc.perform(post("/api/sensor-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Sensor 'SEN-XX-999' não encontrado."));
    }

    @Test
    void readingFromInactiveSensorReturns409() throws Exception {
        when(sensorReadingBO.create(any(SensorReadingRequest.class)))
                .thenThrow(new BusinessRuleException("O sensor 'SEN-JP-002' está inativo e não recebe leituras novas."));

        mockMvc.perform(post("/api/sensor-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    // ---- GET: aberto ----

    @Test
    void historyIsOpenAndHasNoPeriodByDefault() throws Exception {
        when(sensorReadingBO.findBySensor(10L, null, null)).thenReturn(List.of(READING));

        mockMvc.perform(get("/api/sensor-readings").param("sensorId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
        verify(sensorReadingBO).findBySensor(eq(10L), isNull(), isNull());
    }

    @Test
    void historyReadsThePeriodWithTimeZone() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-11-15T00:00:00-03:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-11-15T23:59:59-03:00");
        when(sensorReadingBO.findBySensor(10L, from, to)).thenReturn(List.of(READING));

        mockMvc.perform(get("/api/sensor-readings")
                        .param("sensorId", "10")
                        .param("from", "2026-11-15T00:00:00-03:00")
                        .param("to", "2026-11-15T23:59:59-03:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensorCode").value("SEN-JH-001"));
        verify(sensorReadingBO).findBySensor(10L, from, to);
    }

    @Test
    void historyWithoutSensorIdReturns400() throws Exception {
        mockMvc.perform(get("/api/sensor-readings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro obrigatório ausente"))
                .andExpect(jsonPath("$.detail").value("Informe o parâmetro sensorId."));
        verifyNoInteractions(sensorReadingBO);
    }

    @Test
    void historyWithInvalidDateReturns400() throws Exception {
        mockMvc.perform(get("/api/sensor-readings").param("sensorId", "10").param("from", "ontem"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
        verifyNoInteractions(sensorReadingBO);
    }

    @Test
    void getByIdReturns200() throws Exception {
        when(sensorReadingBO.findById(100L)).thenReturn(READING);

        mockMvc.perform(get("/api/sensor-readings/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorId").value(10));
    }

    @Test
    void getByIdNotFoundReturns404() throws Exception {
        when(sensorReadingBO.findById(999L)).thenThrow(new ResourceNotFoundException("Leitura 999 não encontrada."));

        mockMvc.perform(get("/api/sensor-readings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Leitura 999 não encontrada."));
    }

    // ---- Leitura é histórico: não se altera nem se exclui ----

    @Test
    void changingOrDeletingReadingsNeedsLogin() throws Exception {
        mockMvc.perform(put("/api/sensor-readings/100").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/sensor-readings/100").with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sensorReadingBO);
    }

    @Test
    @WithMockUser
    void changingOrDeletingReadingsIsNotAllowedEvenWithLogin() throws Exception {
        mockMvc.perform(put("/api/sensor-readings/100").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Método não permitido"));
        mockMvc.perform(delete("/api/sensor-readings/100").with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(sensorReadingBO);
    }
}
