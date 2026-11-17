package br.com.rotasegura.controller;

import br.com.rotasegura.bo.ShelterBO;
import br.com.rotasegura.config.CorsConfig;
import br.com.rotasegura.dto.ShelterRequest;
import br.com.rotasegura.dto.ShelterResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
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
@WebMvcTest(ShelterController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class ShelterControllerTest {

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("app.security.admin.username", () -> "user-" + UUID.randomUUID());
        registry.add("app.security.admin.password", () -> UUID.randomUUID().toString());
    }

    private static final String VALID_BODY = """
            {"name": "CEU Exemplo", "address": "Rua Exemplo, 100", "latitude": -23.478226,
             "longitude": -46.381098, "capacity": 400, "occupiedSpots": 150}
            """;

    private static final ShelterResponse SHELTER = new ShelterResponse(1L, "CEU Exemplo", "Rua Exemplo, 100",
            new BigDecimal("-23.478226"), new BigDecimal("-46.381098"), 400, 150, 250);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShelterBO shelterBO;

    // ---- GET: aberto ----

    @Test
    void listIsOpenWithoutLoginAndShowsAvailableSpots() throws Exception {
        when(shelterBO.findAll()).thenReturn(List.of(SHELTER));

        mockMvc.perform(get("/api/shelters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].capacity").value(400))
                .andExpect(jsonPath("$[0].occupiedSpots").value(150))
                .andExpect(jsonPath("$[0].availableSpots").value(250));
    }

    @Test
    void getByIdReturns200() throws Exception {
        when(shelterBO.findById(1L)).thenReturn(SHELTER);

        mockMvc.perform(get("/api/shelters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Rua Exemplo, 100"));
    }

    @Test
    void getByIdNotFoundReturns404() throws Exception {
        when(shelterBO.findById(99L)).thenThrow(new ResourceNotFoundException("Abrigo 99 não encontrado."));

        mockMvc.perform(get("/api/shelters/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Abrigo 99 não encontrado."));
    }

    // ---- POST ----

    @Test
    @WithMockUser
    void createReturns201WithLocation() throws Exception {
        when(shelterBO.create(any(ShelterRequest.class))).thenReturn(SHELTER);

        mockMvc.perform(post("/api/shelters").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/shelters/1")));
    }

    @Test
    void createWithoutLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/shelters").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Login necessário"));
        verifyNoInteractions(shelterBO);
    }

    @Test
    @WithMockUser
    void createWithoutCsrfReturns403() throws Exception {
        mockMvc.perform(post("/api/shelters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(shelterBO);
    }

    @Test
    @WithMockUser
    void createWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"name": "", "address": " ", "latitude": -23.478226, "longitude": -46.381098,
                 "capacity": 0, "occupiedSpots": -1}
                """;

        mockMvc.perform(post("/api/shelters").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", contains("address", "capacity", "name", "occupiedSpots")));
        verifyNoInteractions(shelterBO);
    }

    @Test
    @WithMockUser
    void createWithMoreOccupiedThanCapacityReturns409() throws Exception {
        when(shelterBO.create(any(ShelterRequest.class)))
                .thenThrow(new BusinessRuleException("As vagas ocupadas (500) passam da capacidade do abrigo (400)."));

        mockMvc.perform(post("/api/shelters").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    // ---- PUT ----

    @Test
    @WithMockUser
    void updateReturns200() throws Exception {
        when(shelterBO.update(eq(1L), any(ShelterRequest.class))).thenReturn(SHELTER);

        mockMvc.perform(put("/api/shelters/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSpots").value(250));
    }

    @Test
    void updateWithoutLoginReturns401() throws Exception {
        mockMvc.perform(put("/api/shelters/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(shelterBO);
    }

    @Test
    @WithMockUser
    void updateNotFoundReturns404() throws Exception {
        when(shelterBO.update(eq(99L), any(ShelterRequest.class)))
                .thenThrow(new ResourceNotFoundException("Abrigo 99 não encontrado."));

        mockMvc.perform(put("/api/shelters/99").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE ----

    @Test
    @WithMockUser
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/shelters/1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(shelterBO).delete(1L);
    }

    @Test
    void deleteWithoutLoginReturns401() throws Exception {
        mockMvc.perform(delete("/api/shelters/1").with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(shelterBO);
    }

    @Test
    @WithMockUser
    void deleteNotFoundReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Abrigo 99 não encontrado.")).when(shelterBO).delete(99L);

        mockMvc.perform(delete("/api/shelters/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
