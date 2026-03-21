package com.adipharma.controller;

import com.adipharma.dto.InvoiceData;
import com.adipharma.exception.ResourceNotFoundException;
import com.adipharma.service.InvoiceDataService;
import com.adipharma.service.InvoicePdfService;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class InvoiceController {

    private final InvoicePdfService invoicePdfService;
    private final InvoiceDataService invoiceDataService;

    public InvoiceController(
        InvoicePdfService invoicePdfService,
        InvoiceDataService invoiceDataService
    ) {
        this.invoicePdfService = invoicePdfService;
        this.invoiceDataService = invoiceDataService;
    }

    @GetMapping("/api/invoices/{id}/pdf")
    @ResponseBody
    public ResponseEntity<?> getInvoicePdf(
        @PathVariable("id") Long invoiceId,
        @RequestParam(name = "download", defaultValue = "false") boolean download
    ) {
        try {
            InvoicePdfService.InvoicePdfDocument doc = invoicePdfService.generateInvoicePdf(invoiceId);
            String safeInvoiceNo = doc.invoiceNo() == null ? "invoice" : doc.invoiceNo();
            String filename = safeInvoiceNo.replaceAll("[^a-zA-Z0-9-_\\.]", "_") + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(filename)
                .build());
            headers.setContentLength(doc.pdfBytes().length);

            return new ResponseEntity<>(doc.pdfBytes(), headers, HttpStatus.OK);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to generate invoice PDF."));
        }
    }

    @GetMapping("/invoices/{id}/thermal")
    public String getThermalReceipt(@PathVariable("id") Long invoiceId, Model model) {
        InvoiceData invoice = invoiceDataService.getInvoiceData(invoiceId);
        model.addAttribute("invoice", invoice);
        return "receipt-thermal";
    }
}
