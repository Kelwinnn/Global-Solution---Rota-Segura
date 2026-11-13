package br.com.rotasegura.bo;

import br.com.rotasegura.dao.RiskAreaDAO;
import br.com.rotasegura.dao.SensorDAO;
import br.com.rotasegura.dto.SensorRequest;
import br.com.rotasegura.dto.SensorResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskType;
import br.com.rotasegura.model.Sensor;
import br.com.rotasegura.model.SensorStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Dados de exemplo simulados (nenhum dado real)
@ExtendWith(MockitoExtension.class)
class SensorBOTest {

    private static final BigDecimal LAT = new BigDecimal("-23.482500");
    private static final BigDecimal LNG = new BigDecimal("-46.414000");

    @Mock
    private SensorDAO sensorDAO;

    @Mock
    private RiskAreaDAO riskAreaDAO;

    @InjectMocks
    private SensorBO sensorBO;

    private static RiskArea area(Long id, String name) {
        RiskArea area = new RiskArea(name, "São Paulo", "Jardim Helena", RiskType.FLOOD,
                new BigDecimal("-23.482000"), new BigDecimal("-46.415000"), 600);
        ReflectionTestUtils.setField(area, "id", id);
        return area;
    }

    private static Sensor sensor(Long id, RiskArea area, String code) {
        Sensor sensor = new Sensor(area, code, LAT, LNG, SensorStatus.ACTIVE);
        ReflectionTestUtils.setField(sensor, "id", id);
        return sensor;
    }

    private static SensorRequest request(Long areaId, String code) {
        return new SensorRequest(areaId, code, LAT, LNG, SensorStatus.ACTIVE);
    }

    // ---- create ----

    @Test
    void createLinksSensorToAreaAndTrimsTheCode() {
        RiskArea area = area(1L, "Várzea do Jardim Helena");
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(area));
        when(sensorDAO.existsByCode("SEN-JH-001")).thenReturn(false);
        when(sensorDAO.save(any(Sensor.class))).thenAnswer(invocation -> {
            Sensor saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        SensorResponse response = sensorBO.create(request(1L, "  SEN-JH-001 "));

        ArgumentCaptor<Sensor> saved = ArgumentCaptor.forClass(Sensor.class);
        verify(sensorDAO).save(saved.capture());
        assertThat(saved.getValue().getArea()).isSameAs(area);
        assertThat(saved.getValue().getCode()).isEqualTo("SEN-JH-001");
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.areaId()).isEqualTo(1L);
        assertThat(response.areaName()).isEqualTo("Várzea do Jardim Helena");
        assertThat(response.status()).isEqualTo(SensorStatus.ACTIVE);
    }

