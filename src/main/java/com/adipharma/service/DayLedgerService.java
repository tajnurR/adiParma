package com.adipharma.service;

import com.adipharma.entity.AdiDayLedger;
import com.adipharma.repository.AdiDayLedgerRepository;
import com.adipharma.repository.AdiPointSalesMasterRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DayLedgerService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_CLOSED = "CLOSED";

    private final AdiDayLedgerRepository ledgerRepository;
    private final AdiPointSalesMasterRepository salesRepository;

    public DayLedgerService(
        AdiDayLedgerRepository ledgerRepository,
        AdiPointSalesMasterRepository salesRepository
    ) {
        this.ledgerRepository = ledgerRepository;
        this.salesRepository = salesRepository;
    }

    public Map<String, Object> getTodayStatus() {
        LocalDate today = LocalDate.now();
        AdiDayLedger ledger = ledgerRepository.findByBusinessDate(today).orElse(null);
        boolean open = ledger != null && STATUS_OPEN.equalsIgnoreCase(ledger.getStatus());
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("date", today);
        response.put("status", open ? STATUS_OPEN : STATUS_CLOSED);
        response.put("openedAt", ledger != null ? ledger.getOpenedAt() : null);
        response.put("closedAt", ledger != null ? ledger.getClosedAt() : null);
        response.put("openedBy", ledger != null ? ledger.getOpenedBy() : null);
        response.put("closedBy", ledger != null ? ledger.getClosedBy() : null);
        response.put("isOpen", open);
        return response;
    }

    public Map<String, Object> openDay() {
        LocalDate today = LocalDate.now();
        AdiDayLedger ledger = ledgerRepository.findByBusinessDate(today).orElse(null);
        if (ledger != null) {
            if (STATUS_OPEN.equalsIgnoreCase(ledger.getStatus())) {
                return getTodayStatus();
            }
            ledger.setStatus(STATUS_OPEN);
            ledger.setOpenedAt(LocalDateTime.now());
            ledger.setOpenedBy("admin");
            ledger.setClosedAt(null);
            ledger.setClosedBy(null);
            ledger.setTotalTransactions(0L);
            ledger.setTotalSales(BigDecimal.ZERO);
            ledger.setTotalCash(BigDecimal.ZERO);
            ledger.setTotalCard(BigDecimal.ZERO);
            ledger.setTotalMobile(BigDecimal.ZERO);
            ledger.setTotalOther(BigDecimal.ZERO);
            ledgerRepository.save(ledger);
            return getTodayStatus();
        }
        AdiDayLedger created = AdiDayLedger.builder()
            .businessDate(today)
            .status(STATUS_OPEN)
            .openedAt(LocalDateTime.now())
            .openedBy("admin")
            .build();
        ledgerRepository.save(created);
        return getTodayStatus();
    }

    public Map<String, Object> closeDay() {
        LocalDate today = LocalDate.now();
        AdiDayLedger ledger = ledgerRepository.findByBusinessDate(today)
            .orElseThrow(() -> new IllegalArgumentException("Day is not open yet."));
        if (!STATUS_OPEN.equalsIgnoreCase(ledger.getStatus())) {
            throw new IllegalArgumentException("Day is already closed.");
        }
        java.time.LocalTime nowTime = java.time.LocalTime.now();
        if (nowTime.isBefore(java.time.LocalTime.of(19, 0))) {
            throw new IllegalArgumentException("Day cannot be closed before 7:00 PM.");
        }

        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long totalTransactions = salesRepository.countSalesBetween(start, end);
        BigDecimal totalSales = salesRepository.sumRevenueBetween(start, end);
        BigDecimal totalCash = salesRepository.sumRevenueByPaymentBetween(1, start, end);
        BigDecimal totalCard = salesRepository.sumRevenueByPaymentBetween(2, start, end);
        BigDecimal totalMobile = salesRepository.sumRevenueByPaymentBetween(3, start, end);
        BigDecimal totalOther = salesRepository.sumRevenueByPaymentBetween(4, start, end);

        ledger.setTotalTransactions(totalTransactions);
        ledger.setTotalSales(totalSales);
        ledger.setTotalCash(totalCash);
        ledger.setTotalCard(totalCard);
        ledger.setTotalMobile(totalMobile);
        ledger.setTotalOther(totalOther);
        ledger.setClosedAt(LocalDateTime.now());
        ledger.setClosedBy("admin");
        ledger.setStatus(STATUS_CLOSED);

        ledgerRepository.save(ledger);
        return getTodayStatus();
    }

    public void ensureDayOpen() {
        LocalDate today = LocalDate.now();
        AdiDayLedger ledger = ledgerRepository.findByBusinessDate(today).orElse(null);
        if (ledger == null || !STATUS_OPEN.equalsIgnoreCase(ledger.getStatus())) {
            throw new IllegalArgumentException("Day is closed. Please open the day to start sales.");
        }
    }

    public java.util.List<Map<String, Object>> getRecentRecords(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 30));
        var page = org.springframework.data.domain.PageRequest.of(
            0,
            safeLimit,
            org.springframework.data.domain.Sort.by("businessDate").descending()
        );
        return ledgerRepository.findAll(page).stream()
            .map(ledger -> {
                Map<String, Object> row = new java.util.HashMap<>();
                row.put("businessDate", ledger.getBusinessDate());
                row.put("status", ledger.getStatus());
                row.put("openedAt", ledger.getOpenedAt());
                row.put("openedBy", ledger.getOpenedBy());
                row.put("closedAt", ledger.getClosedAt());
                row.put("closedBy", ledger.getClosedBy());
                row.put("totalSales", ledger.getTotalSales());
                return row;
            })
            .toList();
    }
}
