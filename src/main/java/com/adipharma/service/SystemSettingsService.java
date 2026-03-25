package com.adipharma.service;

import com.adipharma.entity.AdiSystemSettings;
import com.adipharma.repository.AdiSystemSettingsRepository;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingsService {

    private static final long SETTINGS_ID = 1L;

    private final AdiSystemSettingsRepository repository;
    private volatile AdiSystemSettings cached;

    public SystemSettingsService(AdiSystemSettingsRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void loadCache() {
        cached = repository.findById(SETTINGS_ID).orElse(null);
    }

    public AdiSystemSettings getSettings() {
        AdiSystemSettings current = cached;
        if (current != null) {
            return current;
        }
        current = repository.findById(SETTINGS_ID).orElseGet(this::createDefault);
        cached = current;
        return current;
    }

    @Transactional
    public AdiSystemSettings saveSettings(AdiSystemSettings incoming) {
        AdiSystemSettings settings = repository.findById(SETTINGS_ID).orElseGet(this::createDefault);
        settings.setPharmacyName(trimOrNull(incoming.getPharmacyName()));
        settings.setPharmacyTagline(trimOrNull(incoming.getPharmacyTagline()));
        settings.setPharmacyAddress(trimOrNull(incoming.getPharmacyAddress()));
        settings.setPharmacyPhone(trimOrNull(incoming.getPharmacyPhone()));
        settings.setPharmacyEmail(trimOrNull(incoming.getPharmacyEmail()));
        settings.setInvoiceFooterNote(trimOrNull(incoming.getInvoiceFooterNote()));
        settings.setReceiptFooterNote(trimOrNull(incoming.getReceiptFooterNote()));
        AdiSystemSettings saved = repository.save(settings);
        cached = saved;
        return saved;
    }

    public Map<String, Object> getSettingsPayload() {
        AdiSystemSettings settings = getSettings();
        return Map.of(
            "pharmacyName", fallback(settings.getPharmacyName(), "AdiPharma Pharmacy"),
            "pharmacyTagline", fallback(settings.getPharmacyTagline(), "Admin Panel"),
            "pharmacyAddress", blankToNull(settings.getPharmacyAddress()),
            "pharmacyPhone", blankToNull(settings.getPharmacyPhone()),
            "pharmacyEmail", blankToNull(settings.getPharmacyEmail()),
            "invoiceFooterNote", fallback(
                settings.getInvoiceFooterNote(),
                "Thank you for choosing AdiPharma Pharmacy. Please keep this invoice for your records."
            ),
            "receiptFooterNote", fallback(
                settings.getReceiptFooterNote(),
                "Thank you for choosing AdiPharma Pharmacy."
            )
        );
    }

    private AdiSystemSettings createDefault() {
        AdiSystemSettings settings = AdiSystemSettings.builder()
            .id(SETTINGS_ID)
            .pharmacyName("AdiPharma Pharmacy")
            .pharmacyTagline("Admin Panel")
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
