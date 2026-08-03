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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
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

        assertThatThrownBy(() -> service.save(2026, 3, request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(splitRuleRepository, personRepository);
    }

    @Test
    void save_pessoaNaoEncontrada_lancaBusinessException404() {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(100))
        ));

        when(splitRuleRepository.findByReferenceMonthAndReferenceYear(3, 2026))
                .thenReturn(Optional.empty());
        when(splitRuleRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.findById(person1.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(2026, 3, request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void save_regraNova_criaComItensInformados() {
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(70)),
                new SplitRuleItemRequest(person2.getId(), BigDecimal.valueOf(30))
        ));

        when(splitRuleRepository.findByReferenceMonthAndReferenceYear(3, 2026))
                .thenReturn(Optional.empty());
        when(splitRuleRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(splitRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.findById(person1.getId())).thenReturn(Optional.of(person1));
        when(personRepository.findById(person2.getId())).thenReturn(Optional.of(person2));

        SplitRuleResponse response = service.save(2026, 3, request);

        assertThat(response.referenceMonth()).isEqualTo(3);
        assertThat(response.referenceYear()).isEqualTo(2026);
        assertThat(response.items()).hasSize(2);
    }

    @Test
    void save_regraExistente_substituiItensAntigos() {
        SplitRule existing = new SplitRule();
        existing.setReferenceMonth(3);
        existing.setReferenceYear(2026);
        SplitRuleItem oldItem = new SplitRuleItem();
        oldItem.setSplitRule(existing);
        oldItem.setPerson(person2);
        oldItem.setPercentage(BigDecimal.valueOf(100));
        existing.getItems().add(oldItem);

        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(100))
        ));

        // captura o tamanho da coleção NO MOMENTO do flush — o objeto é mutável e
        // ganha os itens novos logo em seguida, então verificar depois do fato não prova nada
        List<Integer> itemCountAtFlushTime = new java.util.ArrayList<>();
        when(splitRuleRepository.findByReferenceMonthAndReferenceYear(3, 2026))
                .thenReturn(Optional.of(existing));
        when(splitRuleRepository.saveAndFlush(any())).thenAnswer(inv -> {
            SplitRule arg = inv.getArgument(0);
            itemCountAtFlushTime.add(arg.getItems().size());
            return arg;
        });
        when(splitRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.findById(person1.getId())).thenReturn(Optional.of(person1));

        SplitRuleResponse response = service.save(2026, 3, request);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().person().id()).isEqualTo(person1.getId());

        // flush precisa acontecer com a coleção já esvaziada, antes dos itens novos serem montados
        assertThat(itemCountAtFlushTime).containsExactly(0);
    }

    @Test
    void save_regraExistente_adicionaERemovePessoasNaMesmaRequisicao() {
        Person person3 = new Person();
        person3.setId(UUID.randomUUID());
        person3.setName("Filho");

        SplitRule existing = new SplitRule();
        existing.setReferenceMonth(3);
        existing.setReferenceYear(2026);
        SplitRuleItem item1 = new SplitRuleItem();
        item1.setSplitRule(existing);
        item1.setPerson(person1);
        item1.setPercentage(BigDecimal.valueOf(50));
        SplitRuleItem item2 = new SplitRuleItem();
        item2.setSplitRule(existing);
        item2.setPerson(person2);
        item2.setPercentage(BigDecimal.valueOf(50));
        existing.getItems().add(item1);
        existing.getItems().add(item2);

        // mantém person1, remove person2, adiciona person3 — person1 aparece nos
        // itens antigos E nos novos, que é justamente o caso que colide com
        // UNIQUE(split_rule_id, person_id) se o saveAndFlush() for removido
        var request = new SplitRuleRequest(List.of(
                new SplitRuleItemRequest(person1.getId(), BigDecimal.valueOf(40)),
                new SplitRuleItemRequest(person3.getId(), BigDecimal.valueOf(60))
        ));

        List<Integer> itemCountAtFlushTime = new java.util.ArrayList<>();
        when(splitRuleRepository.findByReferenceMonthAndReferenceYear(3, 2026))
                .thenReturn(Optional.of(existing));
        when(splitRuleRepository.saveAndFlush(any())).thenAnswer(inv -> {
            SplitRule arg = inv.getArgument(0);
            itemCountAtFlushTime.add(arg.getItems().size());
            return arg;
        });
        when(splitRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.findById(person1.getId())).thenReturn(Optional.of(person1));
        when(personRepository.findById(person3.getId())).thenReturn(Optional.of(person3));

        SplitRuleResponse response = service.save(2026, 3, request);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items())
                .extracting(item -> item.person().id())
                .containsExactlyInAnyOrder(person1.getId(), person3.getId());

        // flush precisa acontecer com a coleção já esvaziada, antes dos itens
        // novos (inclusive o de person1, que se repete) serem montados
        assertThat(itemCountAtFlushTime).containsExactly(0);
    }

    @Test
    void findByMonth_naoEncontrada_lancaBusinessException404() {
        when(splitRuleRepository.findByReferenceMonthAndReferenceYear(4, 2026))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByMonth(2026, 4))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }
}
