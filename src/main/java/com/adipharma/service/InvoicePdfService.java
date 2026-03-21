package com.adipharma.service;

import com.adipharma.dto.InvoiceData;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class InvoicePdfService {

    private final PdfGenerationService pdfGenerationService;
    private final InvoiceDataService invoiceDataService;

    public InvoicePdfService(
        PdfGenerationService pdfGenerationService,
        InvoiceDataService invoiceDataService
    ) {
        this.pdfGenerationService = pdfGenerationService;
        this.invoiceDataService = invoiceDataService;
    }

    public InvoicePdfDocument generateInvoicePdf(Long invoiceId) {
        InvoiceData invoiceData = invoiceDataService.getInvoiceData(invoiceId);
        byte[] pdf = pdfGenerationService.generateFromTemplate(
            "invoice-a4",
            Map.of("invoice", invoiceData)
        );
        return new InvoicePdfDocument(invoiceData.invoiceNo, pdf);
    }

    public record InvoicePdfDocument(String invoiceNo, byte[] pdfBytes) {
    }
}
