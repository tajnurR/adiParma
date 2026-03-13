package com.adipharma.controller;

import com.adipharma.entity.AdiCustomar;
import com.adipharma.repository.AdiCustomarRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    private final AdiCustomarRepository customarRepository;

    public CustomerApiController(AdiCustomarRepository customarRepository) {
        this.customarRepository = customarRepository;
    }

    @GetMapping("/search")
    public Map<String, Object> search(
        @RequestParam(name = "q", required = false, defaultValue = "") String query,
        @RequestParam(name = "page", required = false, defaultValue = "0") int page,
        @RequestParam(name = "size", required = false, defaultValue = "10") int size
    ) {
        String trimmed = query == null ? "" : query.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 10));

        Map<String, Object> response = new HashMap<>();
        if (trimmed.isEmpty()) {
            response.put("items", List.of());
            response.put("page", safePage);
            response.put("size", safeSize);
            response.put("hasMore", false);
            response.put("total", 0);
            return response;
        }

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by("name").ascending());
        Page<AdiCustomar> results = customarRepository
            .findByNameContainingIgnoreCaseOrContactContainingIgnoreCase(trimmed, trimmed, pageRequest);

        List<Map<String, Object>> items = results.stream().map(customer -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", customer.getId());
            item.put("name", customer.getName());
            item.put("phone", customer.getContact());
            item.put("age", customer.getAge());
            item.put("address", customer.getAddress());
            return item;
        }).toList();

        response.put("items", items);
        response.put("page", safePage);
        response.put("size", safeSize);
        response.put("hasMore", results.hasNext());
        response.put("total", results.getTotalElements());
        return response;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateCustomerRequest request) {
        if (request == null || isBlank(request.name) || isBlank(request.phone) || request.age == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "name, phone, and age are required"));
        }

        AdiCustomar customar = AdiCustomar.builder()
            .name(request.name.trim())
            .contact(request.phone.trim())
            .age(request.age)
            .address(Objects.requireNonNullElse(request.address, "").trim())
            .build();

        AdiCustomar saved = customarRepository.save(customar);

        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("name", saved.getName());
        response.put("phone", saved.getContact());
        response.put("age", saved.getAge());
        response.put("address", saved.getAddress());
        return ResponseEntity.ok(response);
    }

    public static class CreateCustomerRequest {
        public String name;
        public String phone;
        public Integer age;
        public String address;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
