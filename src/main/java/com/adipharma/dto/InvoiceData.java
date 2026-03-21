package com.adipharma.dto;

import java.util.List;

public class InvoiceData {

    public String pharmacyName;
    public String pharmacyAddress;
    public String pharmacyPhone;
    public String pharmacyEmail;
    public String invoiceNo;
    public String invoiceDate;
    public String paymentMethod;
    public String processedBy;
    public String billToName;
    public String billToContact;
    public String billToAddress;
    public String subtotal;
    public String discountTotal;
    public String grandTotal;
    public String footerNote;
    public List<Item> items;

    public static class Item {
        public String name;
        public String code;
        public String qty;
        public String unitPrice;
        public String discount;
        public String total;
    }
}
