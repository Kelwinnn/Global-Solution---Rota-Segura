package br.com.rotasegura.bo;

import br.com.rotasegura.dao.ShelterDAO;
import br.com.rotasegura.dto.ShelterRequest;
import br.com.rotasegura.dto.ShelterResponse;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.Shelter;
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
class ShelterBOTest {

    @Mock
    private ShelterDAO shelterDAO;

    @InjectMocks
    private ShelterBO shelterBO;

    private static ShelterRequest request(String name, int capacity, int occupied) {
        return new ShelterRequest(name, " Rua Exemplo, 100 ", new BigDecimal("-23.478226"),
                new BigDecimal("-46.381098"), capacity, occupied);
    }

    private static Shelter saved(Long id, String name, int capacity, int occupied) {
        Shelter shelter = new Shelter(name, "Rua Exemplo, 100", new BigDecimal("-23.478226"),
                new BigDecimal("-46.381098"), capacity, occupied);
        ReflectionTestUtils.setField(shelter, "id", id);
        return shelter;
    }

    // ---- create ----

    @Test
    void createSavesTrimmedDataAndComputesAvailableSpots() {
        when(shelterDAO.save(any(Shelter.class))).thenAnswer(invocation -> {
            Shelter shelter = invocation.getArgument(0);
            ReflectionTestUtils.setField(shelter, "id", 1L);
            return shelter;
        });

        ShelterResponse response = shelterBO.create(request("  CEU Exemplo ", 400, 150));

        ArgumentCaptor<Shelter> saved = ArgumentCaptor.forClass(Shelter.class);
        verify(shelterDAO).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("CEU Exemplo");
        assertThat(saved.getValue().getAddress()).isEqualTo("Rua Exemplo, 100");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.availableSpots()).isEqualTo(250);
    }

    @Test
    void createFullShelterIsAllowed() {
        when(shelterDAO.save(any(Shelter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(shelterBO.create(request("Escola Exemplo", 200, 200)).availableSpots()).isZero();
    }

    @Test
    void createWithMoreOccupiedThanCapacityThrowsAndDoesNotSave() {
        assertThatThrownBy(() -> shelterBO.create(request("Escola Exemplo", 200, 201)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("201")
                .hasMessageContaining("200");
        verify(shelterDAO, never()).save(any());
    }

    // ---- find ----

    @Test
    void findAllMapsEveryShelter() {
        when(shelterDAO.findAllOrderByName()).thenReturn(List.of(
                saved(1L, "Abrigo A", 400, 0), saved(2L, "Abrigo B", 200, 200)));

        assertThat(shelterBO.findAll()).extracting(ShelterResponse::availableSpots).containsExactly(400, 0);
    }

    @Test
    void findByIdReturnsShelter() {
        when(shelterDAO.findById(1L)).thenReturn(Optional.of(saved(1L, "Abrigo A", 400, 10)));

        assertThat(shelterBO.findById(1L).availableSpots()).isEqualTo(390);
    }

    @Test
    void findByIdNotFoundThrows() {
        when(shelterDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shelterBO.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Abrigo 99");
    }

    // ---- update ----

    @Test
    void updateChangesAllFieldsIncludingOccupancy() {
        Shelter existing = saved(1L, "Abrigo A", 400, 0);
        when(shelterDAO.findById(1L)).thenReturn(Optional.of(existing));
        when(shelterDAO.save(existing)).thenReturn(existing);

        ShelterResponse response = shelterBO.update(1L, request("Abrigo Novo", 300, 120));

        assertThat(response.name()).isEqualTo("Abrigo Novo");
        assertThat(response.capacity()).isEqualTo(300);
        assertThat(response.occupiedSpots()).isEqualTo(120);
        assertThat(response.availableSpots()).isEqualTo(180);
    }

    @Test
    void updateWithMoreOccupiedThanCapacityThrowsAndDoesNotSave() {
        when(shelterDAO.findById(1L)).thenReturn(Optional.of(saved(1L, "Abrigo A", 400, 0)));

        assertThatThrownBy(() -> shelterBO.update(1L, request("Abrigo A", 100, 150)))
                .isInstanceOf(BusinessRuleException.class);
        verify(shelterDAO, never()).save(any());
    }

    @Test
    void updateNotFoundThrows() {
        when(shelterDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shelterBO.update(99L, request("Abrigo A", 100, 0)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(shelterDAO, never()).save(any());
    }

    // ---- delete ----

    @Test
    void deleteRemovesShelter() {
        Shelter existing = saved(1L, "Abrigo A", 400, 0);
        when(shelterDAO.findById(1L)).thenReturn(Optional.of(existing));

        shelterBO.delete(1L);

        verify(shelterDAO).delete(existing);
    }

    @Test
    void deleteNotFoundThrows() {
        when(shelterDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shelterBO.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(shelterDAO, never()).delete(any());
    }
}
