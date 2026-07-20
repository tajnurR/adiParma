package com.adipharma.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "adi_system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdiSystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "pharmacy_name", length = 200)
    private String pharmacyName;

    @Column(name = "pharmacy_tagline", length = 200)
    private String pharmacyTagline;

    @Column(name = "pharmacy_address", length = 500)
    private String pharmacyAddress;

    @Column(name = "pharmacy_phone", length = 100)
    private String pharmacyPhone;

    @Column(name = "pharmacy_email", length = 150)
    private String pharmacyEmail;

    @Column(name = "invoice_title", length = 100)
    private String invoiceTitle;

    @Column(name = "receipt_title", length = 100)
    private String receiptTitle;

    @Column(name = "currency_symbol", length = 20)
    private String currencySymbol;

    @Column(name = "bill_to_label", length = 100)
    private String billToLabel;

    @Column(name = "walk_in_customer_label", length = 150)
    private String walkInCustomerLabel;

    @Column(name = "customer_label", length = 100)
    private String customerLabel;

    @Column(name = "invoice_no_label", length = 100)
    private String invoiceNoLabel;

    @Column(name = "invoice_date_label", length = 100)
    private String invoiceDateLabel;

    @Column(name = "payment_label", length = 100)
    private String paymentLabel;

    @Column(name = "processed_by_label", length = 100)
    private String processedByLabel;

    @Column(name = "item_label", length = 100)
    private String itemLabel;

    @Column(name = "qty_label", length = 100)
    private String qtyLabel;

    @Column(name = "unit_price_label", length = 100)
    private String unitPriceLabel;

    @Column(name = "discount_label", length = 100)
    private String discountLabel;

    @Column(name = "amount_label", length = 100)
    private String amountLabel;

    @Column(name = "subtotal_label", length = 100)
    private String subtotalLabel;

    @Column(name = "grand_total_label", length = 100)
    private String grandTotalLabel;

    @Column(name = "invoice_footer_note", length = 500)
    private String invoiceFooterNote;

    @Column(name = "receipt_footer_note", length = 500)
    private String receiptFooterNote;
}
