package com.aque.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    @Query("SELECT COUNT(i) > 0 FROM SplitRuleItem i WHERE i.person.id = :personId")
    boolean isLinkedToSplitRule(@Param("personId") UUID personId);

    @Query("SELECT COUNT(a) > 0 FROM Asset a WHERE a.person.id = :personId")
    boolean isLinkedToAsset(@Param("personId") UUID personId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}