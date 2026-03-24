package com.adipharma.repository;

import com.adipharma.entity.AdiPointSalesDetails;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface AdiPointSalesDetailsRepository extends JpaRepository<AdiPointSalesDetails, Long> {
    List<AdiPointSalesDetails> findBySalesMasterId(Long salesMasterId);

    @Query("""
        select med.brandName, sum(d.salesQty), sum(d.totalPrice)
        from AdiPointSalesDetails d
        join d.salesMaster m
        join d.medicineStock s
        join s.medicine med
        where m.saleDate >= :start and m.saleDate < :end
        group by med.id, med.brandName
        order by sum(d.salesQty) desc
        """)
    List<Object[]> findTopSellingProducts(
        @Param("start") java.time.LocalDateTime start,
        @Param("end") java.time.LocalDateTime end,
        Pageable pageable
    );
}
