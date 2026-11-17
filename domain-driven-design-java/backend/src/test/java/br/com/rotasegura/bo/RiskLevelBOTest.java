package br.com.rotasegura.bo;

import br.com.rotasegura.dao.RiskAreaDAO;
import br.com.rotasegura.dao.SensorReadingDAO;
import br.com.rotasegura.dto.AreaRiskResponse;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskLevel;
import br.com.rotasegura.model.RiskType;
import br.com.rotasegura.model.Sensor;
import br.com.rotasegura.model.SensorReading;
import br.com.rotasegura.model.SensorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Dados de exemplo simulados (nenhum dado real)
@ExtendWith(MockitoExtension.class)
class RiskLevelBOTest {

    /** "Agora" fixo: 15/11/2026 21:30 UTC (18:30 em Brasília). */
    private static final Instant NOW = Instant.parse("2026-11-15T21:30:00Z");
    private static final OffsetDateTime NOW_UTC = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
    /** Janela da regra: última 1 hora, até agora mais a folga de 5 minutos. */
    private static final OffsetDateTime WINDOW_START = NOW_UTC.minusHours(1);
    private static final OffsetDateTime WINDOW_END = NOW_UTC.plusMinutes(5);

    @Mock
    private RiskAreaDAO riskAreaDAO;

    @Mock
    private SensorReadingDAO sensorReadingDAO;

    private RiskLevelBO riskLevelBO;

    private RiskArea helena;
    private RiskArea pantanal;

    @BeforeEach
    void setUp() {
        riskLevelBO = new RiskLevelBO(riskAreaDAO, sensorReadingDAO, Clock.fixed(NOW, ZoneOffset.UTC));
        helena = area(1L, "Várzea do Jardim Helena");
        pantanal = area(2L, "Beira-Rio do Jardim Pantanal");
    }

    private static RiskArea area(Long id, String name) {
        RiskArea area = new RiskArea(name, "São Paulo", null, RiskType.FLOOD,
                new BigDecimal("-23.482000"), new BigDecimal("-46.415000"), 600);
        ReflectionTestUtils.setField(area, "id", id);
        return area;
    }

    private static Sensor sensor(Long id, RiskArea area, String code) {
        Sensor sensor = new Sensor(area, code, new BigDecimal("-23.481500"), new BigDecimal("-46.414200"),
                SensorStatus.ACTIVE);
        ReflectionTestUtils.setField(sensor, "id", id);
        return sensor;
    }

    private static SensorReading reading(Sensor sensor, String waterLevelCm, int minutesBeforeNow) {
        return new SensorReading(sensor, new BigDecimal("10.00"), new BigDecimal(waterLevelCm),
                NOW_UTC.minusMinutes(minutesBeforeNow));
    }

    // ---- RN01: faixas e bordas ----

    @ParameterizedTest(name = "{0} cm -> {1}")
    @CsvSource({
            "0.00,     LOW",
            "1.05,     LOW",
            "1.09,     LOW",
            "1.10,     MEDIUM",
            "2.99,     MEDIUM",
            "3.00,     HIGH",
            "5.09,     HIGH",
            "5.10,     VERY_HIGH",
            "10.00,    VERY_HIGH",
            "10.01,    VERY_URGENT",
            "99999.99, VERY_URGENT"
    })
    void classifyFollowsTheRangesAndTheEdgeRule(String waterLevelCm, RiskLevel expected) {
        assertThat(riskLevelBO.classify(new BigDecimal(waterLevelCm))).isEqualTo(expected);
    }

    // ---- Nível de uma área ----

    @Test
    void areaTakesTheWorstOfTheLatestReadingOfEachSensor() {
        Sensor jh1 = sensor(10L, helena, "SEN-JH-001");
        Sensor jh2 = sensor(11L, helena, "SEN-JH-002");
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(helena));
        when(sensorReadingDAO.findByAreaAndSensorStatusBetween(1L, SensorStatus.ACTIVE, WINDOW_START, WINDOW_END))
                .thenReturn(List.of(
                        // SEN-JH-001: a água já esteve alta, mas a leitura mais recente é baixa: vale a recente
                        reading(jh1, "6.00", 30),
                        reading(jh1, "0.80", 5),
                        // SEN-JH-002: a mais recente é média, e é a pior das mais recentes
                        reading(jh2, "1.00", 20),
                        reading(jh2, "2.40", 10)));

        AreaRiskResponse risk = riskLevelBO.findByArea(1L);

