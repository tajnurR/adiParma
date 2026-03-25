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

    @Column(name = "invoice_footer_note", length = 500)
    private String invoiceFooterNote;

    @Column(name = "receipt_footer_note", length = 500)
    private String receiptFooterNote;
}
