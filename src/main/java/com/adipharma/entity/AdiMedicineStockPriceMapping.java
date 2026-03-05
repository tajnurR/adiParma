package com.adipharma.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "adi_medicine_stock_price_mapping",
    indexes = {
        @Index(name = "idx_adi_medicine_stock_price_mapping_medicine_id", columnList = "medicine_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdiMedicineStockPriceMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "medicine_id",
        referencedColumnName = "id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_medicine",
            foreignKeyDefinition = "FOREIGN KEY (medicine_id) REFERENCES adi_medicine_details(id) ON DELETE CASCADE"
        )
    )
    private AdiMedicineDetails medicine;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Builder.Default
    @Column(name = "add_date")
    private LocalDateTime addDate = LocalDateTime.now();

    @Column(name = "added_by", length = 100)
    private String addedBy;

    @PrePersist
    public void prePersist() {
        if (addDate == null) {
            addDate = LocalDateTime.now();
        }
    }
}