        assertThat(risk.hasData()).isTrue();
        assertThat(risk.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(risk.waterLevelCm()).isEqualByComparingTo("2.40");
        assertThat(risk.sensorCode()).isEqualTo("SEN-JH-002");
        assertThat(risk.measuredAt()).isEqualTo(NOW_UTC.minusMinutes(10));
        assertThat(risk.areaName()).isEqualTo("Várzea do Jardim Helena");
    }

    @Test
    void areaWithoutRecentReadingHasNoData() {
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(helena));
        when(sensorReadingDAO.findByAreaAndSensorStatusBetween(1L, SensorStatus.ACTIVE, WINDOW_START, WINDOW_END))
                .thenReturn(List.of());

        AreaRiskResponse risk = riskLevelBO.findByArea(1L);

        assertThat(risk.hasData()).isFalse();
        assertThat(risk.riskLevel()).isNull();
        assertThat(risk.waterLevelCm()).isNull();
        assertThat(risk.sensorCode()).isNull();
        assertThat(risk.measuredAt()).isNull();
        assertThat(risk.areaId()).isEqualTo(1L);
    }

    @Test
    void unknownAreaThrowsAndDoesNotLookForReadings() {
        when(riskAreaDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskLevelBO.findByArea(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(sensorReadingDAO, never()).findByAreaAndSensorStatusBetween(anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    // ---- Nível de todas as áreas ----

    @Test
    void findAllGivesEveryAreaEvenWithoutReadings() {
        Sensor jp1 = sensor(20L, pantanal, "SEN-JP-001");
        when(riskAreaDAO.findAllOrderByName()).thenReturn(List.of(pantanal, helena));
        when(sensorReadingDAO.findBySensorStatusBetween(SensorStatus.ACTIVE, WINDOW_START, WINDOW_END))
                .thenReturn(List.of(reading(jp1, "11.30", 15)));

        List<AreaRiskResponse> risks = riskLevelBO.findAll();

        assertThat(risks).extracting(AreaRiskResponse::areaName)
                .containsExactly("Beira-Rio do Jardim Pantanal", "Várzea do Jardim Helena");
        assertThat(risks.get(0).riskLevel()).isEqualTo(RiskLevel.VERY_URGENT);
        assertThat(risks.get(0).sensorCode()).isEqualTo("SEN-JP-001");
        assertThat(risks.get(1).hasData()).isFalse();
    }

    @Test
    void findAllKeepsEachAreaWithItsOwnSensors() {
        Sensor jh1 = sensor(10L, helena, "SEN-JH-001");
        Sensor jp1 = sensor(20L, pantanal, "SEN-JP-001");
        when(riskAreaDAO.findAllOrderByName()).thenReturn(List.of(pantanal, helena));
        when(sensorReadingDAO.findBySensorStatusBetween(SensorStatus.ACTIVE, WINDOW_START, WINDOW_END))
                .thenReturn(List.of(reading(jh1, "0.50", 5), reading(jp1, "4.00", 5)));

        List<AreaRiskResponse> risks = riskLevelBO.findAll();

        assertThat(risks).extracting(AreaRiskResponse::riskLevel)
                .containsExactly(RiskLevel.HIGH, RiskLevel.LOW);
    }

    @Test
    void fullTieIsDecidedBySensorCodeWhateverTheOrder() {
        Sensor a = sensor(10L, helena, "SEN-JH-001");
        Sensor b = sensor(11L, helena, "SEN-JH-002");
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(helena));
        when(sensorReadingDAO.findByAreaAndSensorStatusBetween(1L, SensorStatus.ACTIVE, WINDOW_START, WINDOW_END))
                .thenReturn(List.of(reading(b, "3.20", 5), reading(a, "3.20", 5)))
                .thenReturn(List.of(reading(a, "3.20", 5), reading(b, "3.20", 5)));

        // Mesmo nível e mesmo instante: o resultado é o mesmo nas duas ordens
        assertThat(riskLevelBO.findByArea(1L).sensorCode()).isEqualTo("SEN-JH-002");
        assertThat(riskLevelBO.findByArea(1L).sensorCode()).isEqualTo("SEN-JH-002");
    }

    @Test
    void sameWaterLevelInTwoSensorsKeepsTheMostRecent() {
        Sensor jh1 = sensor(10L, helena, "SEN-JH-001");
        Sensor jh2 = sensor(11L, helena, "SEN-JH-002");
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(helena));
        when(sensorReadingDAO.findByAreaAndSensorStatusBetween(1L, SensorStatus.ACTIVE, WINDOW_START, WINDOW_END))
                .thenReturn(List.of(reading(jh1, "3.20", 20), reading(jh2, "3.20", 5)));

        assertThat(riskLevelBO.findByArea(1L).sensorCode()).isEqualTo("SEN-JH-002");
    }
}
