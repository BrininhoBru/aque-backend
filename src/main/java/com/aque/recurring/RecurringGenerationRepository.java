package com.aque.recurring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecurringGenerationRepository extends JpaRepository<RecurringGeneration, UUID> {

    boolean existsByRecurringIdAndReferenceMonthAndReferenceYear(UUID recurringId, Integer referenceMonth, Integer referenceYear);
}
