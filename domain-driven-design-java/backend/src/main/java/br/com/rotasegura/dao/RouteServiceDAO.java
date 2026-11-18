package br.com.rotasegura.dao;

import br.com.rotasegura.dto.RouteServiceRequest;
import br.com.rotasegura.dto.RouteServiceResponse;
import br.com.rotasegura.exception.RouteOutsideMapException;
import br.com.rotasegura.exception.RouteServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;

/**
 * Acesso ao serviço Python de rota (contrato v2): é uma fonte de dados externa, como o banco é para os outros DAOs.
 * Sem regra de negócio: só faz a chamada e traduz as falhas em duas exceções que o RouteBO entende.
 */
@Component
public class RouteServiceDAO {

    private static final Logger log = LoggerFactory.getLogger(RouteServiceDAO.class);

    private final RestClient restClient;

    public RouteServiceDAO(RestClient routeServiceRestClient) {
        this.restClient = routeServiceRestClient;
    }

    /**
     * POST /route. 400 (origem fora do mapa) vira RouteOutsideMapException; qualquer outra falha
     * (rede, mais de 10 s, 5xx, JSON fora do contrato) vira RouteServiceUnavailableException.
     */
    public RouteServiceResponse findRoute(RouteServiceRequest request) {
        try {
            return restClient.post()
                    .uri("/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((httpRequest, response) -> {
                        if (response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) {
                            // Corpo do Python: {"error": "INVALID_INPUT", "message": "..."}; vai só para o log
                            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new RouteOutsideMapException("Serviço de rota recusou a origem: " + body);
                        }
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            // Ex.: 422 (JSON fora do esquema) indica erro de contrato: o corpo vai para o log
                            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new RouteServiceUnavailableException("Serviço de rota respondeu "
                                    + response.getStatusCode().value() + ": " + body, null);
                        }
                        RouteServiceResponse body = response.bodyTo(RouteServiceResponse.class);
                        if (body == null || body.status() == null) {
                            throw new RouteServiceUnavailableException("Serviço de rota respondeu sem corpo", null);
                        }
                        return body;
                    });
        } catch (RouteOutsideMapException | RouteServiceUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            // Fronteira com um serviço externo: timeout, conexão recusada ou JSON que não bate com o contrato
            throw new RouteServiceUnavailableException("Falha ao chamar o serviço de rota: " + e.getMessage(), e);
        }
    }

    /**
     * Ping no /health a cada 10 min (decidido em 10/11), para o Render gratuito não hibernar o serviço Python
     * enquanto a API está no ar. O primeiro sai 5 s depois da subida: quando a API acorda, o Python (que pode
     * levar uns 30 s para acordar) já começa a acordar junto. Falhar aqui não é problema: só fica no log.
     */
    @Scheduled(initialDelayString = "PT5S", fixedDelayString = "${app.route-service.ping-interval:PT10M}")
    public void keepAwake() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.info("Ping no serviço de rota falhou: {}", e.getMessage());
        }
    }
}
