package com.adipharma.controller;

import com.adipharma.service.CustomerService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerApiController {

    private final CustomerService customerService;

    public CustomerApiController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/search")
    public Map<String, Object> search(
        @RequestParam(name = "q", required = false, defaultValue = "") String query,
        @RequestParam(name = "page", required = false, defaultValue = "0") int page,
        @RequestParam(name = "size", required = false, defaultValue = "10") int size
    ) {
        return customerService.search(query, page, size);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateCustomerRequest request) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "name, phone, age, and address are required"));
        }
        try {
            return ResponseEntity.ok(
                customerService.create(request.name, request.phone, request.age, request.address)
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
        }
    }

    public static class CreateCustomerRequest {
        public String name;
        public String phone;
        public String age;
        public String address;
    }

}
