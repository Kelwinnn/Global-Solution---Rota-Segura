package br.com.rotasegura.security;

import br.com.rotasegura.config.CorsConfig;
import br.com.rotasegura.controller.AuthController;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class SecurityConfigTest {

    // Gerados a cada execução: nenhuma credencial escrita no teste
    private static final String USERNAME = "user-" + UUID.randomUUID();
    private static final String PASSWORD = UUID.randomUUID().toString();

    private static final String FRONT_ORIGIN = "http://localhost:5173";

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("app.security.admin.username", () -> USERNAME);
        registry.add("app.security.admin.password", () -> PASSWORD);
    }

    @Autowired
    private MockMvc mockMvc;

    private static String loginJson(String username, String password) {
        return """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);
    }

    // ---- Rotas abertas ----

    @Test
    void getIsOpenWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void citizenEndpointsNeedNeitherLoginNorCsrf() throws Exception {
        // 404 = passou pela segurança (o controller ainda não existe); 401 ou 403 seria bloqueio
        mockMvc.perform(post("/api/sensor-readings")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/check-ins")).andExpect(status().isNotFound());
    }

    // ---- Rotas protegidas ----

    @Test
    void postWithoutLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/areas").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void postWithoutCsrfReturns403() throws Exception {
        mockMvc.perform(post("/api/areas"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meWithoutLoginReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Login ----

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(USERNAME, "wrong-" + UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithBlankFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullFlowCsrfLoginProtectedPostLogout() throws Exception {
        // 1. O front pede o token CSRF (isso abre a sessão)
        MockHttpSession session = new MockHttpSession();
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.token");
        String sessionIdBeforeLogin = session.getId();

        // 2. Login com usuário e senha certos
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME));
        assertThat(session.getId()).isNotEqualTo(sessionIdBeforeLogin);

        // 3. A sessão vale para as chamadas seguintes
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME));

        // 4. POST protegido com o token real passa pela segurança (404 = rota ainda não existe)
        mockMvc.perform(post("/api/areas").session(session).header("X-CSRF-TOKEN", token))
                .andExpect(status().isNotFound());

        // 5. Logout encerra a sessão
        mockMvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", token))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    // ---- CORS ----

    @Test
    void corsAllowsFrontOriginWithCredentials() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, FRONT_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void corsBlocksUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
