package com.aque.split;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SplitRuleRepository extends JpaRepository<SplitRule, UUID> {
    Optional<SplitRule> findByReferenceMonthAndReferenceYear(Integer referenceMonth, Integer referenceYear);
}