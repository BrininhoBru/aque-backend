package com.aque.split;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "split_rules")
@Getter
@Setter
@NoArgsConstructor
public class SplitRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_month", nullable = false)
    private Integer referenceMonth;

    @Column(name = "reference_year", nullable = false)
    private Integer referenceYear;

    @OneToMany(mappedBy = "splitRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SplitRuleItem> items = new ArrayList<>();
}