package com.adipharma.service;

import com.adipharma.entity.AdiMedicineStockPriceMapping;
import com.adipharma.repository.AdiMedicineStockPriceMappingRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class MedicineStockPriceMappingService {

    private static final int MAX_RESULTS = 50;
    private static final int MAX_CATALOG_PAGE_SIZE = 100;

    private final AdiMedicineStockPriceMappingRepository repository;

    public MedicineStockPriceMappingService(AdiMedicineStockPriceMappingRepository repository) {
        this.repository = repository;
    }

    public List<AdiMedicineStockPriceMapping> getMedicineStockDetailsWithLimit(String query) {
        String trimmed = query == null ? "" : query.trim();
        PageRequest pageRequest = PageRequest.of(0, MAX_RESULTS, Sort.by("id").descending());
        if (trimmed.isEmpty()) {
            return repository.findAllWithMedicine(pageRequest).getContent();
        }
        return repository.searchByBrandCodeOrName(trimmed, pageRequest).getContent();
    }

    public Map<String, Object> getCatalog(
        String query,
        String category,
        int page,
        int size,
        String sortKey,
        String sortDir
    ) {
        String trimmedQuery = query == null ? "" : query.trim();
        String trimmedCategory = category == null ? "" : category.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_CATALOG_PAGE_SIZE));

        String sortField = switch (sortKey == null ? "" : sortKey.toLowerCase()) {
            case "price" -> "price";
            case "stock" -> "qty";
            default -> "medicine.brandName";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(direction, sortField));
        var pageResult = repository.searchCatalog(trimmedQuery, trimmedCategory, pageRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("items", pageResult.getContent());
        response.put("page", pageResult.getNumber());
        response.put("size", pageResult.getSize());
        response.put("total", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());
        return response;
    }

    public List<String> getCatalogCategories() {
        return repository.findDistinctTypes().stream()
            .filter(value -> value != null && !value.trim().isEmpty())
            .collect(Collectors.toList());
    }
}