    @Test
    void createWithUnknownAreaThrowsAndDoesNotSave() {
        when(riskAreaDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorBO.create(request(99L, "SEN-JH-001")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Área de risco 99");
        verify(sensorDAO, never()).save(any());
    }

    @Test
    void createWithDuplicatedCodeThrowsAndDoesNotSave() {
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(area(1L, "Área A")));
        when(sensorDAO.existsByCode("SEN-JH-001")).thenReturn(true);

        assertThatThrownBy(() -> sensorBO.create(request(1L, "SEN-JH-001")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SEN-JH-001");
        verify(sensorDAO, never()).save(any());
    }

    // ---- find ----

    @Test
    void findAllWithoutFilterListsEverySensor() {
        RiskArea area = area(1L, "Área A");
        when(sensorDAO.findAllOrderByCode()).thenReturn(List.of(
                sensor(10L, area, "SEN-001"), sensor(11L, area, "SEN-002")));

        List<SensorResponse> result = sensorBO.findAll(null);

        assertThat(result).extracting(SensorResponse::code).containsExactly("SEN-001", "SEN-002");
        verify(sensorDAO, never()).findByAreaIdOrderByCode(anyLong());
    }

    @Test
    void findAllWithAreaIdListsOnlyThatArea() {
        when(sensorDAO.findByAreaIdOrderByCode(2L)).thenReturn(List.of(sensor(12L, area(2L, "Área B"), "SEN-003")));

        List<SensorResponse> result = sensorBO.findAll(2L);

        assertThat(result).extracting(SensorResponse::areaName).containsExactly("Área B");
        verify(sensorDAO, never()).findAllOrderByCode();
    }

    @Test
    void findByIdReturnsSensorWithItsArea() {
        when(sensorDAO.findById(10L)).thenReturn(Optional.of(sensor(10L, area(1L, "Área A"), "SEN-001")));

        SensorResponse response = sensorBO.findById(10L);

        assertThat(response.code()).isEqualTo("SEN-001");
        assertThat(response.areaName()).isEqualTo("Área A");
    }

    @Test
    void findByIdNotFoundThrows() {
        when(sensorDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorBO.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sensor 99");
    }

    // ---- update ----

    @Test
    void updateChangesFieldsAndCanMoveSensorToAnotherArea() {
        Sensor existing = sensor(10L, area(1L, "Área A"), "SEN-001");
        RiskArea newArea = area(2L, "Área B");
        when(sensorDAO.findById(10L)).thenReturn(Optional.of(existing));
        when(riskAreaDAO.findById(2L)).thenReturn(Optional.of(newArea));
        when(sensorDAO.existsByCodeExcludingId("SEN-009", 10L)).thenReturn(false);
        when(sensorDAO.save(existing)).thenReturn(existing);

        SensorResponse response = sensorBO.update(10L, new SensorRequest(2L, " SEN-009 ",
                new BigDecimal("-23.490000"), new BigDecimal("-46.400000"), SensorStatus.INACTIVE));

        assertThat(response.code()).isEqualTo("SEN-009");
        assertThat(response.areaId()).isEqualTo(2L);
        assertThat(response.areaName()).isEqualTo("Área B");
        assertThat(response.latitude()).isEqualByComparingTo("-23.49");
        assertThat(response.status()).isEqualTo(SensorStatus.INACTIVE);
    }

    @Test
    void updateKeepingOwnCodeIsAllowed() {
        Sensor existing = sensor(10L, area(1L, "Área A"), "SEN-001");
        when(sensorDAO.findById(10L)).thenReturn(Optional.of(existing));
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(existing.getArea()));
        // O próprio sensor é excluído da checagem, então manter o código não conta como duplicado
        when(sensorDAO.existsByCodeExcludingId("SEN-001", 10L)).thenReturn(false);
        when(sensorDAO.save(existing)).thenReturn(existing);

        assertThat(sensorBO.update(10L, request(1L, "SEN-001")).code()).isEqualTo("SEN-001");
    }

    @Test
    void updateWithCodeOfAnotherSensorThrows() {
        when(sensorDAO.findById(10L)).thenReturn(Optional.of(sensor(10L, area(1L, "Área A"), "SEN-001")));
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(area(1L, "Área A")));
        when(sensorDAO.existsByCodeExcludingId("SEN-002", 10L)).thenReturn(true);

        assertThatThrownBy(() -> sensorBO.update(10L, request(1L, "SEN-002")))
                .isInstanceOf(BusinessRuleException.class);
        verify(sensorDAO, never()).save(any());
    }

    @Test
    void updateWithUnknownAreaThrows() {
        when(sensorDAO.findById(10L)).thenReturn(Optional.of(sensor(10L, area(1L, "Área A"), "SEN-001")));
        when(riskAreaDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorBO.update(10L, request(99L, "SEN-001")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Área de risco 99");
        verify(sensorDAO, never()).save(any());
    }

    @Test
    void updateNotFoundThrows() {
        when(sensorDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorBO.update(99L, request(1L, "SEN-001")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sensor 99");
        verify(sensorDAO, never()).save(any());
    }

    // ---- delete ----

    @Test
    void deleteRemovesSensor() {
        Sensor existing = sensor(10L, area(1L, "Área A"), "SEN-001");
        when(sensorDAO.findById(10L)).thenReturn(Optional.of(existing));

        sensorBO.delete(10L);

        verify(sensorDAO).delete(existing);
    }

    @Test
    void deleteNotFoundThrows() {
        when(sensorDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorBO.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(sensorDAO, never()).delete(any());
    }
}
