package com.aque.transaction;

import com.aque.category.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
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

    @Query("""
            SELECT t.category, 
                   COALESCE(SUM(t.amountExpected), 0),
                   COALESCE(SUM(CASE WHEN t.status = 'PAGO' THEN t.amountPaid ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.referenceMonth = :month
              AND t.referenceYear = :year
              AND (:type IS NULL OR t.type = :type)
            GROUP BY t.category
            ORDER BY SUM(t.amountExpected) DESC
            """)
    List<Object[]> sumByCategory(@Param("month") int month,
                                 @Param("year") int year,
                                 @Param("type") CategoryType type);

    @Query("""
            SELECT t.referenceMonth,
                   COALESCE(SUM(CASE WHEN t.type = 'RECEITA' THEN t.amountExpected ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN t.type = 'RECEITA' AND t.status = 'PAGO' THEN t.amountPaid ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN t.type = 'DESPESA' THEN t.amountExpected ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN t.type = 'DESPESA' AND t.status = 'PAGO' THEN t.amountPaid ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.referenceYear = :year
            GROUP BY t.referenceMonth
            ORDER BY t.referenceMonth
            """)
    List<Object[]> evolutionByYear(@Param("year") int year);
}