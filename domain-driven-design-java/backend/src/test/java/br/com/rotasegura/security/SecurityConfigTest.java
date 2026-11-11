package br.com.rotasegura.security;

import br.com.rotasegura.config.CorsConfig;
import br.com.rotasegura.controller.AuthController;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
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
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class})
// Traz o filtro de X-Forwarded-* (server.forward-headers-strategy=framework) para o MockMvc
@ImportAutoConfiguration(ServletWebServerFactoryAutoConfiguration.class)
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

    /** A segurança deixou passar: qualquer status menos 401 e 403 (a rota pode nem existir ainda). */
    private static ResultMatcher passedSecurity() {
        return result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    private String fetchCsrfToken(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
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
    void headIsOpenLikeGet() throws Exception {
        mockMvc.perform(head("/api/auth/csrf"))
                .andExpect(status().isOk());
    }

    @Test
    void citizenEndpointsNeedNeitherLoginNorCsrf() throws Exception {
        mockMvc.perform(post("/api/sensor-readings")).andExpect(passedSecurity());
        mockMvc.perform(post("/api/check-ins")).andExpect(passedSecurity());
    }

    // ---- Sem login: 401 ----

    @Test
    void postPutDeleteWithoutLoginReturn401() throws Exception {
        mockMvc.perform(post("/api/areas").with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/areas/1").with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/areas/1").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void withoutLoginAndWithoutCsrfReturns401() throws Exception {
        mockMvc.perform(post("/api/areas")).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredSessionWithOldTokenReturns401() throws Exception {
        String oldToken = fetchCsrfToken(new MockHttpSession());

        // Sessão nova e vazia = a sessão antiga expirou
        mockMvc.perform(post("/api/areas").session(new MockHttpSession()).header("X-CSRF-TOKEN", oldToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithoutLoginReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(head("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    // ---- Com login, sem CSRF: 403 ----

    @Test
    @WithMockUser
    void loggedInWithoutCsrfReturns403() throws Exception {
        mockMvc.perform(post("/api/areas")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void publicRoutesSkipCsrfOnlyForPost() throws Exception {
        // As rotas do cidadão são livres só no POST; PUT e DELETE nelas exigem token
        mockMvc.perform(put("/api/check-ins")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/sensor-readings")).andExpect(status().isForbidden());
    }

    @Test
    void publicRoutesRequireLoginForOtherMethods() throws Exception {
        mockMvc.perform(delete("/api/check-ins").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void publicRouteWithTrailingSlashIsNotOpen() throws Exception {
        // Trava o comportamento atual: só o caminho exato fica liberado
        mockMvc.perform(post("/api/check-ins/").with(csrf())).andExpect(status().isUnauthorized());
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
        String tokenBeforeLogin = fetchCsrfToken(session);
        String sessionIdBeforeLogin = session.getId();

        // 2. Login com usuário e senha certos: o id da sessão muda
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME));
        assertThat(session.getId()).isNotEqualTo(sessionIdBeforeLogin);

        // 3. O token de antes do login deixou de valer (logado, então 403)
        mockMvc.perform(post("/api/areas").session(session).header("X-CSRF-TOKEN", tokenBeforeLogin))
                .andExpect(status().isForbidden());

        // 4. Com um token novo, o POST protegido passa pela segurança
        String token = fetchCsrfToken(session);
        mockMvc.perform(post("/api/areas").session(session).header("X-CSRF-TOKEN", token))
                .andExpect(passedSecurity());

        // 5. A sessão vale para as chamadas seguintes
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME));

        // 6. Logout encerra a sessão
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

    @Test
    void sameOriginBehindRenderProxyIsNotTreatedAsCors() throws Exception {
        // O Render atende em https e repassa ao Java em http, porta 80, com X-Forwarded-Proto: https
        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.HOST, "rota-segura.onrender.com")
                        .header(HttpHeaders.ORIGIN, "https://rota-segura.onrender.com")
                        .header("X-Forwarded-Proto", "https")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(USERNAME, "wrong-" + UUID.randomUUID())))
                // 401 = chegou ao login; 403 "Invalid CORS request" seria o bug
                .andExpect(status().isUnauthorized());
    }
}
