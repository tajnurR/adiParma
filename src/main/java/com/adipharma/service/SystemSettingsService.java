package com.adipharma.service;

import com.adipharma.entity.AdiSystemSettings;
import com.adipharma.repository.AdiSystemSettingsRepository;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingsService {

    private final AdiSystemSettingsRepository repository;
    private volatile AdiSystemSettings cached;

    public SystemSettingsService(AdiSystemSettingsRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void loadCache() {
        cached = repository.findTopByOrderByIdAsc().orElseGet(this::createDefault);
    }

    public synchronized AdiSystemSettings getSettings() {
        AdiSystemSettings current = cached;
        if (current != null) {
            return current;
        }
        current = repository.findTopByOrderByIdAsc().orElseGet(this::createDefault);
        cached = current;
        return current;
    }

    @Transactional
    public synchronized AdiSystemSettings saveSettings(AdiSystemSettings incoming) {
        AdiSystemSettings settings = repository.findTopByOrderByIdAsc().orElseGet(this::createDefault);
        settings.setPharmacyName(trimOrNull(incoming.getPharmacyName()));
        settings.setPharmacyTagline(trimOrNull(incoming.getPharmacyTagline()));
        settings.setPharmacyAddress(trimOrNull(incoming.getPharmacyAddress()));
        settings.setPharmacyPhone(trimOrNull(incoming.getPharmacyPhone()));
        settings.setPharmacyEmail(trimOrNull(incoming.getPharmacyEmail()));
        settings.setInvoiceTitle(trimOrNull(incoming.getInvoiceTitle()));
        settings.setReceiptTitle(trimOrNull(incoming.getReceiptTitle()));
        settings.setCurrencySymbol(trimOrNull(incoming.getCurrencySymbol()));
        settings.setBillToLabel(trimOrNull(incoming.getBillToLabel()));
        settings.setWalkInCustomerLabel(trimOrNull(incoming.getWalkInCustomerLabel()));
        settings.setCustomerLabel(trimOrNull(incoming.getCustomerLabel()));
        settings.setInvoiceNoLabel(trimOrNull(incoming.getInvoiceNoLabel()));
        settings.setInvoiceDateLabel(trimOrNull(incoming.getInvoiceDateLabel()));
        settings.setPaymentLabel(trimOrNull(incoming.getPaymentLabel()));
        settings.setProcessedByLabel(trimOrNull(incoming.getProcessedByLabel()));
        settings.setItemLabel(trimOrNull(incoming.getItemLabel()));
        settings.setQtyLabel(trimOrNull(incoming.getQtyLabel()));
        settings.setUnitPriceLabel(trimOrNull(incoming.getUnitPriceLabel()));
        settings.setDiscountLabel(trimOrNull(incoming.getDiscountLabel()));
        settings.setAmountLabel(trimOrNull(incoming.getAmountLabel()));
        settings.setSubtotalLabel(trimOrNull(incoming.getSubtotalLabel()));
        settings.setGrandTotalLabel(trimOrNull(incoming.getGrandTotalLabel()));
        settings.setInvoiceFooterNote(trimOrNull(incoming.getInvoiceFooterNote()));
        settings.setReceiptFooterNote(trimOrNull(incoming.getReceiptFooterNote()));
        AdiSystemSettings saved = repository.save(settings);
        cached = saved;
        return saved;
    }

    public Map<String, Object> getSettingsPayload() {
        AdiSystemSettings settings = getSettings();
        Map<String, Object> response = new HashMap<>();
        response.put("pharmacyName", fallback(settings.getPharmacyName(), "AdiPharma Pharmacy"));
        response.put("pharmacyTagline", fallback(settings.getPharmacyTagline(), "Admin Panel"));
        response.put("pharmacyAddress", blankToNull(settings.getPharmacyAddress()));
        response.put("pharmacyPhone", blankToNull(settings.getPharmacyPhone()));
        response.put("pharmacyEmail", blankToNull(settings.getPharmacyEmail()));
        response.put("invoiceTitle", fallback(settings.getInvoiceTitle(), "INVOICE"));
        response.put("receiptTitle", fallback(settings.getReceiptTitle(), "SALES RECEIPT"));
        response.put("currencySymbol", fallback(settings.getCurrencySymbol(), "৳"));
        response.put("billToLabel", fallback(settings.getBillToLabel(), "Bill To"));
        response.put("walkInCustomerLabel", fallback(settings.getWalkInCustomerLabel(), "Walk-in Customer"));
        response.put("customerLabel", fallback(settings.getCustomerLabel(), "Customer"));
        response.put("invoiceNoLabel", fallback(settings.getInvoiceNoLabel(), "Invoice #"));
        response.put("invoiceDateLabel", fallback(settings.getInvoiceDateLabel(), "Date"));
        response.put("paymentLabel", fallback(settings.getPaymentLabel(), "Payment"));
        response.put("processedByLabel", fallback(settings.getProcessedByLabel(), "Processed by"));
        response.put("itemLabel", fallback(settings.getItemLabel(), "Item"));
        response.put("qtyLabel", fallback(settings.getQtyLabel(), "Qty"));
        response.put("unitPriceLabel", fallback(settings.getUnitPriceLabel(), "Unit Price"));
        response.put("discountLabel", fallback(settings.getDiscountLabel(), "Discount"));
        response.put("amountLabel", fallback(settings.getAmountLabel(), "Amount"));
        response.put("subtotalLabel", fallback(settings.getSubtotalLabel(), "Subtotal"));
        response.put("grandTotalLabel", fallback(settings.getGrandTotalLabel(), "Grand Total"));
        response.put(
            "invoiceFooterNote",
            fallback(
                settings.getInvoiceFooterNote(),
                "Thank you for choosing AdiPharma Pharmacy. Please keep this invoice for your records."
            )
        );
        response.put(
            "receiptFooterNote",
            fallback(
                settings.getReceiptFooterNote(),
                "Thank you for choosing AdiPharma Pharmacy."
            )
        );
        return response;
    }

    private AdiSystemSettings createDefault() {
        AdiSystemSettings settings = AdiSystemSettings.builder()
            .pharmacyName("AdiPharma Pharmacy")
            .pharmacyTagline("Admin Panel")
            .invoiceTitle("INVOICE")
            .receiptTitle("SALES RECEIPT")
            .currencySymbol("৳")
            .billToLabel("Bill To")
            .walkInCustomerLabel("Walk-in Customer")
            .customerLabel("Customer")
            .invoiceNoLabel("Invoice #")
            .invoiceDateLabel("Date")
            .paymentLabel("Payment")
            .processedByLabel("Processed by")
            .itemLabel("Item")
            .qtyLabel("Qty")
            .unitPriceLabel("Unit Price")
            .discountLabel("Discount")
            .amountLabel("Amount")
            .subtotalLabel("Subtotal")
            .grandTotalLabel("Grand Total")
            .invoiceFooterNote("Thank you for choosing AdiPharma Pharmacy. Please keep this invoice for your records.")
            .receiptFooterNote("Thank you for choosing AdiPharma Pharmacy.")
            .build();
        return repository.save(settings);
    }

    private String trimOrNull(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
