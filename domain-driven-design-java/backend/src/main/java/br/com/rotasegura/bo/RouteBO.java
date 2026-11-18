package br.com.rotasegura.bo;

import br.com.rotasegura.dao.RouteServiceDAO;
import br.com.rotasegura.dto.AreaRiskResponse;
import br.com.rotasegura.dto.AssistedPersonResponse;
import br.com.rotasegura.dto.RiskAreaResponse;
import br.com.rotasegura.dto.RouteResponse;
import br.com.rotasegura.dto.RouteServiceRequest;
import br.com.rotasegura.dto.RouteServiceResponse;
import br.com.rotasegura.dto.ShelterResponse;
import br.com.rotasegura.exception.RouteOutsideMapException;
import br.com.rotasegura.exception.RouteServiceUnavailableException;
import br.com.rotasegura.model.RiskLevel;
import br.com.rotasegura.model.RouteStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rota segura até o abrigo (RF07), calculada pelo serviço Python.
 * SEM @Transactional de propósito (decidido em 16/11): cada leitura do banco abaixo fecha a própria transação
 * antes da chamada HTTP, então a única conexão do Oracle não fica presa esperando o Python (até 10 s).
 */
@Service
public class RouteBO {

    private static final Logger log = LoggerFactory.getLogger(RouteBO.class);

    /** Decidido em 10/11: sem o Python, orientar ir ao abrigo ou a um local mais alto. */
    static final String UNAVAILABLE_MESSAGE =
            "A rota está indisponível agora. Vá para o abrigo mais próximo ou para um local mais alto.";
    /** Usada se o Python mandar NO_SAFE_ROUTE sem mensagem: a resposta sempre traz uma orientação. */
    static final String NO_SAFE_ROUTE_MESSAGE = "Não há rota segura agora. Vá para um local alto e aguarde ajuda.";
    static final String OUTSIDE_PILOT_AREA_MESSAGE =
            "A rota segura ainda não está disponível na sua região. Vá para o abrigo mais próximo ou para um local mais alto.";

    /** Decidido em 18/11: as outras áreas contam como alagadas a partir de ALTO (3 cm). */
    static final RiskLevel FLOODED_FROM = RiskLevel.HIGH;
    /** Decidido em 18/11: a área da própria pessoa só entra em MUITO URGENTE (água alta demais para caminhar). */
    static final RiskLevel OWN_AREA_FLOODED_FROM = RiskLevel.VERY_URGENT;

    private final AssistedPersonBO assistedPersonBO;
    private final RiskAreaBO riskAreaBO;
    private final RiskLevelBO riskLevelBO;
    private final ShelterBO shelterBO;
    private final RouteServiceDAO routeServiceDAO;

    public RouteBO(AssistedPersonBO assistedPersonBO, RiskAreaBO riskAreaBO, RiskLevelBO riskLevelBO,
                   ShelterBO shelterBO, RouteServiceDAO routeServiceDAO) {
        this.assistedPersonBO = assistedPersonBO;
        this.riskAreaBO = riskAreaBO;
        this.riskLevelBO = riskLevelBO;
        this.shelterBO = shelterBO;
        this.routeServiceDAO = routeServiceDAO;
    }

    /** Rota de onde a pessoa mora até o abrigo com vaga mais próximo. Pessoa inexistente: 404 (sem chamar o Python). */
    public RouteResponse findRouteForAssistedPerson(Long assistedPersonId) {
        AssistedPersonResponse person = assistedPersonBO.findById(assistedPersonId);
        List<ShelterResponse> shelters = shelterBO.findAll();

        RouteServiceRequest request = new RouteServiceRequest(
                new RouteServiceRequest.Point(person.latitude(), person.longitude()),
                shelters.stream()
                        .map(s -> new RouteServiceRequest.ShelterInput(
                                s.id(), s.name(), s.latitude(), s.longitude(), s.availableSpots()))
                        .toList(),
                floodedAreas(person.areaId()));

        try {
            return toResponse(assistedPersonId, routeServiceDAO.findRoute(request), shelters);
        } catch (RouteOutsideMapException e) {
            log.info("Pessoa assistida {} fora do mapa do bairro-piloto: {}", assistedPersonId, e.getMessage());
            return RouteResponse.withoutRoute(assistedPersonId, RouteStatus.OUTSIDE_PILOT_AREA,
                    OUTSIDE_PILOT_AREA_MESSAGE);
        } catch (RouteServiceUnavailableException e) {
            log.warn("Serviço de rota indisponível: {}", e.getMessage());
            return RouteResponse.withoutRoute(assistedPersonId, RouteStatus.ROUTE_UNAVAILABLE, UNAVAILABLE_MESSAGE);
        }
    }

    /** RN05: áreas cujas ruas a rota precisa evitar, pelo nível de risco atual (sem leitura recente, não entra). */
    List<RouteServiceRequest.FloodedArea> floodedAreas(Long ownAreaId) {
        Map<Long, RiskLevel> levelByArea = riskLevelBO.findAll().stream()
                .filter(AreaRiskResponse::hasData)
                .collect(Collectors.toMap(AreaRiskResponse::areaId, AreaRiskResponse::riskLevel));

        return riskAreaBO.findAll().stream()
                .filter(area -> isFlooded(levelByArea.get(area.id()), area.id().equals(ownAreaId)))
                .map(RouteBO::toFloodedArea)
                .toList();
    }

    /** A ordem do enum RiskLevel é do menos ao mais urgente, então dá para comparar. */
    static boolean isFlooded(RiskLevel level, boolean ownArea) {
        if (level == null) {
            return false;
        }
        return level.compareTo(ownArea ? OWN_AREA_FLOODED_FROM : FLOODED_FROM) >= 0;
    }

    private static RouteServiceRequest.FloodedArea toFloodedArea(RiskAreaResponse area) {
        return new RouteServiceRequest.FloodedArea(area.id(), area.latitude(), area.longitude(), area.radiusM());
    }

    private RouteResponse toResponse(Long assistedPersonId, RouteServiceResponse route, List<ShelterResponse> shelters) {
        if ("NO_SAFE_ROUTE".equals(route.status())) {
            String message = route.message() == null || route.message().isBlank() ? NO_SAFE_ROUTE_MESSAGE : route.message();
            return RouteResponse.withoutRoute(assistedPersonId, RouteStatus.NO_SAFE_ROUTE, message);
        }
        if (!"ROUTE_FOUND".equals(route.status()) || route.shelter() == null) {
            // Status que o contrato não prevê: tratar como serviço com problema, nunca inventar uma rota
            log.warn("Resposta do serviço de rota fora do contrato: status {}", route.status());
            return RouteResponse.withoutRoute(assistedPersonId, RouteStatus.ROUTE_UNAVAILABLE, UNAVAILABLE_MESSAGE);
        }

        Map<Long, ShelterResponse> shelterById = shelters.stream()
                .collect(Collectors.toMap(ShelterResponse::id, Function.identity()));
        RouteServiceResponse.ShelterOutput target = route.shelter();
        ShelterResponse registered = shelterById.get(target.id());
        String address = registered == null ? null : registered.address();

        List<RouteResponse.RoutePoint> path = Objects.requireNonNullElse(route.path(),
                        List.<RouteServiceResponse.Point>of()).stream()
                .map(point -> new RouteResponse.RoutePoint(point.lat(), point.lng()))
                .toList();

        return new RouteResponse(assistedPersonId, RouteStatus.ROUTE_FOUND,
                new RouteResponse.ShelterInfo(target.id(), target.name(), address, target.lat(), target.lng()),
                route.distanceMeters(), route.durationMin(), path, route.message());
    }
}
