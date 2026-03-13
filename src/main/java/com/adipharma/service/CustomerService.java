package com.adipharma.service;

import com.adipharma.entity.AdiCustomar;
import com.adipharma.repository.AdiCustomarRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final AdiCustomarRepository customarRepository;

    public CustomerService(AdiCustomarRepository customarRepository) {
        this.customarRepository = customarRepository;
    }

    public Map<String, Object> search(String query, int page, int size) {
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

    public Map<String, Object> create(String name, String phone, String age, String address) {
        if (isBlank(name) || isBlank(phone) || isBlank(age) || isBlank(address)) {
            throw new IllegalArgumentException("name, phone, age, and address are required");
        }
        if (!age.matches("^\\d{1,3}$")) {
            throw new IllegalArgumentException("age must be 1 to 3 digits");
        }

        AdiCustomar customar = AdiCustomar.builder()
            .name(name.trim())
            .contact(phone.trim())
            .age(age)
            .address(address.trim())
            .build();

        AdiCustomar saved = customarRepository.save(customar);

        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("name", saved.getName());
        response.put("phone", saved.getContact());
        response.put("age", saved.getAge());
        response.put("address", saved.getAddress());
        return response;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
