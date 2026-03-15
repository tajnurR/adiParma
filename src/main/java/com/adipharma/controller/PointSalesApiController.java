package com.adipharma.controller;

import com.adipharma.dto.PointSaleCreateRequest;
import com.adipharma.service.PointSalesService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/point-sales")
public class PointSalesApiController {

    private final PointSalesService service;

    public PointSalesApiController(PointSalesService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PointSaleCreateRequest request) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Sale payload is required."));
        }
        try {
            return ResponseEntity.ok(service.create(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
        }
    }
}
