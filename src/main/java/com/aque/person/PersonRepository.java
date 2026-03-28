package com.aque.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    @Query("SELECT COUNT(i) > 0 FROM SplitRuleItem i WHERE i.person.id = :personId")
    boolean isLinkedToSplitRule(@Param("personId") UUID personId);
}