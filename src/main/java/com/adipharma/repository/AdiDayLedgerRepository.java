package com.adipharma.repository;

import com.adipharma.entity.AdiDayLedger;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdiDayLedgerRepository extends JpaRepository<AdiDayLedger, Long> {
    Optional<AdiDayLedger> findByBusinessDate(LocalDate businessDate);
}
