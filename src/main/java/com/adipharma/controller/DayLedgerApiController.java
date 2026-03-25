package com.adipharma.controller;

import com.adipharma.service.DayLedgerService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/day")
public class DayLedgerApiController {

    private final DayLedgerService dayLedgerService;

    public DayLedgerApiController(DayLedgerService dayLedgerService) {
        this.dayLedgerService = dayLedgerService;
    }

    @GetMapping("status")
    public Map<String, Object> getStatus() {
        return dayLedgerService.getTodayStatus();
    }

    @GetMapping("records")
    public java.util.List<Map<String, Object>> getRecords(
        @org.springframework.web.bind.annotation.RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return dayLedgerService.getRecentRecords(limit);
    }

    @PostMapping("open")
    public ResponseEntity<?> openDay() {
        try {
            return ResponseEntity.ok(dayLedgerService.openDay());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("close")
    public ResponseEntity<?> closeDay() {
        try {
            return ResponseEntity.ok(dayLedgerService.closeDay());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
        }
    }
}
