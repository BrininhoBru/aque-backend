package com.aque.split;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // mês a partir do qual esta versão passa a valer (dia sempre normalizado pro 1º)
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    // só pra desempatar quando duas versões têm o mesmo effectiveFrom (editar duas
    // vezes no mesmo mês) — UUID não serve como critério de "mais recente"
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "splitRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SplitRuleItem> items = new ArrayList<>();
}
