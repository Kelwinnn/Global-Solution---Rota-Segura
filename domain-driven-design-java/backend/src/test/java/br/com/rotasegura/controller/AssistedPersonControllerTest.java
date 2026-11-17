package br.com.rotasegura.controller;

import br.com.rotasegura.bo.AssistedPersonBO;
import br.com.rotasegura.config.CorsConfig;
import br.com.rotasegura.dto.AssistedPersonRequest;
import br.com.rotasegura.dto.AssistedPersonResponse;
import br.com.rotasegura.dto.GuardianResponse;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.SpecialNeed;
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

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Dados fictícios: nomes inventados e telefones com DDD 00 (não existe)
@WebMvcTest(AssistedPersonController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class AssistedPersonControllerTest {

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("app.security.admin.username", () -> "user-" + UUID.randomUUID());
        registry.add("app.security.admin.password", () -> UUID.randomUUID().toString());
    }

    private static final String VALID_BODY = """
            {"name": "Benedita Exemplo", "phone": "(00) 00000-0001", "areaId": 1,
             "latitude": -23.4805, "longitude": -46.4138, "specialNeed": "BEDRIDDEN", "notes": "Acamada",
             "guardians": [{"name": "Ana Exemplo", "phone": "(00) 00000-0002", "relationship": "filha"}]}
            """;

    private static final AssistedPersonResponse PERSON = new AssistedPersonResponse(1L, "Benedita Exemplo",
            "(00) 00000-0001", 1L, "Várzea do Jardim Helena", new BigDecimal("-23.480500"), new BigDecimal("-46.413800"),
            SpecialNeed.BEDRIDDEN, "Acamada", List.of(new GuardianResponse(5L, "Ana Exemplo", "(00) 00000-0002", "filha")));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistedPersonBO assistedPersonBO;

    // ---- Privacidade: até o GET exige login ----

    @Test
    void readingWithoutLoginReturns401() throws Exception {
        mockMvc.perform(get("/api/assisted-people"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Login necessário"));
        mockMvc.perform(get("/api/assisted-people/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(head("/api/assisted-people")).andExpect(status().isUnauthorized());
        mockMvc.perform(head("/api/assisted-people/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(assistedPersonBO);
    }

    @Test
    @WithMockUser
    void listWithLoginReturnsPeopleWithGuardians() throws Exception {
        when(assistedPersonBO.findAll(null)).thenReturn(List.of(PERSON));

        mockMvc.perform(get("/api/assisted-people"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].areaName").value("Várzea do Jardim Helena"))
                .andExpect(jsonPath("$[0].specialNeed").value("BEDRIDDEN"))
                .andExpect(jsonPath("$[0].guardians[0].relationship").value("filha"));
    }

    @Test
    @WithMockUser
    void listPassesTheAreaFilter() throws Exception {
        when(assistedPersonBO.findAll(1L)).thenReturn(List.of(PERSON));

        mockMvc.perform(get("/api/assisted-people").param("areaId", "1")).andExpect(status().isOk());
        verify(assistedPersonBO).findAll(1L);
    }

    @Test
    @WithMockUser
    void getByIdNotFoundReturns404() throws Exception {
        when(assistedPersonBO.findById(99L)).thenThrow(new ResourceNotFoundException("Pessoa assistida 99 não encontrada."));

        mockMvc.perform(get("/api/assisted-people/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pessoa assistida 99 não encontrada."));
    }

    // ---- POST ----

    @Test
    @WithMockUser
    void createReturns201WithLocation() throws Exception {
        when(assistedPersonBO.create(any(AssistedPersonRequest.class))).thenReturn(PERSON);

        mockMvc.perform(post("/api/assisted-people").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/assisted-people/1")))
                .andExpect(jsonPath("$.guardians[0].id").value(5));
    }

    @Test
    void createWithoutLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/assisted-people").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(assistedPersonBO);
    }

    @Test
    @WithMockUser
    void createWithoutCsrfReturns403() throws Exception {
        mockMvc.perform(post("/api/assisted-people")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(assistedPersonBO);
    }

    @Test
    @WithMockUser
    void updateAndDeleteWithoutCsrfReturn403() throws Exception {
        mockMvc.perform(put("/api/assisted-people/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/assisted-people/1")).andExpect(status().isForbidden());
        verifyNoInteractions(assistedPersonBO);
    }

    @Test
    @WithMockUser
    void createWithoutGuardiansReturns400() throws Exception {
        mockMvc.perform(post("/api/assisted-people").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replaceAll("\"guardians\": \\[.*]", "\"guardians\": []")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].message", hasItem("Informe ao menos um responsável (RN03)")));
        verifyNoInteractions(assistedPersonBO);
    }

    @Test
    @WithMockUser
    void invalidGuardianInsideTheListReturns400() throws Exception {
        mockMvc.perform(post("/api/assisted-people").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"relationship\": \"filha\"", "\"relationship\": \"\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("guardians[0].relationship")));
        verifyNoInteractions(assistedPersonBO);
    }

    @Test
    @WithMockUser
    void invalidPhoneAndUnknownNeedReturn400() throws Exception {
        mockMvc.perform(post("/api/assisted-people").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"(00) 00000-0001\"", "\"ligue já!\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("phone")));

        mockMvc.perform(post("/api/assisted-people").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("BEDRIDDEN", "TIRED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
        verifyNoInteractions(assistedPersonBO);
    }

    @Test
    @WithMockUser
    void createWithUnknownAreaReturns404() throws Exception {
        when(assistedPersonBO.create(any(AssistedPersonRequest.class)))
                .thenThrow(new ResourceNotFoundException("Área de risco 99 não encontrada."));

        mockMvc.perform(post("/api/assisted-people").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    // ---- PUT e DELETE ----

    @Test
    @WithMockUser
    void updateReturns200() throws Exception {
        when(assistedPersonBO.update(eq(1L), any(AssistedPersonRequest.class))).thenReturn(PERSON);

        mockMvc.perform(put("/api/assisted-people/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void updateAndDeleteWithoutLoginReturn401() throws Exception {
        mockMvc.perform(put("/api/assisted-people/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/assisted-people/1").with(csrf())).andExpect(status().isUnauthorized());
        verifyNoInteractions(assistedPersonBO);
    }

    @Test
    @WithMockUser
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/assisted-people/1").with(csrf())).andExpect(status().isNoContent());
        verify(assistedPersonBO).delete(1L);
    }

    @Test
    @WithMockUser
    void deleteNotFoundReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Pessoa assistida 99 não encontrada.")).when(assistedPersonBO).delete(99L);

        mockMvc.perform(delete("/api/assisted-people/99").with(csrf())).andExpect(status().isNotFound());
    }
}
