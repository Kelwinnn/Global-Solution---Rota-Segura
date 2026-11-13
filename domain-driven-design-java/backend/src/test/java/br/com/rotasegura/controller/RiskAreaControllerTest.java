package br.com.rotasegura.controller;

import br.com.rotasegura.bo.RiskAreaBO;
import br.com.rotasegura.config.CorsConfig;
import br.com.rotasegura.dto.RiskAreaRequest;
import br.com.rotasegura.dto.RiskAreaResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.RiskType;
import br.com.rotasegura.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Dados de exemplo simulados (nenhum dado real)
@WebMvcTest(RiskAreaController.class)
@Import({SecurityConfig.class, CorsConfig.class})
// Traz o filtro de X-Forwarded-* para conferir o Location atrás do proxy
@ImportAutoConfiguration(ServletWebServerFactoryAutoConfiguration.class)
class RiskAreaControllerTest {

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("app.security.admin.username", () -> "user-" + UUID.randomUUID());
        registry.add("app.security.admin.password", () -> UUID.randomUUID().toString());
    }

    private static final String VALID_BODY = """
            {"name": "Várzea Norte", "city": "São Paulo", "district": "Jardim Helena",
             "riskType": "FLOOD", "latitude": -23.4831, "longitude": -46.3982, "radiusM": 500}
            """;

    private static final RiskAreaResponse AREA = new RiskAreaResponse(1L, "Várzea Norte", "São Paulo",
            "Jardim Helena", RiskType.FLOOD, new BigDecimal("-23.483100"), new BigDecimal("-46.398200"), 500);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskAreaBO riskAreaBO;

    // ---- GET: aberto ----

    @Test
    void listIsOpenWithoutLogin() throws Exception {
        when(riskAreaBO.findAll()).thenReturn(List.of(AREA));

        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].riskType").value("FLOOD"));
    }

    @Test
    void getByIdReturns200() throws Exception {
        when(riskAreaBO.findById(1L)).thenReturn(AREA);

        mockMvc.perform(get("/api/areas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Várzea Norte"));
    }

    @Test
    void getByIdNotFoundReturns404ProblemDetail() throws Exception {
        when(riskAreaBO.findById(99L)).thenThrow(new ResourceNotFoundException("Área de risco 99 não encontrada."));

        mockMvc.perform(get("/api/areas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Área de risco 99 não encontrada."))
                .andExpect(jsonPath("$.instance").value("/api/areas/99"));
    }

    @Test
    void getByIdWithTextIdReturns400() throws Exception {
        mockMvc.perform(get("/api/areas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
    }

    // ---- POST ----

    @Test
    @WithMockUser
    void createReturns201WithLocation() throws Exception {
        when(riskAreaBO.create(any(RiskAreaRequest.class))).thenReturn(AREA);

        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/areas/1")))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createWithoutLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Login necessário"));
        verifyNoInteractions(riskAreaBO);
    }

    @Test
    @WithMockUser
    void createWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"name": "", "city": "São Paulo", "riskType": "FLOOD",
                 "latitude": -95, "longitude": -46.3982, "radiusM": 0}
                """;

        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("latitude")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("radiusM")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("O raio deve ser maior que zero")));
        verifyNoInteractions(riskAreaBO);
    }

    @Test
    @WithMockUser
    void createWithUnknownRiskTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("FLOOD", "TORNADO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
    }

    @Test
    @WithMockUser
    void createWithTooManyDecimalsOrTooLongNameReturns400() throws Exception {
        String invalid = VALID_BODY
                .replace("-23.4831", "-23.4831234")
                .replace("\"Várzea Norte\"", "\"" + "a".repeat(101) + "\"");

        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].message", hasItem("A latitude aceita até 6 casas decimais")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("O nome pode ter até 100 caracteres")));
    }

    @Test
    @WithMockUser
    void createWithNumberInsteadOfRiskTypeTextReturns400() throws Exception {
        // Sem fail-on-numbers-for-enums, "riskType": 1 viraria LANDSLIDE (a posição no enum)
        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"FLOOD\"", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
        verifyNoInteractions(riskAreaBO);
    }

    @Test
    @WithMockUser
    void createWithRadiusAboveIntegerLimitReturns400() throws Exception {
        // 2147483647 é o teto do Integer do Java e do CHECK do banco
        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"radiusM\": 500", "\"radiusM\": 2147483648")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
        verifyNoInteractions(riskAreaBO);
    }

    @Test
    @WithMockUser
    void createWithDecimalRadiusReturns400() throws Exception {
        // Sem accept-float-as-int=false, o Jackson cortaria 500.9 para 500 sem avisar
        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"radiusM\": 500", "\"radiusM\": 500.9")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(riskAreaBO);
    }

    @Test
    @WithMockUser
    void createDuplicatedReturns409() throws Exception {
        when(riskAreaBO.create(any(RiskAreaRequest.class)))
                .thenThrow(new BusinessRuleException("Já existe uma área de risco chamada 'Várzea Norte' em São Paulo."));

        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    // ---- PUT ----

    @Test
    @WithMockUser
    void updateReturns200() throws Exception {
        when(riskAreaBO.update(eq(1L), any(RiskAreaRequest.class))).thenReturn(AREA);

        mockMvc.perform(put("/api/areas/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void updateWithInvalidBodyReturns400() throws Exception {
        mockMvc.perform(put("/api/areas/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"São Paulo\"", "\"\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("city")));
        verifyNoInteractions(riskAreaBO);
    }

    @Test
    @WithMockUser
    void updateNotFoundReturns404() throws Exception {
        when(riskAreaBO.update(eq(99L), any(RiskAreaRequest.class)))
                .thenThrow(new ResourceNotFoundException("Área de risco 99 não encontrada."));

        mockMvc.perform(put("/api/areas/99").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateDuplicatedReturns409() throws Exception {
        when(riskAreaBO.update(eq(1L), any(RiskAreaRequest.class)))
                .thenThrow(new BusinessRuleException("Já existe uma área de risco chamada 'Várzea Norte' em São Paulo."));

        mockMvc.perform(put("/api/areas/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void updateWithoutLoginReturns401() throws Exception {
        mockMvc.perform(put("/api/areas/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(riskAreaBO);
    }

    // ---- DELETE ----

    @Test
    @WithMockUser
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/areas/1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(riskAreaBO).delete(1L);
    }

    @Test
    void deleteWithoutLoginReturns401() throws Exception {
        mockMvc.perform(delete("/api/areas/1").with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(riskAreaBO);
    }

    @Test
    @WithMockUser
    void deleteNotFoundReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Área de risco 99 não encontrada.")).when(riskAreaBO).delete(99L);

        mockMvc.perform(delete("/api/areas/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteLinkedAreaReturns409() throws Exception {
        doThrow(new DataIntegrityViolationException("FK")).when(riskAreaBO).delete(1L);

        mockMvc.perform(delete("/api/areas/1").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito com dados existentes"));
    }

    // ---- Location, erros do Spring MVC e erro inesperado ----

    @Test
    @WithMockUser
    void locationUsesHttpsBehindProxyAndIgnoresQueryString() throws Exception {
        when(riskAreaBO.create(any(RiskAreaRequest.class))).thenReturn(AREA);

        mockMvc.perform(post("/api/areas?origem=teste").with(csrf())
                        .header("Host", "rota-segura.onrender.com")
                        .header("X-Forwarded-Proto", "https")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "https://rota-segura.onrender.com/api/areas/1"));
    }

    @Test
    @WithMockUser
    void unsupportedMethodReturns405InPortuguese() throws Exception {
        mockMvc.perform(patch("/api/areas/1").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Método não permitido"))
                .andExpect(jsonPath("$.detail").value("O método PATCH não é aceito nesta rota."));
    }

    @Test
    @WithMockUser
    void nonJsonBodyReturns415InPortuguese() throws Exception {
        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("área"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.title").value("Formato não aceito"));
    }

    @Test
    void unknownRouteReturns404InPortuguese() throws Exception {
        mockMvc.perform(get("/api/nada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Esta rota não existe."));
    }

    @Test
    @WithMockUser
    void forgedForwardedPrefixKeepsErrorsInOurFormat() throws Exception {
        when(riskAreaBO.findById(99L)).thenThrow(new ResourceNotFoundException("Área de risco 99 não encontrada."));
        mockMvc.perform(get("/api/areas/99").header("X-Forwarded-Prefix", "/a b"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.instance").value("/a%20b/api/areas/99"));

        mockMvc.perform(post("/api/areas").with(csrf()).header("X-Forwarded-Prefix", "/a{b}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"radiusM\": 500", "\"radiusM\": 0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("radiusM")));

        mockMvc.perform(patch("/api/areas/1").with(csrf()).header("X-Forwarded-Prefix", "/a b"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Método não permitido"));
    }

    @Test
    @WithMockUser
    void createWithBracesInForgedPrefixStillReturns201() throws Exception {
        when(riskAreaBO.create(any(RiskAreaRequest.class))).thenReturn(AREA);

        mockMvc.perform(post("/api/areas").with(csrf()).header("X-Forwarded-Prefix", "/a{b}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/areas/1")));
    }

    @Test
    @WithMockUser
    void validationErrorsComeSortedByField() throws Exception {
        String invalid = """
                {"name": "", "city": "", "riskType": "FLOOD",
                 "latitude": -95, "longitude": 200, "radiusM": 0}
                """;

        mockMvc.perform(post("/api/areas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        contains("city", "latitude", "longitude", "name", "radiusM")));
    }

    @Test
    void otherSpringErrorsAreInPortuguese() throws Exception {
        when(riskAreaBO.findAll()).thenThrow(new AsyncRequestTimeoutException());
        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Serviço indisponível"));

        when(riskAreaBO.findById(1L)).thenThrow(new HttpMessageNotWritableException("falha ao escrever"));
        mockMvc.perform(get("/api/areas/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value(not(containsString("escrever"))));
    }

    // ---- Erro de segurança lançado dentro do controller: quem responde é o Spring Security ----

    @Test
    void accessDeniedInsideControllerWithoutLoginReturns401() throws Exception {
        when(riskAreaBO.findAll()).thenThrow(new AccessDeniedException("sem permissão"));

        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Login necessário"));
    }

    @Test
    @WithMockUser
    void accessDeniedInsideControllerWithLoginReturns403() throws Exception {
        when(riskAreaBO.findAll()).thenThrow(new AccessDeniedException("sem permissão"));

        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para esta ação."));
    }

    @Test
    void authenticationErrorInsideControllerReturns401() throws Exception {
        when(riskAreaBO.findAll()).thenThrow(new BadCredentialsException("credencial recusada"));

        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Login necessário"));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        when(riskAreaBO.findAll()).thenThrow(new IllegalStateException("conexão com o banco caiu"));

        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value(not(containsString("banco"))));
    }
}
