package com.aque.transaction;

import com.aque.category.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    boolean existsByRecurringIdAndReferenceMonthAndReferenceYear(UUID recurringId, Integer referenceMonth, Integer referenceYear);

    @Query("""
            SELECT COALESCE(SUM(t.amountExpected), 0)
            FROM Transaction t
            WHERE t.referenceMonth = :month
              AND t.referenceYear = :year
              AND t.type = :type
            """)
    BigDecimal sumExpected(@Param("month") int month,
                           @Param("year") int year,
                           @Param("type") CategoryType type);

    @Query("""
            SELECT COALESCE(SUM(t.amountPaid), 0)
            FROM Transaction t
            WHERE t.referenceMonth = :month
              AND t.referenceYear = :year
              AND t.type = :type
              AND t.status = :status
            """)
    BigDecimal sumPaid(@Param("month") int month,
                       @Param("year") int year,
                       @Param("type") CategoryType type,
                       @Param("status") TransactionStatus status);
}