package com.adipharma.repository;

import com.adipharma.entity.AdiMedicineStockPriceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

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

    @EntityGraph(attributePaths = { "medicine", "medicine.generic", "medicine.manufacturer" })
    @Query("""
        select m from AdiMedicineStockPriceMapping m
        join m.medicine med
        left join med.generic gen
        left join med.manufacturer man
        where (:query = '' or lower(med.brandCode) like lower(concat('%', :query, '%'))
           or lower(med.brandName) like lower(concat('%', :query, '%'))
           or lower(gen.genericName) like lower(concat('%', :query, '%'))
           or lower(man.manufacturerName) like lower(concat('%', :query, '%')))
          and (:category = '' or lower(med.type) = lower(:category))
        """)
    Page<AdiMedicineStockPriceMapping> searchCatalog(
        @Param("query") String query,
        @Param("category") String category,
        Pageable pageable
    );

    @Query("""
        select distinct med.type from AdiMedicineStockPriceMapping m
        join m.medicine med
        where med.type is not null and med.type <> ''
        order by med.type
        """)
    List<String> findDistinctTypes();
}
