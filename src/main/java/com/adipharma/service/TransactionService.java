package com.adipharma.service;

import com.adipharma.common.enums.PaymentMethod;
import com.adipharma.entity.AdiMedicineDetails;
import com.adipharma.entity.AdiPointSalesDetails;
import com.adipharma.entity.AdiPointSalesMaster;
import com.adipharma.repository.AdiPointSalesDetailsRepository;
import com.adipharma.repository.AdiPointSalesMasterRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final AdiPointSalesMasterRepository masterRepository;
    private final AdiPointSalesDetailsRepository detailsRepository;

    public TransactionService(
        AdiPointSalesMasterRepository masterRepository,
        AdiPointSalesDetailsRepository detailsRepository
    ) {
        this.masterRepository = masterRepository;
        this.detailsRepository = detailsRepository;
    }

    public Map<String, Object> listTransactions(
        String query,
        Integer paymentType,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int size,
        String sortKey,
        String sortDir
    ) {
        String trimmed = query == null ? "" : query.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 50));
        String sortField = "saleDate";
        if ("total".equalsIgnoreCase(sortKey)) {
            sortField = "totalAmount";
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(direction, sortField));

        LocalDateTime start = startDate != null
            ? startDate.atStartOfDay()
            : LocalDate.of(1970, 1, 1).atStartOfDay();
        LocalDateTime end = endDate != null
            ? endDate.plusDays(1).atStartOfDay()
            : LocalDate.of(2999, 12, 31).atStartOfDay();

        var pageResult = masterRepository.searchTransactions(trimmed, paymentType, start, end, pageRequest);

        List<Map<String, Object>> items = pageResult.getContent().stream()
            .map(master -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", master.getId());
                row.put("invoiceNo", master.getInvoiceNo());
                row.put("customer", master.getCustomer() != null ? master.getCustomer().getName() : "Walk-in");
                row.put("payment", PaymentMethod.fromCode(master.getPaymentType()) != null
                    ? PaymentMethod.fromCode(master.getPaymentType()).getLabel()
                    : "—");
                row.put("totalAmount", master.getTotalAmount());
                row.put("saleDate", master.getSaleDate() != null ? master.getSaleDate() : master.getCreatedOn());
                return row;
            })
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", safePage);
        response.put("size", safeSize);
        response.put("total", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());
        return response;
    }

    public Map<String, Object> getTransaction(Long id) {
        AdiPointSalesMaster master = masterRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

        List<AdiPointSalesDetails> details = detailsRepository.findBySalesMasterId(id);
        List<Map<String, Object>> items = details.stream()
            .map(detail -> {
                Map<String, Object> row = new HashMap<>();
                AdiMedicineDetails medicine = detail.getMedicineStock() != null
                    ? detail.getMedicineStock().getMedicine()
                    : null;
                row.put("name", medicine != null ? medicine.getBrandName() : "Item");
                row.put("code", medicine != null ? medicine.getBrandCode() : "");
                row.put("qty", detail.getSalesQty());
                row.put("totalPrice", detail.getTotalPrice());
                return row;
            })
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("id", master.getId());
        response.put("invoiceNo", master.getInvoiceNo());
        response.put("customer", master.getCustomer() != null ? master.getCustomer().getName() : "Walk-in");
        response.put("payment", PaymentMethod.fromCode(master.getPaymentType()) != null
            ? PaymentMethod.fromCode(master.getPaymentType()).getLabel()
            : "—");
        response.put("totalAmount", master.getTotalAmount());
        response.put("saleDate", master.getSaleDate() != null ? master.getSaleDate() : master.getCreatedOn());
        response.put("items", items);
        return response;
    }
}
