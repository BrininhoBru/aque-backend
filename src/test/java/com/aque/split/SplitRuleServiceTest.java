package com.aque.split;

import com.aque.exception.BusinessException;
import com.aque.person.Person;
import com.aque.person.PersonRepository;
import com.aque.split.dto.request.SplitRuleItemRequest;
import com.aque.split.dto.request.SplitRuleRequest;
import com.aque.split.dto.response.SplitRuleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SplitRuleServiceTest {

    @Mock
    private SplitRuleRepository splitRuleRepository;

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private SplitRuleService service;

    private Person person1;
    private Person person2;

    @BeforeEach
    void setup() {
        person1 = new Person();
        person1.setId(UUID.randomUUID());
        person1.setName("Eu");

        person2 = new Person();
        person2.setId(UUID.randomUUID());
        person2.setName("Esposa");
    }

    @Test
    void save_somaDiferenteDe100_lancaBusinessException() {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(60)),
                new SplitRuleItemRequest(person2.getId(), BigDecimal.valueOf(30))
        ));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(splitRuleRepository, personRepository);
    }

    @Test
    void save_personIdDuplicado_lancaBusinessException400() {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(50)),
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(50))
        ));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(splitRuleRepository, personRepository);
    }

    @Test
    void save_pessoaNaoEncontrada_lancaBusinessException404() {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(100))
        ));

        when(personRepository.findById(person1.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void save_criaVersaoComEffectiveFromNoPrimeiroDiaDoMesAtual() {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(70)),
                new SplitRuleItemRequest(person2.getId(), BigDecimal.valueOf(30))
        ));

        when(splitRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.findById(person1.getId())).thenReturn(Optional.of(person1));
        when(personRepository.findById(person2.getId())).thenReturn(Optional.of(person2));

        SplitRuleResponse response = service.save(request);

        assertThat(response.effectiveFrom()).isEqualTo(LocalDate.now().withDayOfMonth(1));
        assertThat(response.items()).hasSize(2);
    }

    @Test
    void save_duasVezes_criaDuasVersoesDistintas() {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(100))
        ));

        when(splitRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.findById(person1.getId())).thenReturn(Optional.of(person1));

        service.save(request);
        service.save(request);

        ArgumentCaptor<SplitRule> captor = ArgumentCaptor.forClass(SplitRule.class);
        verify(splitRuleRepository, times(2)).save(captor.capture());

        List<SplitRule> saved = captor.getAllValues();
        assertThat(saved.get(0)).isNotSameAs(saved.get(1));
    }

    @Test
    void findByMonth_naoEncontrada_lancaBusinessException404() {
        when(splitRuleRepository.findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
                LocalDate.of(2026, 4, 1))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByMonth(2026, 4))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }
}
