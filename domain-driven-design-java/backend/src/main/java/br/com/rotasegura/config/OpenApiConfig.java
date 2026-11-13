package br.com.rotasegura.config;

import br.com.rotasegura.dto.InvalidFieldResponse;
import br.com.rotasegura.security.SecurityConfig;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

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
    static final String PROBLEM_JSON = "application/problem+json";
    static final String PROBLEM_DETAIL_REF = "#/components/schemas/ProblemDetail";

    private static final Set<PathItem.HttpMethod> READ_ONLY_METHODS =
            Set.of(PathItem.HttpMethod.GET, PathItem.HttpMethod.HEAD, PathItem.HttpMethod.OPTIONS);

    /**
     * Regras aplicadas a toda a documentação, em ordem: primeiro marca as operações protegidas
     * (cadeado do CSRF, 401 e 403), depois põe o formato ProblemDetail em toda resposta de erro.
     */
    @Bean
    public OpenApiCustomizer apiConventions() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            markProtectedOperations(openApi);
            useProblemDetailOnErrorResponses(openApi);
        };
    }

    /** Operações que exigem login e token: cadeado do CSRF e respostas 401 e 403 (se ainda não declaradas). */
    private static void markProtectedOperations(OpenAPI openApi) {
        openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    // Público é só o POST das rotas liberadas, igual ao SecurityConfig
                    boolean publicPost = method == PathItem.HttpMethod.POST
                            && SecurityConfig.PUBLIC_POST_ENDPOINTS.contains(path);
                    boolean protectedOperation = !READ_ONLY_METHODS.contains(method) && !publicPost;
                    if (!protectedOperation) {
                        return;
                    }

                    SecurityRequirement csrf = new SecurityRequirement().addList(CSRF_SCHEME);
                    if (operation.getSecurity() == null || !operation.getSecurity().contains(csrf)) {
                        operation.addSecurityItem(csrf);
                    }
                    if (operation.getResponses() == null) {
                        operation.setResponses(new ApiResponses());
                    }
                    operation.getResponses().putIfAbsent("401",
                            new ApiResponse().description("Sem login ou sessão expirada"));
                    operation.getResponses().putIfAbsent("403",
                            new ApiResponse().description("Token CSRF ausente ou inválido"));
                }));
    }

    /** Nas respostas 4xx e 5xx, o Swagger mostra o formato ProblemDetail em vez do DTO de sucesso. */
    private static void useProblemDetailOnErrorResponses(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        Components components = openApi.getComponents();
        ModelConverters.getInstance().read(ProblemDetail.class).forEach(components::addSchemas);
        ModelConverters.getInstance().read(InvalidFieldResponse.class).forEach(components::addSchemas);

        // Ajusta a descrição ao JSON que a API devolve de verdade: sem "properties", com "errors"
        Schema<?> problemSchema = components.getSchemas().get("ProblemDetail");
        if (problemSchema.getProperties() != null) {
            problemSchema.getProperties().remove("properties");
        }
        problemSchema.addProperty("errors", new ArraySchema()
                .items(new Schema<>().$ref("#/components/schemas/InvalidFieldResponse"))
                .description("Só nos erros de validação (400): um item por campo inválido"));

        openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {
                    if (operation.getResponses() == null) {
                        return;
                    }
                    operation.getResponses().forEach((code, response) -> {
                        if (code.startsWith("4") || code.startsWith("5")) {
                            response.setContent(new Content().addMediaType(PROBLEM_JSON,
                                    new MediaType().schema(new Schema<>().$ref(PROBLEM_DETAIL_REF))));
                        }
                    });
                }));
    }
}
