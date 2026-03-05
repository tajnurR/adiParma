package com.mailum.adi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "adi_medicine_generic",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_adi_medicine_generic_generic_code", columnNames = "generic_code"),
        @UniqueConstraint(name = "uk_adi_medicine_generic_slug", columnNames = "slug")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdiMedicineGeneric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "generic_code", nullable = false, unique = true, length = 100)
    private String genericCode;

    @Column(name = "generic_name", nullable = false, length = 255)
    private String genericName;

    @Column(name = "slug", nullable = true, unique = true, length = 255)
    private String slug;
}
