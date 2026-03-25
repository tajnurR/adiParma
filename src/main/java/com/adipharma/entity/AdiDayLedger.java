package com.adipharma.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
    name = "adi_day_ledger",
    indexes = {
        @Index(name = "idx_adi_day_ledger_business_date", columnList = "business_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdiDayLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "opened_by", length = 100)
    private String openedBy;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Builder.Default
    @Column(name = "total_sales", precision = 12, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_transactions")
    private Long totalTransactions = 0L;

    @Builder.Default
    @Column(name = "total_cash", precision = 12, scale = 2)
    private BigDecimal totalCash = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_card", precision = 12, scale = 2)
    private BigDecimal totalCard = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_mobile", precision = 12, scale = 2)
    private BigDecimal totalMobile = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_other", precision = 12, scale = 2)
    private BigDecimal totalOther = BigDecimal.ZERO;
}
