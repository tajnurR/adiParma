package com.adipharma.dto;

import java.math.BigDecimal;
import java.util.List;

public class PointSaleCreateRequest {

    public Integer customerId;
    public Integer paymentType;
    public BigDecimal cashReceived;
    public BigDecimal changeAmount;
    public BigDecimal totalAmount;
    public List<Item> items;

    public static class Item {
        public Long id;
        public Integer qty;
        public BigDecimal discount;
        public String discountType;
        public BigDecimal totalPrice;
    }
}
