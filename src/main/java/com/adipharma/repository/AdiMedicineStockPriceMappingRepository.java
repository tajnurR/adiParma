package com.adipharma.repository;

import com.adipharma.entity.AdiMedicineStockPriceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdiMedicineStockPriceMappingRepository extends JpaRepository<AdiMedicineStockPriceMapping, Long> {
    @EntityGraph(attributePaths = { "medicine", "medicine.generic", "medicine.manufacturer" })
    @Query("select m from AdiMedicineStockPriceMapping m")
    Page<AdiMedicineStockPriceMapping> findAllWithMedicine(Pageable pageable);

    @EntityGraph(attributePaths = { "medicine", "medicine.generic", "medicine.manufacturer" })
    @Query("""
        select m from AdiMedicineStockPriceMapping m
        join m.medicine med
        where lower(med.brandCode) like lower(concat('%', :query, '%'))
           or lower(med.brandName) like lower(concat('%', :query, '%'))
        """)
    Page<AdiMedicineStockPriceMapping> searchByBrandCodeOrName(@Param("query") String query, Pageable pageable);
}
