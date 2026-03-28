package com.aque.person.dto.response;

import com.aque.person.Person;

import java.util.UUID;

public record PersonResponse(
        UUID id,
        String name
) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName());
    }
}