package com.adipharma.repository;

import com.adipharma.entity.AdiSystemSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdiSystemSettingsRepository extends JpaRepository<AdiSystemSettings, Long> {
    Optional<AdiSystemSettings> findTopByOrderByIdAsc();
}
