package com.aque.person;

import com.aque.exception.BusinessException;
import com.aque.person.dto.request.PersonRequest;
import com.aque.person.dto.response.PersonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    public List<PersonResponse> findAll() {
        return personRepository.findAll().stream()
                .map(PersonResponse::from)
                .toList();
    }

    public PersonResponse create(PersonRequest request) {
        validateNameNotDuplicated(request.name());

        Person person = new Person();
        person.setName(request.name());
        return PersonResponse.from(personRepository.save(person));
    }

    public PersonResponse update(UUID id, PersonRequest request) {
        Person person = findById(id);

        if (personRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new BusinessException(
                    "Já existe uma pessoa com esse nome",
                    HttpStatus.BAD_REQUEST
            );
        }

        person.setName(request.name());
        return PersonResponse.from(personRepository.save(person));
    }

    public void delete(UUID id) {
        Person person = findById(id);

        if (personRepository.isLinkedToSplitRule(id)) {
            throw new BusinessException(
                    "Pessoa vinculada a uma regra de divisão e não pode ser excluída",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (personRepository.isLinkedToAsset(id)) {
            throw new BusinessException(
                    "Pessoa vinculada a um ativo e não pode ser excluída",
                    HttpStatus.BAD_REQUEST
            );
        }

        personRepository.delete(person);
    }

    private void validateNameNotDuplicated(String name) {
        if (personRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(
                    "Já existe uma pessoa com esse nome",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private Person findById(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Pessoa não encontrada",
                        HttpStatus.NOT_FOUND
                ));
    }
}