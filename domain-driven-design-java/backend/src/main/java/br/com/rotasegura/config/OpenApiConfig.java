package br.com.rotasegura.config;

import br.com.rotasegura.security.SecurityConfig;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Rota Segura API",
        version = "0.0.1",
        description = "Alertas de alagamento, check-in do morador e rota até o abrigo. "
                + "Para POST, PUT e DELETE: faça login em POST /api/auth/login, copie o token de "
                + "GET /api/auth/csrf e cole em Authorize. "
                + "Para sair: POST /api/auth/logout com o mesmo token (responde 204)."))
@SecurityScheme(
        name = OpenApiConfig.CSRF_SCHEME,
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-CSRF-TOKEN",
        description = "Token de GET /api/auth/csrf (depois do login)")
public class OpenApiConfig {

    static final String CSRF_SCHEME = "csrf";

    /** Marca com o cadeado do CSRF só as operações que exigem login e token. */
    @Bean
    public OpenApiCustomizer csrfOnProtectedOperations() {
        Set<PathItem.HttpMethod> readOnlyMethods =
                Set.of(PathItem.HttpMethod.GET, PathItem.HttpMethod.HEAD, PathItem.HttpMethod.OPTIONS);

        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) ->
                    pathItem.readOperationsMap().forEach((method, operation) -> {
                        // Público é só o POST das rotas liberadas, igual ao SecurityConfig
                        boolean publicPost = method == PathItem.HttpMethod.POST
                                && SecurityConfig.PUBLIC_POST_ENDPOINTS.contains(path);
                        boolean protectedOperation = !readOnlyMethods.contains(method) && !publicPost;
                        if (protectedOperation) {
                            operation.addSecurityItem(new SecurityRequirement().addList(CSRF_SCHEME));
                        }
                    }));
        };
    }
}
