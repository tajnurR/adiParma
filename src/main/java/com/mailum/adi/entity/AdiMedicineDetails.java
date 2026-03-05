package com.mailum.adi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "adi_medicine_details",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_adi_medicine_details_brand_code", columnNames = "brand_code"),
        @UniqueConstraint(name = "uk_adi_medicine_details_slug", columnNames = "slug")
    },
    indexes = {
        @Index(name = "idx_adi_medicine_details_generic_id", columnList = "generic_id"),
        @Index(name = "idx_adi_medicine_details_manufacturer_id", columnList = "manufacturer_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdiMedicineDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "brand_code", nullable = false, unique = true, length = 100)
    private String brandCode;

    @Column(name = "brand_name", nullable = false, length = 255)
    private String brandName;

    @Column(name = "type", length = 100)
    private String type;

    @Column(name = "slug", nullable = true, unique = true, length = 255)
    private String slug;

    @Column(name = "dosage_form", length = 100)
    private String dosageForm;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "generic_id",
        referencedColumnName = "id",
        nullable = true,
        foreignKey = @ForeignKey(
            name = "fk_generic",
            foreignKeyDefinition = "FOREIGN KEY (generic_id) REFERENCES adi_medicine_generic(id) ON DELETE SET NULL ON UPDATE CASCADE"
        )
    )
    private AdiMedicineGeneric generic;

    @Column(name = "strength", length = 100)
    private String strength;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "manufacturer_id",
        referencedColumnName = "id",
        nullable = true,
        foreignKey = @ForeignKey(
            name = "fk_manufacturer",
            foreignKeyDefinition = "FOREIGN KEY (manufacturer_id) REFERENCES adi_medicine_manufacturals(id) ON DELETE SET NULL ON UPDATE CASCADE"
        )
    )
    private AdiMedicineManufacturals manufacturer;
}
