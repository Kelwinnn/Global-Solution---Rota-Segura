package br.com.rotasegura.bo;

import br.com.rotasegura.dao.SensorDAO;
import br.com.rotasegura.dao.SensorReadingDAO;
import br.com.rotasegura.dto.SensorReadingRequest;
import br.com.rotasegura.dto.SensorReadingResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskType;
import br.com.rotasegura.model.Sensor;
import br.com.rotasegura.model.SensorReading;
import br.com.rotasegura.model.SensorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Dados de exemplo simulados (nenhum dado real)
@ExtendWith(MockitoExtension.class)
class SensorReadingBOTest {

    /** "Agora" fixo nos testes, para a consulta sem "to" ter um fim previsível. */
    private static final Instant NOW = Instant.parse("2026-11-19T21:30:00Z");
    private static final OffsetDateTime MEASURED_AT = OffsetDateTime.parse("2026-11-19T18:00:00-03:00");

    @Mock
    private SensorReadingDAO sensorReadingDAO;

    @Mock
    private SensorDAO sensorDAO;

    private SensorReadingBO sensorReadingBO;

    @BeforeEach
    void setUp() {
        sensorReadingBO = new SensorReadingBO(sensorReadingDAO, sensorDAO, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Sensor sensor(Long id, String code, SensorStatus status) {
        RiskArea area = new RiskArea("Várzea do Jardim Helena", "São Paulo", "Jardim Helena", RiskType.FLOOD,
                new BigDecimal("-23.482000"), new BigDecimal("-46.415000"), 600);
        ReflectionTestUtils.setField(area, "id", 1L);
        Sensor sensor = new Sensor(area, code, new BigDecimal("-23.481500"), new BigDecimal("-46.414200"), status);
        ReflectionTestUtils.setField(sensor, "id", id);
        return sensor;
    }

    private static SensorReadingRequest request(String code) {
        return new SensorReadingRequest(code, new BigDecimal("12.40"), new BigDecimal("2.30"), MEASURED_AT);
    }

    private static final OffsetDateTime NOW_UTC = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

    // ---- create ----

    @Test
    void createRecordsTheReadingForTheSensorWithThatCode() {
        Sensor sensor = sensor(10L, "SEN-JH-001", SensorStatus.ACTIVE);
        when(sensorDAO.findByCode("SEN-JH-001")).thenReturn(Optional.of(sensor));
        when(sensorReadingDAO.save(any(SensorReading.class))).thenAnswer(invocation -> {
            SensorReading saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        SensorReadingResponse response = sensorReadingBO.create(request("  SEN-JH-001 "));

        ArgumentCaptor<SensorReading> saved = ArgumentCaptor.forClass(SensorReading.class);
        verify(sensorReadingDAO).save(saved.capture());
        assertThat(saved.getValue().getSensor()).isSameAs(sensor);
        assertThat(saved.getValue().getRainMm()).isEqualByComparingTo("12.4");
        assertThat(saved.getValue().getWaterLevelCm()).isEqualByComparingTo("2.3");
        assertThat(saved.getValue().getMeasuredAt()).isEqualTo(MEASURED_AT);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.sensorId()).isEqualTo(10L);
        assertThat(response.sensorCode()).isEqualTo("SEN-JH-001");
        assertThat(response.areaId()).isEqualTo(1L);
    }

    @Test
    void createKeepsTwoDecimalsAndCutsTheTimeToMicroseconds() {
        Sensor sensor = sensor(10L, "SEN-JH-001", SensorStatus.ACTIVE);
        OffsetDateTime withNanos = OffsetDateTime.parse("2026-11-19T18:00:00.123456789-03:00");
        OffsetDateTime cut = OffsetDateTime.parse("2026-11-19T18:00:00.123456-03:00");
        when(sensorDAO.findByCode("SEN-JH-001")).thenReturn(Optional.of(sensor));
        when(sensorReadingDAO.existsBySensorAt(10L, cut)).thenReturn(false);
        when(sensorReadingDAO.save(any(SensorReading.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sensorReadingBO.create(new SensorReadingRequest("SEN-JH-001", new BigDecimal("12.4"), new BigDecimal("2"), withNanos));

        ArgumentCaptor<SensorReading> saved = ArgumentCaptor.forClass(SensorReading.class);
        verify(sensorReadingDAO).save(saved.capture());
        assertThat(saved.getValue().getRainMm()).hasToString("12.40");
        assertThat(saved.getValue().getWaterLevelCm()).hasToString("2.00");
        assertThat(saved.getValue().getMeasuredAt()).isEqualTo(cut);
    }

    @Test
    void createWithNegativeScaleKeepsTwoDecimals() {
        // 100 sem zeros à direita vira 1E+2 (escala negativa): precisa ser gravado como 100.00
        when(sensorDAO.findByCode("SEN-JH-001")).thenReturn(Optional.of(sensor(10L, "SEN-JH-001", SensorStatus.ACTIVE)));
        when(sensorReadingDAO.existsBySensorAt(10L, MEASURED_AT)).thenReturn(false);
        when(sensorReadingDAO.save(any(SensorReading.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sensorReadingBO.create(new SensorReadingRequest("SEN-JH-001", new BigDecimal("1E+2"), new BigDecimal("0"), MEASURED_AT));

        ArgumentCaptor<SensorReading> saved = ArgumentCaptor.forClass(SensorReading.class);
        verify(sensorReadingDAO).save(saved.capture());
        assertThat(saved.getValue().getRainMm()).hasToString("100.00");
        assertThat(saved.getValue().getWaterLevelCm()).hasToString("0.00");
    }

    @Test
    void createRepeatedReadingThrowsAndDoesNotSave() {
        when(sensorDAO.findByCode("SEN-JH-001")).thenReturn(Optional.of(sensor(10L, "SEN-JH-001", SensorStatus.ACTIVE)));
        when(sensorReadingDAO.existsBySensorAt(10L, MEASURED_AT)).thenReturn(true);

        assertThatThrownBy(() -> sensorReadingBO.create(request("SEN-JH-001")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Já existe uma leitura do sensor 'SEN-JH-001' em 2026-11-19T21:00:00Z.");
        verify(sensorReadingDAO, never()).save(any());
    }

    @Test
    void createWithUnknownCodeThrowsAndDoesNotSave() {
        when(sensorDAO.findByCode("SEN-XX-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorReadingBO.create(request("SEN-XX-999")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SEN-XX-999");
        verify(sensorReadingDAO, never()).save(any());
    }

    @Test
    void createForInactiveSensorThrowsAndDoesNotSave() {
        when(sensorDAO.findByCode("SEN-JP-002")).thenReturn(Optional.of(sensor(11L, "SEN-JP-002", SensorStatus.INACTIVE)));

        assertThatThrownBy(() -> sensorReadingBO.create(request("SEN-JP-002")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo");
        verify(sensorReadingDAO, never()).save(any());
    }

    // ---- consultas ----

    @Test
    void findBySensorWithoutPeriodHasNoStartAndEndsNow() {
        when(sensorReadingDAO.findBySensorUntil(10L, NOW_UTC)).thenReturn(List.of());

        assertThat(sensorReadingBO.findBySensor(10L, null, null)).isEmpty();
        verify(sensorReadingDAO).findBySensorUntil(10L, NOW_UTC);
        verify(sensorReadingDAO, never()).findBySensorBetween(any(), any(), any());
    }

    @Test
    void findBySensorWithOnlyFromEndsNow() {
        OffsetDateTime from = OffsetDateTime.parse("2026-11-19T00:00:00-03:00");
        when(sensorReadingDAO.findBySensorBetween(10L, from, NOW_UTC)).thenReturn(List.of());

        sensorReadingBO.findBySensor(10L, from, null);

        verify(sensorReadingDAO).findBySensorBetween(10L, from, NOW_UTC);
    }

    @Test
    void findBySensorWithOnlyToHasNoStart() {
        OffsetDateTime to = OffsetDateTime.parse("2026-11-19T12:00:00-03:00");
        when(sensorReadingDAO.findBySensorUntil(10L, to)).thenReturn(List.of());

        sensorReadingBO.findBySensor(10L, null, to);

        verify(sensorReadingDAO).findBySensorUntil(10L, to);
        verify(sensorReadingDAO, never()).findBySensorBetween(any(), any(), any());
    }

    @Test
    void findBySensorUsesThePeriodInformedAndMapsEveryReading() {
        OffsetDateTime from = OffsetDateTime.parse("2026-11-19T00:00:00-03:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-11-19T23:59:59-03:00");
        Sensor sensor = sensor(10L, "SEN-JH-001", SensorStatus.ACTIVE);
        when(sensorReadingDAO.findBySensorBetween(10L, from, to)).thenReturn(List.of(
                new SensorReading(sensor, new BigDecimal("8.10"), new BigDecimal("2.80"), MEASURED_AT.plusMinutes(10)),
                new SensorReading(sensor, new BigDecimal("12.40"), new BigDecimal("2.30"), MEASURED_AT)));

        List<SensorReadingResponse> result = sensorReadingBO.findBySensor(10L, from, to);

        assertThat(result).extracting(SensorReadingResponse::measuredAt)
                .containsExactly(MEASURED_AT.plusMinutes(10), MEASURED_AT);
        assertThat(result).extracting(SensorReadingResponse::sensorCode).containsOnly("SEN-JH-001");
    }

    @Test
    void findByIdReturnsTheReading() {
        SensorReading reading = new SensorReading(sensor(10L, "SEN-JH-001", SensorStatus.ACTIVE),
                new BigDecimal("12.40"), new BigDecimal("2.30"), MEASURED_AT);
        ReflectionTestUtils.setField(reading, "id", 100L);
        when(sensorReadingDAO.findById(100L)).thenReturn(Optional.of(reading));

        assertThat(sensorReadingBO.findById(100L).waterLevelCm()).isEqualByComparingTo("2.3");
    }

    @Test
    void findByIdNotFoundThrows() {
        when(sensorReadingDAO.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorReadingBO.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Leitura 999");
    }
}
