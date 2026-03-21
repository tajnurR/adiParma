package com.adipharma.service;

import com.adipharma.common.enums.PaymentMethod;
import com.adipharma.dto.InvoiceData;
import com.adipharma.entity.AdiCustomar;
import com.adipharma.entity.AdiMedicineDetails;
import com.adipharma.entity.AdiMedicineStockPriceMapping;
import com.adipharma.entity.AdiPointSalesDetails;
import com.adipharma.entity.AdiPointSalesMaster;
import com.adipharma.exception.ResourceNotFoundException;
import com.adipharma.repository.AdiPointSalesDetailsRepository;
import com.adipharma.repository.AdiPointSalesMasterRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceDataService {

    private static final DateTimeFormatter INVOICE_DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final AdiPointSalesMasterRepository masterRepository;
    private final AdiPointSalesDetailsRepository detailsRepository;

    @Value("${adipharma.pharmacy.name:AdiPharma Pharmacy}")
    private String pharmacyName;

    @Value("${adipharma.pharmacy.address:}")
    private String pharmacyAddress;

    @Value("${adipharma.pharmacy.phone:}")
    private String pharmacyPhone;

    @Value("${adipharma.pharmacy.email:}")
    private String pharmacyEmail;

    @Value("${adipharma.invoice.footer:Thank you for choosing AdiPharma Pharmacy. Please keep this invoice for your records.}")
    private String invoiceFooter;

    public InvoiceDataService(
        AdiPointSalesMasterRepository masterRepository,
        AdiPointSalesDetailsRepository detailsRepository
    ) {
        this.masterRepository = masterRepository;
        this.detailsRepository = detailsRepository;
    }

    @Transactional(readOnly = true)
    public InvoiceData getInvoiceData(Long invoiceId) {
        AdiPointSalesMaster master = masterRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
        List<AdiPointSalesDetails> details = detailsRepository.findBySalesMasterId(invoiceId);
        return buildInvoiceData(master, details);
    }

    private InvoiceData buildInvoiceData(
        AdiPointSalesMaster master,
        List<AdiPointSalesDetails> details
    ) {
        InvoiceData invoice = new InvoiceData();
        invoice.pharmacyName = pharmacyName;
        invoice.pharmacyAddress = blankToNull(pharmacyAddress);
        invoice.pharmacyPhone = blankToNull(pharmacyPhone);
        invoice.pharmacyEmail = blankToNull(pharmacyEmail);
        invoice.footerNote = invoiceFooter;
        invoice.invoiceNo = fallback(master.getInvoiceNo(), "—");
        invoice.invoiceDate = formatDate(master.getSaleDate());
        invoice.paymentMethod = formatPayment(master.getPaymentType());
        invoice.processedBy = fallback(master.getCreatedBy(), "—");

        AdiCustomar customer = master.getCustomer();
        invoice.billToName = customer != null ? fallback(customer.getName(), "Walk-in Customer") : "Walk-in Customer";
        invoice.billToContact = customer != null ? blankToNull(customer.getContact()) : null;
        invoice.billToAddress = customer != null ? blankToNull(customer.getAddress()) : null;

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;

        invoice.items = new java.util.ArrayList<>();
        for (AdiPointSalesDetails detail : details) {
            InvoiceData.Item item = new InvoiceData.Item();
            AdiMedicineStockPriceMapping stock = detail.getMedicineStock();
            AdiMedicineDetails medicine = stock != null ? stock.getMedicine() : null;
            item.name = fallback(buildMedicineName(medicine), "Item");
            item.code = medicine != null ? blankToNull(medicine.getBrandCode()) : null;

            BigDecimal unitPrice = stock != null && stock.getPrice() != null
                ? stock.getPrice()
                : BigDecimal.ZERO;
            int qty = detail.getSalesQty() == null ? 0 : detail.getSalesQty();
            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal lineTotal = detail.getTotalPrice() == null ? BigDecimal.ZERO : detail.getTotalPrice();
            BigDecimal lineDiscount = lineSubtotal.subtract(lineTotal);
            if (lineDiscount.compareTo(BigDecimal.ZERO) < 0) {
                lineDiscount = BigDecimal.ZERO;
            }

            subtotal = subtotal.add(lineSubtotal);
            discountTotal = discountTotal.add(lineDiscount);

            item.qty = String.valueOf(qty);
            item.unitPrice = formatMoney(unitPrice);
            item.discount = formatDiscount(detail.getDiscount(), detail.getDiscountType());
            item.total = formatMoney(lineTotal);
            invoice.items.add(item);
        }

        invoice.subtotal = formatMoney(subtotal);
        invoice.discountTotal = formatMoney(discountTotal);
        invoice.grandTotal = formatMoney(master.getTotalAmount());
        return invoice;
    }

    private String formatPayment(Integer paymentType) {
        PaymentMethod method = PaymentMethod.fromCode(paymentType);
        return method == null ? "—" : method.getLabel();
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "—";
        }
        return dateTime.format(INVOICE_DATE_FORMAT);
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return "৳" + safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDiscount(BigDecimal value, String type) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return "—";
        }
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("PERCENT") || normalized.equals("PERCENTAGE")) {
            return value.stripTrailingZeros().toPlainString() + "%";
        }
        return formatMoney(value);
    }

    private String buildMedicineName(AdiMedicineDetails medicine) {
        if (medicine == null) {
            return null;
        }
        String name = medicine.getBrandName() == null ? "" : medicine.getBrandName().trim();
        String strength = medicine.getStrength() == null ? "" : medicine.getStrength().trim();
        if (!strength.isEmpty()) {
            return name.isEmpty() ? strength : name + " " + strength;
        }
        return name.isEmpty() ? null : name;
    }

    private String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
