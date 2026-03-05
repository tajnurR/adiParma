package com.mailum.adi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "adi_point_sales_details",
    indexes = {
        @Index(name = "idx_adi_point_sales_details_medicine_stock_id", columnList = "medicine_stock_id"),
        @Index(name = "idx_adi_point_sales_details_sales_master_id", columnList = "sales_master_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdiPointSalesDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "medicine_stock_id",
        referencedColumnName = "id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_stock_price",
            foreignKeyDefinition = "FOREIGN KEY (medicine_stock_id) REFERENCES adi_medicine_stock_price_mapping(id) ON DELETE RESTRICT"
        )
    )
    private AdiMedicineStockPriceMapping medicineStock;

    @Column(name = "sales_qty", nullable = false)
    private Integer salesQty;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "sales_master_id",
        referencedColumnName = "id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_sales_master",
            foreignKeyDefinition = "FOREIGN KEY (sales_master_id) REFERENCES adi_point_sales_master(id) ON DELETE CASCADE"
        )
    )
    private AdiPointSalesMaster salesMaster;
}
