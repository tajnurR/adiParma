package com.adipharma.service;

import com.adipharma.common.enums.PaymentMethod;
import com.adipharma.dto.PointSaleCreateRequest;
import com.adipharma.dto.PointSaleCreateResponse;
import com.adipharma.entity.AdiCustomar;
import com.adipharma.entity.AdiMedicineDetails;
import com.adipharma.entity.AdiMedicineStockPriceMapping;
import com.adipharma.entity.AdiPointSalesDetails;
import com.adipharma.entity.AdiPointSalesMaster;
import com.adipharma.repository.AdiCustomarRepository;
import com.adipharma.repository.AdiMedicineStockPriceMappingRepository;
import com.adipharma.repository.AdiPointSalesDetailsRepository;
import com.adipharma.repository.AdiPointSalesMasterRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointSalesService {

    private static final DateTimeFormatter INVOICE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int INVOICE_SEQUENCE_WIDTH = 5;

    private final Object invoiceLock = new Object();
    private final AdiPointSalesMasterRepository masterRepository;
    private final AdiPointSalesDetailsRepository detailsRepository;
    private final AdiCustomarRepository customarRepository;
    private final AdiMedicineStockPriceMappingRepository stockRepository;

    public PointSalesService(
        AdiPointSalesMasterRepository masterRepository,
        AdiPointSalesDetailsRepository detailsRepository,
        AdiCustomarRepository customarRepository,
        AdiMedicineStockPriceMappingRepository stockRepository
    ) {
        this.masterRepository = masterRepository;
        this.detailsRepository = detailsRepository;
        this.customarRepository = customarRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public PointSaleCreateResponse create(PointSaleCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.customerId == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        if (request.items == null || request.items.isEmpty()) {
            throw new IllegalArgumentException("At least one cart item is required.");
        }
        if (request.paymentType == null || PaymentMethod.fromCode(request.paymentType) == null) {
            throw new IllegalArgumentException("Valid payment type is required.");
        }
        if (request.cashReceived == null || request.cashReceived.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cash received is required.");
        }
        if (request.changeAmount == null || request.changeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Change amount is required.");
        }
        if (request.totalAmount == null || request.totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount is required.");
        }

        AdiCustomar customer = customarRepository.findById(request.customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found."));

        List<Long> stockIds = request.items.stream()
            .map(item -> item == null ? null : item.id)
            .filter(id -> id != null)
            .toList();
        if (stockIds.size() != request.items.size()) {
            throw new IllegalArgumentException("Each cart item must include a medicine stock id.");
        }

        List<AdiMedicineStockPriceMapping> stockItems = stockRepository.findAllById(stockIds);
        Map<Long, AdiMedicineStockPriceMapping> stockMap = stockItems.stream()
            .collect(Collectors.toMap(AdiMedicineStockPriceMapping::getId, item -> item));
        if (stockMap.size() != stockIds.size()) {
            throw new IllegalArgumentException("One or more medicine stock items were not found.");
        }

        AdiPointSalesMaster savedMaster;
        synchronized (invoiceLock) {
            savedMaster = saveMasterWithInvoice(request, customer);
        }

        List<AdiPointSalesDetails> detailEntities = new ArrayList<>();
        for (PointSaleCreateRequest.Item item : request.items) {
            if (item.qty == null || item.qty <= 0) {
                throw new IllegalArgumentException("Each cart item must include a valid quantity.");
            }
            if (item.totalPrice == null || item.totalPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Each cart item must include a valid total price.");
            }
            String discountType = normalizeDiscountType(item.discountType);
            if (discountType == null) {
                throw new IllegalArgumentException("Each cart item must include a valid discount type.");
            }
            BigDecimal discount = item.discount == null ? BigDecimal.ZERO : item.discount;

            AdiPointSalesDetails detail = AdiPointSalesDetails.builder()
                .medicineStock(stockMap.get(item.id))
                .salesQty(item.qty)
                .totalPrice(item.totalPrice)
                .discount(discount)
                .discountType(discountType)
                .salesMaster(savedMaster)
                .build();
            detailEntities.add(detail);
        }

        List<AdiPointSalesDetails> savedDetails = detailsRepository.saveAll(detailEntities);
        updateStockQuantities(request.items, stockMap);

        return buildResponse(savedMaster, savedDetails);
    }

    private void updateStockQuantities(
        List<PointSaleCreateRequest.Item> items,
        Map<Long, AdiMedicineStockPriceMapping> stockMap
    ) {
        for (PointSaleCreateRequest.Item item : items) {
            AdiMedicineStockPriceMapping stock = stockMap.get(item.id);
            if (stock == null) {
                continue;
            }
            int currentQty = stock.getQty() == null ? 0 : stock.getQty();
            int soldQty = item.qty == null ? 0 : item.qty;
            int newQty = currentQty - soldQty;
            stock.setQty(Math.max(newQty, 0));
        }
        stockRepository.saveAll(stockMap.values());
    }

    private AdiPointSalesMaster saveMasterWithInvoice(PointSaleCreateRequest request, AdiCustomar customer) {
        int attempts = 0;
        while (attempts < 3) {
            attempts++;
            AdiPointSalesMaster master = AdiPointSalesMaster.builder()
                .invoiceNo(generateInvoiceNo())
                .customer(customer)
                .paymentType(request.paymentType)
                .cashReceived(request.cashReceived)
                .changeAmount(request.changeAmount)
                .totalAmount(request.totalAmount)
                .saleDate(LocalDateTime.now())
                .build();
            try {
                return masterRepository.saveAndFlush(master);
            } catch (DataIntegrityViolationException ex) {
                if (attempts >= 3) {
                    throw new IllegalArgumentException("Failed to generate a unique invoice number.");
                }
            }
        }
        throw new IllegalArgumentException("Failed to generate a unique invoice number.");
    }

    private String generateInvoiceNo() {
        LocalDate today = LocalDate.now();
        String datePart = today.format(INVOICE_DATE_FORMAT);
        String prefix = "I" + datePart + "-";
        Optional<AdiPointSalesMaster> latest = masterRepository
            .findTopByInvoiceNoStartingWithOrderByInvoiceNoDesc(prefix);
        int nextSequence = 1;
        if (latest.isPresent()) {
            String invoiceNo = latest.get().getInvoiceNo();
            if (invoiceNo != null && invoiceNo.startsWith(prefix) && invoiceNo.length() > prefix.length()) {
                String suffix = invoiceNo.substring(prefix.length());
                try {
                    nextSequence = Integer.parseInt(suffix) + 1;
                } catch (NumberFormatException ignored) {
                    nextSequence = 1;
                }
            }
        }
        return prefix + String.format("%0" + INVOICE_SEQUENCE_WIDTH + "d", nextSequence);
    }

    private String normalizeDiscountType(String discountType) {
        if (discountType == null) {
            return null;
        }
        String value = discountType.trim().toUpperCase(Locale.ROOT);
        if (value.equals("PERCENT") || value.equals("PERCENTAGE")) {
            return "PERCENT";
        }
        if (value.equals("BDT") || value.equals("AMOUNT")) {
            return "BDT";
        }
        return null;
    }

    private PointSaleCreateResponse buildResponse(
        AdiPointSalesMaster master,
        List<AdiPointSalesDetails> details
    ) {
        PointSaleCreateResponse response = new PointSaleCreateResponse();
        response.id = master.getId();
        response.invoiceNo = master.getInvoiceNo();
        response.customerId = master.getCustomer() != null ? master.getCustomer().getId() : null;
        response.customerName = master.getCustomer() != null ? master.getCustomer().getName() : null;
        response.paymentType = master.getPaymentType();
        response.processedBy = master.getCreatedBy();
        response.cashReceived = master.getCashReceived();
        response.changeAmount = master.getChangeAmount();
        response.totalAmount = master.getTotalAmount();
        response.saleDate = master.getSaleDate();

        List<PointSaleCreateResponse.Item> items = details.stream()
            .map(detail -> {
                PointSaleCreateResponse.Item item = new PointSaleCreateResponse.Item();
                item.id = detail.getId();
                item.medicineStockId = detail.getMedicineStock() != null ? detail.getMedicineStock().getId() : null;
                AdiMedicineStockPriceMapping stock = detail.getMedicineStock();
                AdiMedicineDetails medicine = stock != null ? stock.getMedicine() : null;
                item.medicineName = buildMedicineName(medicine);
                item.medicineCode = medicine != null ? medicine.getBrandCode() : null;
                item.unitPrice = stock != null ? stock.getPrice() : null;
                item.qty = detail.getSalesQty();
                item.totalPrice = detail.getTotalPrice();
                item.discount = detail.getDiscount();
                item.discountType = detail.getDiscountType();
                return item;
            })
            .toList();
        response.items = items;
        return response;
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
}
