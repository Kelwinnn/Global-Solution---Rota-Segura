package br.com.rotasegura.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
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

        config.csrfOnProtectedOperations().customise(openApi);

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
    void ignoresDocumentWithoutPaths() {
        config.csrfOnProtectedOperations().customise(new OpenAPI());
    }
}
