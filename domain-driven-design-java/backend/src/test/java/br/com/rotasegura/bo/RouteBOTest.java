package br.com.rotasegura.bo;

import br.com.rotasegura.dao.RouteServiceDAO;
import br.com.rotasegura.dto.AreaRiskResponse;
import br.com.rotasegura.dto.AssistedPersonResponse;
import br.com.rotasegura.dto.RiskAreaResponse;
import br.com.rotasegura.dto.RouteResponse;
import br.com.rotasegura.dto.RouteServiceRequest;
import br.com.rotasegura.dto.RouteServiceResponse;
import br.com.rotasegura.dto.ShelterResponse;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.exception.RouteOutsideMapException;
import br.com.rotasegura.exception.RouteServiceUnavailableException;
import br.com.rotasegura.model.RiskLevel;
import br.com.rotasegura.model.RiskType;
import br.com.rotasegura.model.RouteStatus;
import br.com.rotasegura.model.SpecialNeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Dados fictícios: nomes inventados, telefones com DDD 00; abrigo do OSM com vagas simuladas
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RouteBOTest {

    private static final long OWN_AREA = 1L;

    @Mock
    private AssistedPersonBO assistedPersonBO;
    @Mock
    private RiskAreaBO riskAreaBO;
    @Mock
    private RiskLevelBO riskLevelBO;
    @Mock
    private ShelterBO shelterBO;
    @Mock
    private RouteServiceDAO routeServiceDAO;

    @InjectMocks
    private RouteBO routeBO;

    private static final AssistedPersonResponse PERSON = new AssistedPersonResponse(7L, "Benedita Exemplo",
            "(00) 00000-0001", OWN_AREA, "Várzea do Jardim Helena", new BigDecimal("-23.480500"),
            new BigDecimal("-46.413800"), SpecialNeed.BEDRIDDEN, null, List.of());

    private static final ShelterResponse MURURES = new ShelterResponse(3L,
            "Escola Municipal de Educação Fundamental Murures", "Rua dos Murures",
            new BigDecimal("-23.476716"), new BigDecimal("-46.418575"), 200, 0, 200);
    private static final ShelterResponse FULL = new ShelterResponse(4L, "Escola Estadual Doutor Diogo de Faria",
            "Rua Santa Rosa de Lima, 591", new BigDecimal("-23.486630"), new BigDecimal("-46.428978"), 200, 200, 0);

    private static RiskAreaResponse area(long id) {
        return new RiskAreaResponse(id, "Área " + id, "São Paulo", null, RiskType.FLOOD,
                new BigDecimal("-23.48"), new BigDecimal("-46.41"), 500);
    }

    private static AreaRiskResponse risk(long areaId, RiskLevel level) {
        return new AreaRiskResponse(areaId, "Área " + areaId, true, level, BigDecimal.ONE, "SEN-" + areaId,
                OffsetDateTime.parse("2026-11-18T12:00:00Z"));
    }

    private static final RouteServiceResponse FOUND = new RouteServiceResponse("ROUTE_FOUND",
            new RouteServiceResponse.ShelterOutput(3L, "Escola Municipal de Educação Fundamental Murures",
                    new BigDecimal("-23.476716"), new BigDecimal("-46.418575")),
            700, 10,
            List.of(new RouteServiceResponse.Point(new BigDecimal("-23.4805"), new BigDecimal("-46.4138")),
                    new RouteServiceResponse.Point(new BigDecimal("-23.476716"), new BigDecimal("-46.418575"))),
            "Siga a rota até Escola Municipal de Educação Fundamental Murures.");

    @BeforeEach
    void setUp() {
        when(assistedPersonBO.findById(7L)).thenReturn(PERSON);
        when(shelterBO.findAll()).thenReturn(List.of(MURURES, FULL));
        when(riskAreaBO.findAll()).thenReturn(List.of(area(OWN_AREA), area(2L), area(3L), area(4L)));
        when(riskLevelBO.findAll()).thenReturn(List.of());
    }

    private RouteServiceRequest sentRequest() {
        ArgumentCaptor<RouteServiceRequest> captor = ArgumentCaptor.forClass(RouteServiceRequest.class);
        verify(routeServiceDAO).findRoute(captor.capture());
        return captor.getValue();
    }

    // ---- O que vai para o Python ----

    @Test
    void routeStartsWhereThePersonLivesAndSendsEveryShelterWithItsFreeSpots() {
        when(routeServiceDAO.findRoute(any())).thenReturn(FOUND);

        routeBO.findRouteForAssistedPerson(7L);

        RouteServiceRequest request = sentRequest();
        assertThat(request.origin().lat()).isEqualByComparingTo("-23.4805");
        assertThat(request.origin().lng()).isEqualByComparingTo("-46.4138");
        // O lotado vai também, com 0 vagas: quem o ignora é o Python (RN06)
        assertThat(request.shelters()).extracting(RouteServiceRequest.ShelterInput::availableSpots)
                .containsExactly(200, 0);
    }

    @Test
    void otherAreasCountAsFloodedFromHighRisk() {
        when(riskLevelBO.findAll()).thenReturn(List.of(
                risk(2L, RiskLevel.MEDIUM), risk(3L, RiskLevel.HIGH), risk(4L, RiskLevel.VERY_URGENT)));
        when(routeServiceDAO.findRoute(any())).thenReturn(FOUND);

        routeBO.findRouteForAssistedPerson(7L);

        assertThat(sentRequest().floodedAreas()).extracting(RouteServiceRequest.FloodedArea::areaId)
                .containsExactly(3L, 4L);
    }

    @Test
    void ownAreaOnlyCountsAsFloodedWhenVeryUrgent() {
        assertThat(RouteBO.isFlooded(RiskLevel.VERY_HIGH, true)).isFalse();
        assertThat(RouteBO.isFlooded(RiskLevel.VERY_URGENT, true)).isTrue();
        assertThat(RouteBO.isFlooded(RiskLevel.MEDIUM, false)).isFalse();
        assertThat(RouteBO.isFlooded(RiskLevel.HIGH, false)).isTrue();
        // Sem leitura recente não há como saber: a área não entra
        assertThat(RouteBO.isFlooded(null, false)).isFalse();
    }

    @Test
    void ownAreaInVeryHighRiskStaysOutSoTheRouteCanTakeThePersonAway() {
        when(riskLevelBO.findAll()).thenReturn(List.of(risk(OWN_AREA, RiskLevel.VERY_HIGH)));
        when(routeServiceDAO.findRoute(any())).thenReturn(FOUND);

        routeBO.findRouteForAssistedPerson(7L);

        assertThat(sentRequest().floodedAreas()).isEmpty();
    }

    @Test
    void ownAreaInVeryUrgentRiskIsSentAsFlooded() {
        when(riskLevelBO.findAll()).thenReturn(List.of(risk(OWN_AREA, RiskLevel.VERY_URGENT)));
        when(routeServiceDAO.findRoute(any())).thenReturn(FOUND);

        routeBO.findRouteForAssistedPerson(7L);

        assertThat(sentRequest().floodedAreas()).extracting(RouteServiceRequest.FloodedArea::areaId)
                .containsExactly(OWN_AREA);
    }

    @Test
    void routeBOHoldsNoTransactionWhileCallingPython() throws Exception {
        // Decidido em 16/11: o pool do Oracle tem 1 conexão; ela não pode ficar presa durante a chamada HTTP
        assertThat(RouteBO.class.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(RouteBO.class.getMethod("findRouteForAssistedPerson", Long.class)
                .isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    void areaWithoutRecentReadingIsNotFlooded() {
        when(riskLevelBO.findAll()).thenReturn(List.of(AreaRiskResponse.noData(2L, "Área 2")));
        when(routeServiceDAO.findRoute(any())).thenReturn(FOUND);

        routeBO.findRouteForAssistedPerson(7L);

        assertThat(sentRequest().floodedAreas()).isEmpty();
    }

    // ---- O que volta para o cliente ----

    @Test
    void routeFoundComesWithTheShelterAddressFromOurDatabase() {
        when(routeServiceDAO.findRoute(any())).thenReturn(FOUND);

        RouteResponse response = routeBO.findRouteForAssistedPerson(7L);

        assertThat(response.status()).isEqualTo(RouteStatus.ROUTE_FOUND);
        assertThat(response.assistedPersonId()).isEqualTo(7L);
        assertThat(response.shelter().address()).isEqualTo("Rua dos Murures");
        assertThat(response.distanceMeters()).isEqualTo(700);
        assertThat(response.durationMin()).isEqualTo(10);
        assertThat(response.path()).hasSize(2);
        assertThat(response.message()).startsWith("Siga a rota");
    }

    @Test
    void noSafeRouteKeepsThePythonGuidance() {
        when(routeServiceDAO.findRoute(any())).thenReturn(new RouteServiceResponse("NO_SAFE_ROUTE", null, null,
                null, List.of(), "Não há rota segura agora. Vá para um local alto e aguarde ajuda."));

        RouteResponse response = routeBO.findRouteForAssistedPerson(7L);

        assertThat(response.status()).isEqualTo(RouteStatus.NO_SAFE_ROUTE);
        assertThat(response.shelter()).isNull();
        assertThat(response.path()).isEmpty();
        assertThat(response.message()).contains("local alto");
    }

    @Test
    void noSafeRouteWithoutMessageGetsTheDefaultGuidance() {
        when(routeServiceDAO.findRoute(any())).thenReturn(
                new RouteServiceResponse("NO_SAFE_ROUTE", null, null, null, List.of(), " "));

        assertThat(routeBO.findRouteForAssistedPerson(7L).message()).isEqualTo(RouteBO.NO_SAFE_ROUTE_MESSAGE);
    }

    @Test
    void shelterNotInOurDatabaseComesWithoutAddress() {
        when(shelterBO.findAll()).thenReturn(List.of(FULL));
        when(routeServiceDAO.findRoute(any())).thenReturn(FOUND);

        RouteResponse response = routeBO.findRouteForAssistedPerson(7L);

        assertThat(response.status()).isEqualTo(RouteStatus.ROUTE_FOUND);
        assertThat(response.shelter().address()).isNull();
    }

    @Test
    void serviceDownStillGuidesThePerson() {
        when(routeServiceDAO.findRoute(any())).thenThrow(new RouteServiceUnavailableException("timeout", null));

        RouteResponse response = routeBO.findRouteForAssistedPerson(7L);

        assertThat(response.status()).isEqualTo(RouteStatus.ROUTE_UNAVAILABLE);
        assertThat(response.message()).isEqualTo(RouteBO.UNAVAILABLE_MESSAGE);
        assertThat(response.path()).isEmpty();
    }

    @Test
    void personOutsideThePilotMapGetsTheGeneralGuidance() {
        when(routeServiceDAO.findRoute(any())).thenThrow(new RouteOutsideMapException("origem a mais de 300 m"));

        RouteResponse response = routeBO.findRouteForAssistedPerson(7L);

        assertThat(response.status()).isEqualTo(RouteStatus.OUTSIDE_PILOT_AREA);
        assertThat(response.message()).isEqualTo(RouteBO.OUTSIDE_PILOT_AREA_MESSAGE);
    }

    @Test
    void unexpectedStatusFromPythonIsTreatedAsUnavailable() {
        when(routeServiceDAO.findRoute(any())).thenReturn(
                new RouteServiceResponse("SOMETHING_NEW", null, null, null, List.of(), "?"));

        assertThat(routeBO.findRouteForAssistedPerson(7L).status()).isEqualTo(RouteStatus.ROUTE_UNAVAILABLE);
    }

    @Test
    void unknownPersonIs404AndPythonIsNotCalled() {
        when(assistedPersonBO.findById(99L)).thenThrow(new ResourceNotFoundException("Pessoa assistida 99 não encontrada."));

        assertThatThrownBy(() -> routeBO.findRouteForAssistedPerson(99L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(routeServiceDAO);
    }
}
