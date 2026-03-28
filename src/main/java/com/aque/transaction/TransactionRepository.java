package com.aque.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    boolean existsByRecurringIdAndReferenceMonthAndReferenceYear(
            UUID recurringId, Integer referenceMonth, Integer referenceYear);
}