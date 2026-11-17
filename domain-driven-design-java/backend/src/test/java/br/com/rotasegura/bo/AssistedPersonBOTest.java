package br.com.rotasegura.bo;

import br.com.rotasegura.dao.AssistedPersonDAO;
import br.com.rotasegura.dao.RiskAreaDAO;
import br.com.rotasegura.dto.AssistedPersonRequest;
import br.com.rotasegura.dto.AssistedPersonResponse;
import br.com.rotasegura.dto.GuardianRequest;
import br.com.rotasegura.exception.BusinessRuleException;
import br.com.rotasegura.exception.ResourceNotFoundException;
import br.com.rotasegura.model.AssistedPerson;
import br.com.rotasegura.model.Guardian;
import br.com.rotasegura.model.RiskArea;
import br.com.rotasegura.model.RiskType;
import br.com.rotasegura.model.SpecialNeed;
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

// Dados fictícios: nomes inventados e telefones com DDD 00 (não existe)
@ExtendWith(MockitoExtension.class)
class AssistedPersonBOTest {

    @Mock
    private AssistedPersonDAO assistedPersonDAO;

    @Mock
    private RiskAreaDAO riskAreaDAO;

    @InjectMocks
    private AssistedPersonBO assistedPersonBO;

    private static RiskArea area(Long id, String name) {
        RiskArea area = new RiskArea(name, "São Paulo", "Jardim Helena", RiskType.FLOOD,
                new BigDecimal("-23.482000"), new BigDecimal("-46.415000"), 550);
        ReflectionTestUtils.setField(area, "id", id);
        return area;
    }

    private static GuardianRequest guardian(String name) {
        return new GuardianRequest(" " + name + " ", " (00) 00000-0002 ", " filha ");
    }

    private static AssistedPersonRequest request(Long areaId, List<GuardianRequest> guardians) {
        return new AssistedPersonRequest(" Benedita Exemplo ", " (00) 00000-0001 ", areaId,
                new BigDecimal("-23.480500"), new BigDecimal("-46.413800"), SpecialNeed.BEDRIDDEN, "   ", guardians);
    }

    private static AssistedPerson saved(Long id, RiskArea area, String... guardianNames) {
        AssistedPerson person = new AssistedPerson(area, "Benedita Exemplo", "(00) 00000-0001",
                new BigDecimal("-23.480500"), new BigDecimal("-46.413800"), SpecialNeed.BEDRIDDEN, null);
        ReflectionTestUtils.setField(person, "id", id);
        person.replaceGuardians(java.util.Arrays.stream(guardianNames)
                .map(name -> new Guardian(name, "(00) 00000-0002", "filha")).toList());
        return person;
    }

    // ---- create ----

