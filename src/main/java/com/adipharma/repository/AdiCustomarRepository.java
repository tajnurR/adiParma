package com.adipharma.repository;

import com.adipharma.entity.AdiCustomar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdiCustomarRepository extends JpaRepository<AdiCustomar, Integer> {
    Page<AdiCustomar> findByNameContainingIgnoreCaseOrContactContainingIgnoreCase(String name, String contact, Pageable pageable);
}
