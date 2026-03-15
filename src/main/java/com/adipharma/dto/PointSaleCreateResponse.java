package com.adipharma.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PointSaleCreateResponse {

    public Long id;
    public String invoiceNo;
    public Integer customerId;
    public String customerName;
    public Integer paymentType;
    public String processedBy;
    public BigDecimal cashReceived;
    public BigDecimal changeAmount;
    public BigDecimal totalAmount;
    public LocalDateTime saleDate;
    public List<Item> items;

    public static class Item {
        public Long id;
        public Long medicineStockId;
        public String medicineName;
        public String medicineCode;
        public BigDecimal unitPrice;
        public Integer qty;
        public BigDecimal totalPrice;
        public BigDecimal discount;
        public String discountType;
    }
}
