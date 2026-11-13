package br.com.rotasegura.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void addsCsrfOnlyToProtectedWriteOperations() {
        Operation listAreas = new Operation();
        Operation createArea = new Operation();
        Operation updateArea = new Operation();
        Operation deleteArea = new Operation();
        Operation login = new Operation();
        Operation sendReading = new Operation();
        Operation deleteCheckIns = new Operation();

        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/areas", new PathItem().get(listAreas).post(createArea))
                .addPathItem("/api/areas/{id}", new PathItem().put(updateArea).delete(deleteArea))
                .addPathItem("/api/auth/login", new PathItem().post(login))
                .addPathItem("/api/sensor-readings", new PathItem().post(sendReading))
                .addPathItem("/api/check-ins", new PathItem().delete(deleteCheckIns)));

        config.apiConventions().customise(openApi);

        assertThat(createArea.getSecurity()).isNotEmpty();
        assertThat(updateArea.getSecurity()).isNotEmpty();
        assertThat(deleteArea.getSecurity()).isNotEmpty();
        // Rota liberada só no POST: um DELETE nela continua protegido
        assertThat(deleteCheckIns.getSecurity()).isNotEmpty();
        assertThat(listAreas.getSecurity()).isNull();
        assertThat(login.getSecurity()).isNull();
        assertThat(sendReading.getSecurity()).isNull();
    }

    @Test
    void protectedOperationsDocument401And403WithoutReplacingDeclaredOnes() {
        ApiResponse declared401 = new ApiResponse().description("Sem login");
        Operation createArea = new Operation().responses(new ApiResponses().addApiResponse("401", declared401));
        Operation listAreas = new Operation().responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("Lista de áreas")));
        Operation login = new Operation();

        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/areas", new PathItem().get(listAreas).post(createArea))
                .addPathItem("/api/auth/login", new PathItem().post(login)));

        config.apiConventions().customise(openApi);

        assertThat(createArea.getResponses().get("401")).isSameAs(declared401);
        assertThat(createArea.getResponses().get("403").getDescription()).isEqualTo("Token CSRF ausente ou inválido");
        assertThat(createArea.getResponses().get("403").getContent()).containsKey("application/problem+json");
        assertThat(listAreas.getResponses()).containsOnlyKeys("200");
        assertThat(login.getResponses()).isNull();
    }

    @Test
    void errorResponsesUseProblemDetailAndSuccessKeepsItsOwnSchema() {
        ApiResponse ok = new ApiResponse().description("Área encontrada");
        ApiResponse notFound = new ApiResponse().description("Área não encontrada");
        ApiResponse serverError = new ApiResponse().description("Erro interno");
        Operation findArea = new Operation().responses(new ApiResponses()
                .addApiResponse("200", ok)
                .addApiResponse("404", notFound)
                .addApiResponse("500", serverError));

        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/areas/{id}", new PathItem().get(findArea)));

        config.apiConventions().customise(openApi);

        assertThat(notFound.getContent().get("application/problem+json").getSchema().get$ref())
                .isEqualTo("#/components/schemas/ProblemDetail");
        assertThat(serverError.getContent()).containsKey("application/problem+json");
        assertThat(ok.getContent()).isNull();
    }

    @Test
    void problemDetailSchemaMatchesTheRealJson() {
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/areas", new PathItem().get(new Operation())));

        config.apiConventions().customise(openApi);

        Schema<?> problemDetail = openApi.getComponents().getSchemas().get("ProblemDetail");
        assertThat(problemDetail.getProperties())
                .containsKeys("type", "title", "status", "detail", "instance", "errors")
                .doesNotContainKey("properties");
        assertThat(openApi.getComponents().getSchemas()).containsKey("InvalidFieldResponse");
    }

    @Test
    void runningTwiceGivesTheSameDocument() {
        Operation createArea = new Operation();
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/areas", new PathItem().post(createArea)));

        config.apiConventions().customise(openApi);
        config.apiConventions().customise(openApi);

        assertThat(createArea.getSecurity()).hasSize(1);
        assertThat(createArea.getResponses()).containsOnlyKeys("401", "403");
    }

    @Test
    void ignoresDocumentWithoutPaths() {
        config.apiConventions().customise(new OpenAPI());
    }
}
