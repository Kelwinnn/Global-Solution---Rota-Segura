package br.com.rotasegura.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cliente HTTP do serviço Python de rota (RF07). A URL vem de ROUTE_SERVICE_URL (padrão: http://localhost:8000).
 * @EnableScheduling liga o ping periódico no /health (RouteServiceDAO.keepAwake), decidido em 10/11.
 */
@Configuration
@EnableScheduling
public class RouteServiceConfig {

    /** Decidido em 10/11: passou disso, a rota fica indisponível e o alerta segue sem ela. */
    public static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient routeServiceRestClient(RestClient.Builder builder,
                                             @Value("${app.route-service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        // O Builder do Spring Boot já vem com o ObjectMapper da aplicação (mesmas regras de JSON da API)
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
