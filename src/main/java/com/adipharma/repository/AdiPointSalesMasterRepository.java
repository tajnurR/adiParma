package com.adipharma.repository;

import com.adipharma.entity.AdiPointSalesMaster;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdiPointSalesMasterRepository extends JpaRepository<AdiPointSalesMaster, Long> {
    Optional<AdiPointSalesMaster> findTopByInvoiceNoStartingWithOrderByInvoiceNoDesc(String prefix);
    List<AdiPointSalesMaster> findByCustomerIdOrderBySaleDateDesc(Integer customerId);
    Optional<AdiPointSalesMaster> findByIdAndCustomerId(Long id, Integer customerId);

    @Query("""
        select count(m) from AdiPointSalesMaster m
        where m.saleDate >= :start and m.saleDate < :end
        """)
    long countSalesBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("""
        select coalesce(sum(m.totalAmount), 0) from AdiPointSalesMaster m
        where m.saleDate >= :start and m.saleDate < :end
        """)
    BigDecimal sumRevenueBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}
