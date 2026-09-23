package com.aque.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findByPersonId(UUID personId);

    List<Asset> findByExternalCode(String externalCode);

    List<Asset> findByNameIgnoreCaseAndTypeAndExternalCodeIsNull(String name, AssetType type);

    List<Asset> findByExternalCodeIsNotNull();

    @Query("SELECT COALESCE(SUM(a.currentValue), 0) FROM Asset a")
    BigDecimal sumCurrentValue();
}
