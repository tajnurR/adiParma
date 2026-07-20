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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceDataService {

    private static final DateTimeFormatter INVOICE_DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final AdiPointSalesMasterRepository masterRepository;
    private final AdiPointSalesDetailsRepository detailsRepository;

    private final SystemSettingsService settingsService;

    public InvoiceDataService(
        AdiPointSalesMasterRepository masterRepository,
        AdiPointSalesDetailsRepository detailsRepository,
        SystemSettingsService settingsService
    ) {
        this.masterRepository = masterRepository;
        this.detailsRepository = detailsRepository;
        this.settingsService = settingsService;
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
        var settings = settingsService.getSettingsPayload();
        invoice.pharmacyName = (String) settings.get("pharmacyName");
        invoice.pharmacyAddress = (String) settings.get("pharmacyAddress");
        invoice.pharmacyPhone = (String) settings.get("pharmacyPhone");
        invoice.pharmacyEmail = (String) settings.get("pharmacyEmail");
        invoice.invoiceTitle = (String) settings.get("invoiceTitle");
        invoice.receiptTitle = (String) settings.get("receiptTitle");
        invoice.currencySymbol = (String) settings.get("currencySymbol");
        invoice.billToLabel = (String) settings.get("billToLabel");
        invoice.customerLabel = (String) settings.get("customerLabel");
        invoice.invoiceNoLabel = (String) settings.get("invoiceNoLabel");
        invoice.invoiceDateLabel = (String) settings.get("invoiceDateLabel");
        invoice.paymentLabelText = (String) settings.get("paymentLabel");
        invoice.processedByLabel = (String) settings.get("processedByLabel");
        invoice.itemLabel = (String) settings.get("itemLabel");
        invoice.qtyLabel = (String) settings.get("qtyLabel");
        invoice.unitPriceLabel = (String) settings.get("unitPriceLabel");
        invoice.discountLabel = (String) settings.get("discountLabel");
        invoice.amountLabel = (String) settings.get("amountLabel");
        invoice.subtotalLabel = (String) settings.get("subtotalLabel");
        invoice.grandTotalLabel = (String) settings.get("grandTotalLabel");
        invoice.footerNote = (String) settings.get("invoiceFooterNote");
        invoice.receiptFooterNote = (String) settings.get("receiptFooterNote");
        invoice.invoiceNo = fallback(master.getInvoiceNo(), "—");
        invoice.invoiceDate = formatDate(master.getSaleDate());
        invoice.paymentMethod = formatPayment(master.getPaymentType());
        invoice.processedBy = fallback(master.getCreatedBy(), "—");

        AdiCustomar customer = master.getCustomer();
        String walkInCustomerLabel = (String) settings.get("walkInCustomerLabel");
        invoice.billToName = customer != null
            ? fallback(customer.getName(), walkInCustomerLabel)
            : walkInCustomerLabel;
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
            item.unitPrice = formatMoney(unitPrice, invoice.currencySymbol);
            item.discount = formatDiscount(detail.getDiscount(), detail.getDiscountType(), invoice.currencySymbol);
            item.total = formatMoney(lineTotal, invoice.currencySymbol);
            invoice.items.add(item);
        }

        invoice.subtotal = formatMoney(subtotal, invoice.currencySymbol);
        invoice.discountTotal = formatMoney(discountTotal, invoice.currencySymbol);
        invoice.grandTotal = formatMoney(master.getTotalAmount(), invoice.currencySymbol);
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

    private String formatMoney(BigDecimal value, String currencySymbol) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return fallback(currencySymbol, "৳") + safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDiscount(BigDecimal value, String type, String currencySymbol) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return "—";
        }
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("PERCENT") || normalized.equals("PERCENTAGE")) {
            return value.stripTrailingZeros().toPlainString() + "%";
        }
        return formatMoney(value, currencySymbol);
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
