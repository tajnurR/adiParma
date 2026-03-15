package com.adipharma.repository;

import com.adipharma.entity.AdiPointSalesMaster;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdiPointSalesMasterRepository extends JpaRepository<AdiPointSalesMaster, Long> {
    Optional<AdiPointSalesMaster> findTopByInvoiceNoStartingWithOrderByInvoiceNoDesc(String prefix);
}
