package br.com.rotasegura.bo;

import br.com.rotasegura.dao.RiskAreaDAO;
import br.com.rotasegura.dto.RiskAreaRequest;
import br.com.rotasegura.dto.RiskAreaResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskType;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Dados de exemplo simulados (nenhum dado real)
@ExtendWith(MockitoExtension.class)
class RiskAreaBOTest {

    @Mock
    private RiskAreaDAO riskAreaDAO;

    @InjectMocks
    private RiskAreaBO riskAreaBO;

    private static RiskAreaRequest request(String name, String city, String district) {
        return new RiskAreaRequest(name, city, district, RiskType.FLOOD,
                new BigDecimal("-23.483100"), new BigDecimal("-46.398200"), 500);
    }

    private static RiskArea savedArea(Long id, String name, String city) {
        RiskArea area = new RiskArea(name, city, "Jardim Helena", RiskType.FLOOD,
                new BigDecimal("-23.483100"), new BigDecimal("-46.398200"), 500);
        ReflectionTestUtils.setField(area, "id", id);
        return area;
    }

    // ---- create ----

    @Test
    void createSavesTrimmedDataAndReturnsResponse() {
        when(riskAreaDAO.existsByNameAndCity("Várzea Norte", "São Paulo")).thenReturn(false);
        when(riskAreaDAO.save(any(RiskArea.class))).thenAnswer(invocation -> {
            RiskArea area = invocation.getArgument(0);
            ReflectionTestUtils.setField(area, "id", 1L);
            return area;
        });

        RiskAreaResponse response = riskAreaBO.create(request("  Várzea Norte ", " São Paulo ", "   "));

        ArgumentCaptor<RiskArea> saved = ArgumentCaptor.forClass(RiskArea.class);
        verify(riskAreaDAO).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Várzea Norte");
        assertThat(saved.getValue().getCity()).isEqualTo("São Paulo");
        assertThat(saved.getValue().getDistrict()).isNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.radiusM()).isEqualTo(500);
    }

    @Test
    void createWithDuplicatedNameInCityThrowsAndDoesNotSave() {
        when(riskAreaDAO.existsByNameAndCity("Várzea Norte", "São Paulo")).thenReturn(true);

        assertThatThrownBy(() -> riskAreaBO.create(request("Várzea Norte", "São Paulo", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Várzea Norte");
        verify(riskAreaDAO, never()).save(any());
    }

    // ---- find ----

    @Test
    void findAllMapsEveryArea() {
        when(riskAreaDAO.findAllOrderByName()).thenReturn(List.of(
                savedArea(1L, "Área A", "Guarulhos"),
                savedArea(2L, "Área B", "Mogi das Cruzes")));

        List<RiskAreaResponse> result = riskAreaBO.findAll();

        assertThat(result).extracting(RiskAreaResponse::name).containsExactly("Área A", "Área B");
    }

    @Test
    void findByIdReturnsArea() {
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(savedArea(1L, "Área A", "Guarulhos")));

        assertThat(riskAreaBO.findById(1L).name()).isEqualTo("Área A");
    }

    @Test
    void findByIdNotFoundThrows() {
        when(riskAreaDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskAreaBO.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---- update ----

    @Test
    void updateChangesAllFields() {
        RiskArea existing = savedArea(1L, "Área A", "Guarulhos");
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(existing));
        when(riskAreaDAO.existsByNameAndCityExcludingId("Área Nova", "Suzano", 1L)).thenReturn(false);
        when(riskAreaDAO.save(existing)).thenReturn(existing);

        RiskAreaResponse response = riskAreaBO.update(1L, new RiskAreaRequest("Área Nova", "Suzano", "Centro",
                RiskType.LANDSLIDE, new BigDecimal("-23.540000"), new BigDecimal("-46.310000"), 300));

        assertThat(response.name()).isEqualTo("Área Nova");
        assertThat(response.city()).isEqualTo("Suzano");
        assertThat(response.district()).isEqualTo("Centro");
        assertThat(response.riskType()).isEqualTo(RiskType.LANDSLIDE);
        assertThat(response.radiusM()).isEqualTo(300);
    }

    @Test
    void updateTrimsTextAndTurnsBlankDistrictIntoNull() {
        RiskArea existing = savedArea(1L, "Área A", "Guarulhos");
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(existing));
        when(riskAreaDAO.existsByNameAndCityExcludingId("Área A", "Guarulhos", 1L)).thenReturn(false);
        when(riskAreaDAO.save(existing)).thenReturn(existing);

        riskAreaBO.update(1L, request("  Área A ", " Guarulhos  ", "   "));
        assertThat(existing.getName()).isEqualTo("Área A");
        assertThat(existing.getCity()).isEqualTo("Guarulhos");
        assertThat(existing.getDistrict()).isNull();

        riskAreaBO.update(1L, request("Área A", "Guarulhos", "  Centro "));
        assertThat(existing.getDistrict()).isEqualTo("Centro");
    }

    @Test
    void updateKeepingOwnNameIsAllowed() {
        RiskArea existing = savedArea(1L, "Área A", "Guarulhos");
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(existing));
        // A própria área é excluída da checagem, então manter o nome não conta como duplicado
        when(riskAreaDAO.existsByNameAndCityExcludingId("Área A", "Guarulhos", 1L)).thenReturn(false);
        when(riskAreaDAO.save(existing)).thenReturn(existing);

        assertThat(riskAreaBO.update(1L, request("Área A", "Guarulhos", null)).name()).isEqualTo("Área A");
    }

    @Test
    void updateWithNameOfAnotherAreaThrows() {
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(savedArea(1L, "Área A", "Guarulhos")));
        when(riskAreaDAO.existsByNameAndCityExcludingId("Área B", "Guarulhos", 1L)).thenReturn(true);

        assertThatThrownBy(() -> riskAreaBO.update(1L, request("Área B", "Guarulhos", null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(riskAreaDAO, never()).save(any());
    }

    @Test
    void updateNotFoundThrows() {
        when(riskAreaDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskAreaBO.update(99L, request("Área A", "Guarulhos", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(riskAreaDAO, never()).save(any());
    }

    // ---- delete ----

    @Test
    void deleteRemovesArea() {
        RiskArea existing = savedArea(1L, "Área A", "Guarulhos");
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(existing));

        riskAreaBO.delete(1L);

        verify(riskAreaDAO).delete(existing);
    }

    @Test
    void deleteNotFoundThrows() {
        when(riskAreaDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskAreaBO.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(riskAreaDAO, never()).delete(any());
    }
}
