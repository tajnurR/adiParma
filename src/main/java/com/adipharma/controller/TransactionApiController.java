package com.adipharma.controller;

import com.adipharma.service.TransactionService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionApiController {

    private final TransactionService transactionService;

    public TransactionApiController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public Map<String, Object> list(
        @RequestParam(name = "q", required = false, defaultValue = "") String query,
        @RequestParam(name = "payment", required = false) Integer payment,
        @RequestParam(name = "start", required = false) String start,
        @RequestParam(name = "end", required = false) String end,
        @RequestParam(name = "page", required = false, defaultValue = "0") int page,
        @RequestParam(name = "size", required = false, defaultValue = "15") int size,
        @RequestParam(name = "sort", required = false, defaultValue = "date") String sort,
        @RequestParam(name = "dir", required = false, defaultValue = "desc") String dir
    ) {
        LocalDate startDate = parseDate(start);
        LocalDate endDate = parseDate(end);
        return transactionService.listTransactions(query, payment, startDate, endDate, page, size, sort, dir);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(transactionService.getTransaction(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
