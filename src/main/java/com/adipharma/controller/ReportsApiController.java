package com.adipharma.controller;

import com.adipharma.service.ReportsService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportsApiController {

    private final ReportsService reportsService;

    public ReportsApiController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary(
        @RequestParam(name = "start", required = false) String start,
        @RequestParam(name = "end", required = false) String end
    ) {
        LocalDate startDate = parseDate(start, LocalDate.now().minusDays(30));
        LocalDate endDate = parseDate(end, LocalDate.now());
        return reportsService.getReport(startDate, endDate, 10, 30);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
        @RequestParam(name = "start", required = false) String start,
        @RequestParam(name = "end", required = false) String end
    ) {
        LocalDate startDate = parseDate(start, LocalDate.now().minusDays(30));
        LocalDate endDate = parseDate(end, LocalDate.now());
        Map<String, Object> report = reportsService.getReport(startDate, endDate, 10, 30);

        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value\n");
        csv.append("Total Sales,").append(report.get("totalSales")).append("\n");
        csv.append("Total Revenue,").append(report.get("totalRevenue")).append("\n");
        csv.append("Avg Order Value,").append(report.get("avgOrderValue")).append("\n");
        csv.append("Total Products,").append(report.get("totalProducts")).append("\n");
        csv.append("Stock Value,").append(report.get("stockValue")).append("\n");
        csv.append("Low Stock,").append(report.get("lowStock")).append("\n");
        csv.append("Expiring Soon,").append(report.get("expiringSoon")).append("\n\n");

        csv.append("Top Selling Products\n");
        csv.append("Product,Qty Sold,Revenue\n");
        Object topProductsObj = report.get("topProducts");
        if (topProductsObj instanceof Iterable<?> products) {
            for (Object item : products) {
                if (item instanceof Map<?, ?> row) {
                    csv.append(row.get("product")).append(",")
                        .append(row.get("qty")).append(",")
                        .append(row.get("revenue")).append("\n");
                }
            }
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"analytics-report.csv\"");
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private static LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return fallback;
        }
    }
}
