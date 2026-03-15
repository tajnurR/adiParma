package com.adipharma.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "adi_point_sales_master",
    indexes = {
        @Index(name = "idx_adi_point_sales_master_customer_id", columnList = "customer_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdiPointSalesMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "invoice_no", nullable = false, unique = true, length = 30)
    private String invoiceNo;

    @Column(name = "payment_type", nullable = false)
    private Integer paymentType;

    @Column(name = "cash_received", precision = 12, scale = 2)
    private BigDecimal cashReceived;

    @Column(name = "change_amount", precision = 12, scale = 2)
    private BigDecimal changeAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "customer_id",
        referencedColumnName = "id",
        nullable = true,
        foreignKey = @ForeignKey(
            name = "fk_customer",
            foreignKeyDefinition = "FOREIGN KEY (customer_id) REFERENCES adi_customar(id) ON DELETE SET NULL"
        )
    )
    private AdiCustomar customer;

    @Column(name = "sale_date")
    private LocalDateTime saleDate;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_on")
    private LocalDateTime createdOn = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdOn == null) {
            createdOn = LocalDateTime.now();
        }
    }
}
