package com.aque.recurring;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "recurring_generations")
@Getter
@Setter
@NoArgsConstructor
public class RecurringGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recurring_id", nullable = false)
    private UUID recurringId;

    @Column(name = "reference_month", nullable = false)
    private Integer referenceMonth;

    @Column(name = "reference_year", nullable = false)
    private Integer referenceYear;
}
