package com.mailum.adi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "adi_medicine_manufacturals",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_adi_medicine_manufacturals_manufacturer_code", columnNames = "manufacturer_code"),
        @UniqueConstraint(name = "uk_adi_medicine_manufacturals_slug", columnNames = "slug")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdiMedicineManufacturals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "manufacturer_code", nullable = false, unique = true, length = 100)
    private String manufacturerCode;

    @Column(name = "manufacturer_name", nullable = false, length = 255)
    private String manufacturerName;

    @Column(name = "slug", nullable = true, unique = true, length = 255)
    private String slug;

    @Builder.Default
    @Column(name = "generics_count")
    private Integer genericsCount = 0;

    @Builder.Default
    @Column(name = "brand_names_count")
    private Integer brandNamesCount = 0;
}
