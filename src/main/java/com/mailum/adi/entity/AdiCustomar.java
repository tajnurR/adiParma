package com.mailum.adi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "adi_customar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdiCustomar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "contact", length = 100)
    private String contact;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Builder.Default
    @Column(name = "added")
    private LocalDateTime added = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (added == null) {
            added = LocalDateTime.now();
        }
    }
}
