package com.adipharma.repository;

import com.adipharma.entity.AdiPointSalesDetails;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdiPointSalesDetailsRepository extends JpaRepository<AdiPointSalesDetails, Long> {
    List<AdiPointSalesDetails> findBySalesMasterId(Long salesMasterId);
}
