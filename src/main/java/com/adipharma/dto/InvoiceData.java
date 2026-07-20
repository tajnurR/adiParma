package com.adipharma.dto;

import java.util.List;

public class InvoiceData {

    public String pharmacyName;
    public String pharmacyAddress;
    public String pharmacyPhone;
    public String pharmacyEmail;
    public String invoiceTitle;
    public String receiptTitle;
    public String currencySymbol;
    public String billToLabel;
    public String customerLabel;
    public String invoiceNoLabel;
    public String invoiceDateLabel;
    public String paymentLabelText;
    public String processedByLabel;
    public String itemLabel;
    public String qtyLabel;
    public String unitPriceLabel;
    public String discountLabel;
    public String amountLabel;
    public String subtotalLabel;
    public String grandTotalLabel;
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
    public String receiptFooterNote;
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