    @Test
    void createSavesPersonWithGuardiansLinkedBothWays() {
        RiskArea area = area(1L, "Várzea do Jardim Helena");
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(area));
        when(assistedPersonDAO.save(any(AssistedPerson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssistedPersonResponse response = assistedPersonBO.create(request(1L, List.of(guardian("Ana Exemplo"))));

        ArgumentCaptor<AssistedPerson> saved = ArgumentCaptor.forClass(AssistedPerson.class);
        verify(assistedPersonDAO).save(saved.capture());
        AssistedPerson person = saved.getValue();
        assertThat(person.getName()).isEqualTo("Benedita Exemplo");
        assertThat(person.getPhone()).isEqualTo("(00) 00000-0001");
        assertThat(person.getNotes()).isNull();
        assertThat(person.getArea()).isSameAs(area);
        assertThat(person.getGuardians()).singleElement().satisfies(guardian -> {
            assertThat(guardian.getName()).isEqualTo("Ana Exemplo");
            assertThat(guardian.getRelationship()).isEqualTo("filha");
            // O responsável aponta de volta para o assistido: é isso que grava a FK ID_ASSISTED_PERSON
            assertThat(guardian.getAssistedPerson()).isSameAs(person);
        });
        assertThat(response.areaName()).isEqualTo("Várzea do Jardim Helena");
        assertThat(response.guardians()).extracting("name").containsExactly("Ana Exemplo");
    }

    @Test
    void createWithoutGuardiansBreaksRn03AndDoesNotSave() {
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(area(1L, "Área A")));

        assertThatThrownBy(() -> assistedPersonBO.create(request(1L, List.of())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RN03");
        verify(assistedPersonDAO, never()).save(any());
    }

    @Test
    void createWithUnknownAreaThrowsAndDoesNotSave() {
        when(riskAreaDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assistedPersonBO.create(request(99L, List.of(guardian("Ana Exemplo")))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Área de risco 99");
        verify(assistedPersonDAO, never()).save(any());
    }

    // ---- find ----

    @Test
    void findAllWithoutFilterListsEveryone() {
        when(assistedPersonDAO.findAllOrderByName()).thenReturn(List.of(saved(1L, area(1L, "Área A"), "Ana Exemplo")));

        assertThat(assistedPersonBO.findAll(null)).extracting(AssistedPersonResponse::name)
                .containsExactly("Benedita Exemplo");
        verify(assistedPersonDAO, never()).findByAreaIdOrderByName(any());
    }

    @Test
    void findAllWithAreaIdListsOnlyThatArea() {
        when(assistedPersonDAO.findByAreaIdOrderByName(2L)).thenReturn(List.of(saved(1L, area(2L, "Área B"), "Ana")));

        assertThat(assistedPersonBO.findAll(2L)).extracting(AssistedPersonResponse::areaName).containsExactly("Área B");
        verify(assistedPersonDAO, never()).findAllOrderByName();
    }

    @Test
    void findByIdReturnsPersonWithGuardians() {
        when(assistedPersonDAO.findById(1L)).thenReturn(Optional.of(saved(1L, area(1L, "Área A"), "Ana", "Bruno")));

        assertThat(assistedPersonBO.findById(1L).guardians()).extracting("name").containsExactly("Ana", "Bruno");
    }

    @Test
    void findByIdNotFoundThrows() {
        when(assistedPersonDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assistedPersonBO.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pessoa assistida 99");
    }

    // ---- update ----

    @Test
    void updateReplacesTheWholeGuardianListAndCanChangeTheArea() {
        AssistedPerson existing = saved(1L, area(1L, "Área A"), "Ana", "Bruno");
        Guardian oldAna = existing.getGuardians().get(0);
        RiskArea newArea = area(2L, "Área B");
        when(assistedPersonDAO.findById(1L)).thenReturn(Optional.of(existing));
        when(riskAreaDAO.findById(2L)).thenReturn(Optional.of(newArea));
        when(assistedPersonDAO.save(existing)).thenReturn(existing);

        AssistedPersonResponse response = assistedPersonBO.update(1L, request(2L, List.of(guardian("Carla"))));

        assertThat(response.areaName()).isEqualTo("Área B");
        assertThat(response.guardians()).extracting("name").containsExactly("Carla");
        // Quem saiu da lista perde a ligação (o orphanRemoval exclui do banco)
        assertThat(oldAna.getAssistedPerson()).isNull();
    }

    @Test
    void updateWithoutGuardiansBreaksRn03AndDoesNotSave() {
        when(assistedPersonDAO.findById(1L)).thenReturn(Optional.of(saved(1L, area(1L, "Área A"), "Ana")));
        when(riskAreaDAO.findById(1L)).thenReturn(Optional.of(area(1L, "Área A")));

        assertThatThrownBy(() -> assistedPersonBO.update(1L, request(1L, List.of())))
                .isInstanceOf(BusinessRuleException.class);
        verify(assistedPersonDAO, never()).save(any());
    }

    @Test
    void updateNotFoundThrows() {
        when(assistedPersonDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assistedPersonBO.update(99L, request(1L, List.of(guardian("Ana")))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(assistedPersonDAO, never()).save(any());
    }

    // ---- delete ----

    @Test
    void deleteRemovesPerson() {
        AssistedPerson existing = saved(1L, area(1L, "Área A"), "Ana");
        when(assistedPersonDAO.findById(1L)).thenReturn(Optional.of(existing));

        assistedPersonBO.delete(1L);

        verify(assistedPersonDAO).delete(existing);
    }

    @Test
    void deleteNotFoundThrows() {
        when(assistedPersonDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assistedPersonBO.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(assistedPersonDAO, never()).delete(any());
    }
}
