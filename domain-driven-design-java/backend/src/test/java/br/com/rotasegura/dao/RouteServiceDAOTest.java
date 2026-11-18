package br.com.rotasegura.dao;

import br.com.rotasegura.dto.RouteServiceRequest;
import br.com.rotasegura.dto.RouteServiceResponse;
import br.com.rotasegura.exception.RouteOutsideMapException;
import br.com.rotasegura.exception.RouteServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** MockRestServiceServer faz o papel do serviço Python: nada sai pela rede. JSON no formato do contrato v2. */
class RouteServiceDAOTest {

    private static final String BASE = "http://route-service.test";

    private MockRestServiceServer server;
    private RouteServiceDAO dao;

    private static final RouteServiceRequest REQUEST = new RouteServiceRequest(
            new RouteServiceRequest.Point(new BigDecimal("-23.4805"), new BigDecimal("-46.4138")),
            List.of(new RouteServiceRequest.ShelterInput(3L, "Escola Exemplo", new BigDecimal("-23.476716"),
                    new BigDecimal("-46.418575"), 200)),
            List.of(new RouteServiceRequest.FloodedArea(2L, new BigDecimal("-23.477"), new BigDecimal("-46.398"), 800)));

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        dao = new RouteServiceDAO(builder.build());
    }

    @Test
    void sendsTheRequestInTheContractFormatAndReadsTheRoute() {
        server.expect(requestTo(BASE + "/route"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"origin": {"lat": -23.4805, "lng": -46.4138},
                         "shelters": [{"id": 3, "name": "Escola Exemplo", "lat": -23.476716, "lng": -46.418575,
                                       "availableSpots": 200}],
                         "floodedAreas": [{"areaId": 2, "lat": -23.477, "lng": -46.398, "radiusM": 800}]}
                        """, true))
                .andRespond(withSuccess("""
                        {"status": "ROUTE_FOUND",
                         "shelter": {"id": 3, "name": "Escola Exemplo", "lat": -23.476716, "lng": -46.418575},
                         "distanceMeters": 700, "durationMin": 10,
                         "path": [{"lat": -23.4805, "lng": -46.4138}, {"lat": -23.476716, "lng": -46.418575}],
                         "message": "Siga a rota até Escola Exemplo."}
                        """, MediaType.APPLICATION_JSON));

        RouteServiceResponse response = dao.findRoute(REQUEST);

        assertThat(response.status()).isEqualTo("ROUTE_FOUND");
        assertThat(response.shelter().id()).isEqualTo(3L);
        assertThat(response.distanceMeters()).isEqualTo(700);
        assertThat(response.path()).hasSize(2);
        server.verify();
    }

    @Test
    void noSafeRouteIsANormalAnswer() {
        server.expect(requestTo(BASE + "/route")).andRespond(withSuccess("""
                {"status": "NO_SAFE_ROUTE", "shelter": null, "distanceMeters": null, "durationMin": null,
                 "path": [], "message": "Não há rota segura agora. Vá para um local alto e aguarde ajuda."}
                """, MediaType.APPLICATION_JSON));

        RouteServiceResponse response = dao.findRoute(REQUEST);

        assertThat(response.status()).isEqualTo("NO_SAFE_ROUTE");
        assertThat(response.shelter()).isNull();
    }

    @Test
    void badRequestMeansOriginOutsideTheMap() {
        server.expect(requestTo(BASE + "/route")).andRespond(withBadRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"INVALID_INPUT\", \"message\": \"origin: a mais de 300 m da rua mais próxima\"}"));

        assertThatThrownBy(() -> dao.findRoute(REQUEST)).isInstanceOf(RouteOutsideMapException.class);
    }

    @Test
    void mapNotLoadedIsUnavailable() {
        server.expect(requestTo(BASE + "/route")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"MAP_UNAVAILABLE\", \"message\": \"mapa não carregado\"}"));

        assertThatThrownBy(() -> dao.findRoute(REQUEST)).isInstanceOf(RouteServiceUnavailableException.class);
    }

    @Test
    void internalErrorIsUnavailable() {
        server.expect(requestTo(BASE + "/route")).andRespond(withServerError());

        assertThatThrownBy(() -> dao.findRoute(REQUEST)).isInstanceOf(RouteServiceUnavailableException.class);
    }

    @Test
    void timeoutIsUnavailable() {
        server.expect(requestTo(BASE + "/route")).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> dao.findRoute(REQUEST)).isInstanceOf(RouteServiceUnavailableException.class);
    }

    @Test
    void connectionRefusedIsUnavailable() {
        server.expect(requestTo(BASE + "/route")).andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> dao.findRoute(REQUEST)).isInstanceOf(RouteServiceUnavailableException.class);
    }

    @Test
    void answerOutsideTheContractIsUnavailable() {
        server.expect(requestTo(BASE + "/route")).andRespond(withSuccess("<html>Not Found</html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> dao.findRoute(REQUEST)).isInstanceOf(RouteServiceUnavailableException.class);
    }

    @Test
    void emptyAnswerIsUnavailable() {
        server.expect(requestTo(BASE + "/route")).andRespond(withSuccess());

        assertThatThrownBy(() -> dao.findRoute(REQUEST)).isInstanceOf(RouteServiceUnavailableException.class);
    }

    @Test
    void keepAwakePingsHealthAndNeverThrows() {
        server.expect(requestTo(BASE + "/health")).andExpect(method(HttpMethod.GET)).andRespond(withServerError());

        dao.keepAwake();

        server.verify();
    }
}
