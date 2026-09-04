package com.aque.person;

import com.aque.exception.BusinessException;
import com.aque.person.dto.request.PersonRequest;
import com.aque.person.dto.response.PersonResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private PersonService service;

    private Person person;

    @BeforeEach
    void setup() {
        person = new Person();
        person.setId(UUID.randomUUID());
        person.setName("Eu");
    }

    @Test
    void findAll_retornaTodasAsPessoas() {
        when(personRepository.findAll()).thenReturn(List.of(person));

        List<PersonResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Eu");
    }

    @Test
    void create_deveSalvarComNomeInformado() {
        var request = new PersonRequest("Esposa");
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonResponse response = service.create(request);

        assertThat(response.name()).isEqualTo("Esposa");
    }

    @Test
    void create_nomeDuplicado_lancaBusinessException400() {
        var request = new PersonRequest("eu");
        when(personRepository.existsByNameIgnoreCase("eu")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_pessoaExistente_atualizaComSucesso() {
        var request = new PersonRequest("Nome Atualizado");
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(personRepository.existsByNameIgnoreCaseAndIdNot("Nome Atualizado", person.getId())).thenReturn(false);
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonResponse response = service.update(person.getId(), request);

        assertThat(response.name()).isEqualTo("Nome Atualizado");
    }

    @Test
    void update_renomeiaParaOProprioNomeAtual_naoLancaExcecao() {
        var request = new PersonRequest("Eu");
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(personRepository.existsByNameIgnoreCaseAndIdNot("Eu", person.getId())).thenReturn(false);
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonResponse response = service.update(person.getId(), request);

        assertThat(response.name()).isEqualTo("Eu");
    }

    @Test
    void update_nomeDuplicadoDeOutraPessoa_lancaBusinessException400() {
        var request = new PersonRequest("Esposa");
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(personRepository.existsByNameIgnoreCaseAndIdNot("Esposa", person.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.update(person.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_pessoaNaoEncontrada_lancaBusinessException404() {
        UUID id = UUID.randomUUID();
        when(personRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new PersonRequest("X")))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_pessoaSemVinculo_removeComSucesso() {
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(personRepository.isLinkedToSplitRule(person.getId())).thenReturn(false);

        service.delete(person.getId());

        verify(personRepository).delete(person);
    }

    @Test
    void delete_pessoaVinculadaARegraDeDivisao_lancaBusinessException400() {
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(personRepository.isLinkedToSplitRule(person.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(person.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void delete_pessoaNaoEncontrada_lancaBusinessException404() {
        UUID id = UUID.randomUUID();
        when(personRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }
}
