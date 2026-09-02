package com.aque.split;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface SplitRuleRepository extends JpaRepository<SplitRule, UUID> {

    Optional<SplitRule> findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(LocalDate monthStart);
}
