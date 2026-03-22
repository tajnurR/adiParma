package com.adipharma.service;

import com.adipharma.entity.AdiMedicineDetails;
import com.adipharma.entity.AdiMedicineStockPriceMapping;
import com.adipharma.entity.AdiPointSalesDetails;
import com.adipharma.entity.AdiPointSalesMaster;
import com.adipharma.repository.AdiPointSalesDetailsRepository;
import com.adipharma.repository.AdiPointSalesMasterRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerSalesService {

    private final AdiPointSalesMasterRepository masterRepository;
    private final AdiPointSalesDetailsRepository detailsRepository;

    public CustomerSalesService(
        AdiPointSalesMasterRepository masterRepository,
        AdiPointSalesDetailsRepository detailsRepository
    ) {
        this.masterRepository = masterRepository;
        this.detailsRepository = detailsRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesForCustomer(Integer customerId) {
        List<AdiPointSalesMaster> masters = masterRepository
            .findByCustomerIdOrderBySaleDateDesc(customerId);

        String customerName = masters.isEmpty() || masters.get(0).getCustomer() == null
            ? null
            : masters.get(0).getCustomer().getName();

        List<Map<String, Object>> sales = masters.stream().map(master -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", master.getId());
            item.put("invoiceNo", master.getInvoiceNo());
            item.put("paymentType", master.getPaymentType());
            item.put("cashReceived", master.getCashReceived());
            item.put("changeAmount", master.getChangeAmount());
            item.put("totalAmount", master.getTotalAmount());
            item.put("saleDate", master.getSaleDate());
            item.put("createdBy", master.getCreatedBy());
            return item;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("customerId", customerId);
        response.put("customerName", customerName);
        response.put("sales", sales);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSaleDetails(Integer customerId, Long saleId) {
        Optional<AdiPointSalesMaster> master = masterRepository.findByIdAndCustomerId(saleId, customerId);
        if (master.isEmpty()) {
            return Map.of("saleId", saleId, "items", List.of());
        }

        List<AdiPointSalesDetails> details = detailsRepository.findBySalesMasterId(saleId);
        List<Map<String, Object>> items = details.stream().map(detail -> {
            Map<String, Object> item = new HashMap<>();
            AdiMedicineStockPriceMapping stock = detail.getMedicineStock();
            AdiMedicineDetails medicine = stock != null ? stock.getMedicine() : null;
            item.put("id", detail.getId());
            item.put("medicineName", medicine != null ? medicine.getBrandName() : null);
            item.put("medicineCode", medicine != null ? medicine.getBrandCode() : null);
            item.put("qty", detail.getSalesQty());
            item.put("unitPrice", stock != null ? stock.getPrice() : null);
            item.put("discount", detail.getDiscount());
            item.put("discountType", detail.getDiscountType());
            item.put("totalPrice", detail.getTotalPrice());
            return item;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("saleId", saleId);
        response.put("items", items);
        return response;
    }
}
