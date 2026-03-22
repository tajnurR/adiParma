package com.adipharma.controller;

import com.adipharma.service.CustomerService;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping("/datatable")
    public Map<String, Object> datatable(
        @RequestParam(name = "draw", required = false, defaultValue = "0") int draw,
        @RequestParam(name = "start", required = false, defaultValue = "0") int start,
        @RequestParam(name = "length", required = false, defaultValue = "20") int length,
        @RequestParam(name = "search[value]", required = false, defaultValue = "") String search,
        @RequestParam(name = "order[0][column]", required = false, defaultValue = "3") int orderColumn,
        @RequestParam(name = "order[0][dir]", required = false, defaultValue = "desc") String orderDir
    ) {
        String sortField = switch (orderColumn) {
            case 0 -> "name";
            case 1 -> "contact";
            case 2 -> "address";
            case 3 -> "added";
            default -> "added";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(orderDir)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortField);

        Map<String, Object> response = customerService.datatable(search, start, length, sort);
        response.put("draw", draw);
        return response;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Integer id) {
        try {
            return ResponseEntity.ok(customerService.getById(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
        }
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

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable("id") Integer id,
        @RequestBody CreateCustomerRequest request
    ) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "name, phone, age, and address are required"));
        }
        try {
            return ResponseEntity.ok(
                customerService.update(id, request.name, request.phone, request.age, request.address)
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
