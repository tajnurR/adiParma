package com.adipharma.service;

import com.adipharma.entity.AdiMedicineDetails;
import com.adipharma.entity.AdiMedicineGeneric;
import com.adipharma.entity.AdiMedicineManufacturals;
import com.adipharma.entity.AdiMedicineStockPriceMapping;
import com.adipharma.repository.AdiMedicineDetailsRepository;
import com.adipharma.repository.AdiMedicineGenericRepository;
import com.adipharma.repository.AdiMedicineManufacturalsRepository;
import com.adipharma.repository.AdiMedicineStockPriceMappingRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class MedicineStockPriceMappingService {

    private static final int MAX_RESULTS = 50;
    private static final int MAX_CATALOG_PAGE_SIZE = 100;

    private final AdiMedicineStockPriceMappingRepository repository;
    private final AdiMedicineDetailsRepository detailsRepository;
    private final AdiMedicineGenericRepository genericRepository;
    private final AdiMedicineManufacturalsRepository manufacturerRepository;

    public MedicineStockPriceMappingService(
        AdiMedicineStockPriceMappingRepository repository,
        AdiMedicineDetailsRepository detailsRepository,
        AdiMedicineGenericRepository genericRepository,
        AdiMedicineManufacturalsRepository manufacturerRepository
    ) {
        this.repository = repository;
        this.detailsRepository = detailsRepository;
        this.genericRepository = genericRepository;
        this.manufacturerRepository = manufacturerRepository;
    }

    public List<AdiMedicineStockPriceMapping> getMedicineStockDetailsWithLimit(String query) {
        return getMedicineStockDetailsWithLimit(query, "all");
    }

    public List<AdiMedicineStockPriceMapping> getMedicineStockDetailsWithLimit(String query, String searchBy) {
        String trimmed = query == null ? "" : query.trim();
        String scope = normalizeSearchBy(searchBy);
        PageRequest pageRequest = PageRequest.of(0, MAX_RESULTS, Sort.by("id").descending());
        if (trimmed.isEmpty()) {
            return repository.findAllWithMedicine(pageRequest).getContent();
        }
        return repository.searchByTermAndScope(trimmed, scope, pageRequest).getContent();
    }

    private static String normalizeSearchBy(String searchBy) {
        if (searchBy == null) {
            return "all";
        }
        return switch (searchBy.trim().toLowerCase()) {
            case "medicine", "generic", "company", "code", "category" -> searchBy.trim().toLowerCase();
            default -> "all";
        };
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

    public List<Map<String, Object>> getGenerics() {
        return genericRepository.findAll(Sort.by("genericName").ascending())
            .stream()
            .map(generic -> Map.<String, Object>of(
                "id", generic.getId(),
                "name", generic.getGenericName()
            ))
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getManufacturers() {
        return manufacturerRepository.findAll(Sort.by("manufacturerName").ascending())
            .stream()
            .map(manufacturer -> Map.<String, Object>of(
                "id", manufacturer.getId(),
                "name", manufacturer.getManufacturerName()
            ))
            .collect(Collectors.toList());
    }

    public Map<String, Object> createProduct(
        String name,
        String code,
        Long genericId,
        Long manufacturerId,
        String category,
        String description,
        BigDecimal sellingPrice,
        BigDecimal costPrice,
        Integer qty,
        java.time.LocalDate expireDate,
        Boolean requiresRx,
        Boolean trackExpiry
    ) {
        if (isBlank(name) || isBlank(code) || genericId == null || manufacturerId == null || isBlank(category)) {
            throw new IllegalArgumentException("name, code, generic, manufacturer, and category are required");
        }
        if (!code.trim().matches("^M\\d{6}$")) {
            throw new IllegalArgumentException("code must follow format M######");
        }
        if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("selling price must be a positive number");
        }
        if (costPrice == null) {
            throw new IllegalArgumentException("cost price is required");
        }
        if (costPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("cost price must be a positive number");
        }
        if (qty == null || qty < 0) {
            throw new IllegalArgumentException("stock quantity must be zero or greater");
        }
        if (expireDate == null) {
            throw new IllegalArgumentException("expire date is required");
        }
        if (detailsRepository.existsByBrandCode(code.trim())) {
            throw new IllegalArgumentException("product code already exists");
        }

        AdiMedicineGeneric generic = genericRepository.findById(genericId)
            .orElseThrow(() -> new IllegalArgumentException("generic not found"));
        AdiMedicineManufacturals manufacturer = manufacturerRepository.findById(manufacturerId)
            .orElseThrow(() -> new IllegalArgumentException("manufacturer not found"));

        AdiMedicineDetails details = AdiMedicineDetails.builder()
            .brandName(name.trim())
            .brandCode(code.trim())
            .type(category.trim())
            .description(isBlank(description) ? null : description.trim())
            .generic(generic)
            .manufacturer(manufacturer)
            .requiresRx(requiresRx != null && requiresRx)
            .trackExpiry(trackExpiry != null && trackExpiry)
            .build();

        AdiMedicineDetails savedDetails = detailsRepository.save(details);

        AdiMedicineStockPriceMapping mapping = AdiMedicineStockPriceMapping.builder()
            .medicine(savedDetails)
            .price(sellingPrice)
            .costPrice(costPrice)
            .qty(qty)
            .expireDate(expireDate)
            .addedBy("admin")
            .build();

        AdiMedicineStockPriceMapping savedMapping = repository.save(mapping);

        Map<String, Object> response = new HashMap<>();
        response.put("id", savedMapping.getId());
        response.put("medicineId", savedDetails.getId());
        response.put("name", savedDetails.getBrandName());
        response.put("code", savedDetails.getBrandCode());
        return response;
    }

    public Map<String, Object> getProductDetails(Long mappingId) {
        AdiMedicineStockPriceMapping mapping = repository.findByIdWithMedicine(mappingId)
            .orElseThrow(() -> new IllegalArgumentException("product not found"));
        AdiMedicineDetails details = mapping.getMedicine();
        AdiMedicineGeneric generic = details != null ? details.getGeneric() : null;
        AdiMedicineManufacturals manufacturer = details != null ? details.getManufacturer() : null;

        Map<String, Object> response = new HashMap<>();
        response.put("id", mapping.getId());
        response.put("name", details != null ? details.getBrandName() : null);
        response.put("code", details != null ? details.getBrandCode() : null);
        response.put("category", details != null ? details.getType() : null);
        response.put("description", details != null ? details.getDescription() : null);
        response.put("genericId", generic != null ? generic.getId() : null);
        response.put("manufacturerId", manufacturer != null ? manufacturer.getId() : null);
        response.put("sellingPrice", mapping.getPrice());
        response.put("costPrice", mapping.getCostPrice());
        response.put("qty", mapping.getQty());
        response.put("expireDate", mapping.getExpireDate());
        response.put("requiresRx", details != null ? details.getRequiresRx() : null);
        response.put("trackExpiry", details != null ? details.getTrackExpiry() : null);
        return response;
    }

    public Map<String, Object> updateProduct(
        Long mappingId,
        String name,
        String code,
        Long genericId,
        Long manufacturerId,
        String category,
        String description,
        BigDecimal sellingPrice,
        BigDecimal costPrice,
        Integer qty,
        Boolean requiresRx,
        Boolean trackExpiry
    ) {
        if (mappingId == null) {
            throw new IllegalArgumentException("product id is required");
        }
        if (isBlank(name) || isBlank(code) || genericId == null || manufacturerId == null || isBlank(category)) {
            throw new IllegalArgumentException("name, code, generic, manufacturer, and category are required");
        }
        if (!code.trim().matches("^M\\d{6}$")) {
            throw new IllegalArgumentException("code must follow format M######");
        }

        AdiMedicineStockPriceMapping mapping = repository.findByIdWithMedicine(mappingId)
            .orElseThrow(() -> new IllegalArgumentException("product not found"));
        AdiMedicineDetails details = mapping.getMedicine();
        if (details == null) {
            throw new IllegalArgumentException("product details not found");
        }
        if (detailsRepository.existsByBrandCodeAndIdNot(code.trim(), details.getId())) {
            throw new IllegalArgumentException("product code already exists");
        }

        AdiMedicineGeneric generic = genericRepository.findById(genericId)
            .orElseThrow(() -> new IllegalArgumentException("generic not found"));
        AdiMedicineManufacturals manufacturer = manufacturerRepository.findById(manufacturerId)
            .orElseThrow(() -> new IllegalArgumentException("manufacturer not found"));

        details.setBrandName(name.trim());
        details.setBrandCode(code.trim());
        details.setType(category.trim());
        details.setDescription(isBlank(description) ? null : description.trim());
        details.setGeneric(generic);
        details.setManufacturer(manufacturer);
        details.setRequiresRx(requiresRx != null && requiresRx);
        details.setTrackExpiry(trackExpiry != null && trackExpiry);

        detailsRepository.save(details);
        AdiMedicineStockPriceMapping savedMapping = repository.save(mapping);

        Map<String, Object> response = new HashMap<>();
        response.put("id", savedMapping.getId());
        response.put("medicineId", details.getId());
        response.put("name", details.getBrandName());
        response.put("code", details.getBrandCode());
        return response;
    }

    public Map<String, Object> addPricing(
        Long medicineId,
        BigDecimal sellingPrice,
        BigDecimal costPrice,
        Integer qty,
        java.time.LocalDate expireDate
    ) {
        if (medicineId == null) {
            throw new IllegalArgumentException("medicine id is required");
        }
        if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("selling price must be a positive number");
        }
        if (costPrice == null || costPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("cost price must be a positive number");
        }
        if (qty == null || qty < 0) {
            throw new IllegalArgumentException("stock quantity must be zero or greater");
        }
        if (expireDate == null) {
            throw new IllegalArgumentException("expire date is required");
        }

        AdiMedicineDetails details = detailsRepository.findById(medicineId)
            .orElseThrow(() -> new IllegalArgumentException("medicine not found"));

        AdiMedicineStockPriceMapping mapping = repository
            .findByMedicineAndPrices(medicineId, sellingPrice, costPrice, expireDate)
            .orElseGet(() -> AdiMedicineStockPriceMapping.builder()
                .medicine(details)
                .price(sellingPrice)
                .costPrice(costPrice)
                .expireDate(expireDate)
                .addedBy("admin")
                .build()
            );

        int currentQty = mapping.getQty() == null ? 0 : mapping.getQty();
        mapping.setQty(currentQty + qty);
        mapping.setExpireDate(expireDate);
        AdiMedicineStockPriceMapping saved = repository.save(mapping);

        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("medicineId", details.getId());
        response.put("qty", saved.getQty());
        response.put("price", saved.getPrice());
        response.put("costPrice", saved.getCostPrice());
        return response;
    }

    public Map<String, Object> getStockAlerts(
        String query,
        String type,
        int page,
        int size,
        String sortKey,
        String sortDir,
        int lowStockLimit,
        int expiringDays
    ) {
        String trimmedQuery = query == null ? "" : query.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_CATALOG_PAGE_SIZE));

        PageRequest pageRequest = PageRequest.of(0, 5000, Sort.by("id").descending());
        var pageResult = repository.searchAlerts(trimmedQuery, pageRequest);

        java.time.LocalDate thresholdDate = java.time.LocalDate.now().plusDays(expiringDays);

        List<Map<String, Object>> filtered = pageResult.getContent().stream()
            .map(mapping -> {
                AdiMedicineDetails details = mapping.getMedicine();
                Map<String, Object> row = new HashMap<>();
                row.put("id", mapping.getId());
                row.put("sku", details != null ? details.getBrandCode() : null);
                row.put("name", details != null ? details.getBrandName() : null);
                row.put("qty", mapping.getQty());
                row.put("expireDate", mapping.getExpireDate());
                String status = buildAlertStatus(mapping, lowStockLimit, thresholdDate);
                row.put("status", status);
                return row;
            })
            .filter(row -> filterByType(row, type))
            .collect(Collectors.toList());

        String sortField = switch (sortKey == null ? "" : sortKey.toLowerCase()) {
            case "quantity" -> "qty";
            case "expiry" -> "expireDate";
            default -> "sku";
        };
        boolean desc = "desc".equalsIgnoreCase(sortDir);
        filtered.sort((a, b) -> {
            Object av = a.get(sortField);
            Object bv = b.get(sortField);
            int cmp;
            if (av == null && bv == null) {
                cmp = 0;
            } else if (av == null) {
                cmp = 1;
            } else if (bv == null) {
                cmp = -1;
            } else if (av instanceof Comparable && bv instanceof Comparable) {
                cmp = ((Comparable) av).compareTo(bv);
            } else {
                cmp = String.valueOf(av).compareTo(String.valueOf(bv));
            }
            return desc ? -cmp : cmp;
        });

        int total = filtered.size();
        int from = Math.min(safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<Map<String, Object>> items = filtered.subList(from, to);

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", safePage);
        response.put("size", safeSize);
        response.put("total", total);
        response.put("totalPages", (int) Math.ceil(total / (double) safeSize));
        return response;
    }

    public Map<String, Object> getStockAlertSummary(int lowStockLimit, int expiringDays) {
        java.time.LocalDate thresholdDate = java.time.LocalDate.now().plusDays(expiringDays);
        long lowStock = repository.countLowStock(lowStockLimit);
        long expiring = repository.countExpiringSoon(thresholdDate);
        long outOfStock = repository.countOutOfStock();
        Map<String, Object> response = new HashMap<>();
        response.put("lowStock", lowStock);
        response.put("expiringSoon", expiring);
        response.put("outOfStock", outOfStock);
        response.put("expiringDays", expiringDays);
        return response;
    }

    public Map<String, Object> getStockList(
        String query,
        String status,
        int page,
        int size,
        String sortKey,
        String sortDir,
        int lowStockLimit,
        int expiringDays
    ) {
        String trimmedQuery = query == null ? "" : query.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_CATALOG_PAGE_SIZE));

        PageRequest pageRequest = PageRequest.of(0, 5000, Sort.by("id").descending());
        var pageResult = repository.searchAlerts(trimmedQuery, pageRequest);

        java.time.LocalDate thresholdDate = java.time.LocalDate.now().plusDays(expiringDays);

        List<Map<String, Object>> filtered = pageResult.getContent().stream()
            .map(mapping -> {
                AdiMedicineDetails details = mapping.getMedicine();
                Map<String, Object> row = new HashMap<>();
                row.put("id", mapping.getId());
                row.put("code", details != null ? details.getBrandCode() : null);
                row.put("name", details != null ? details.getBrandName() : null);
                row.put("category", details != null ? details.getType() : null);
                row.put("qty", mapping.getQty());
                row.put("price", mapping.getPrice());
                row.put("costPrice", mapping.getCostPrice());
                row.put("expireDate", mapping.getExpireDate());
                String statusLabel = buildAlertStatus(mapping, lowStockLimit, thresholdDate);
                row.put("status", statusLabel);
                return row;
            })
            .filter(row -> filterByStockStatus(row, status))
            .collect(Collectors.toList());

        String sortField = switch (sortKey == null ? "" : sortKey.toLowerCase()) {
            case "name" -> "name";
            case "qty" -> "qty";
            case "expiry" -> "expireDate";
            case "price" -> "price";
            default -> "code";
        };
        boolean desc = "desc".equalsIgnoreCase(sortDir);
        filtered.sort((a, b) -> {
            Object av = a.get(sortField);
            Object bv = b.get(sortField);
            int cmp;
            if (av == null && bv == null) {
                cmp = 0;
            } else if (av == null) {
                cmp = 1;
            } else if (bv == null) {
                cmp = -1;
            } else if (av instanceof Comparable && bv instanceof Comparable) {
                cmp = ((Comparable) av).compareTo(bv);
            } else {
                cmp = String.valueOf(av).compareTo(String.valueOf(bv));
            }
            return desc ? -cmp : cmp;
        });

        int total = filtered.size();
        int from = Math.min(safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<Map<String, Object>> items = filtered.subList(from, to);

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", safePage);
        response.put("size", safeSize);
        response.put("total", total);
        response.put("totalPages", (int) Math.ceil(total / (double) safeSize));
        return response;
    }

    public Map<String, Object> getStockSummary(int lowStockLimit, int expiringDays) {
        java.time.LocalDate thresholdDate = java.time.LocalDate.now().plusDays(expiringDays);
        long total = repository.count();
        long lowStock = repository.countLowStock(lowStockLimit);
        long expiring = repository.countExpiringSoon(thresholdDate);
        long outOfStock = repository.countOutOfStock();
        Map<String, Object> response = new HashMap<>();
        response.put("total", total);
        response.put("lowStock", lowStock);
        response.put("expiringSoon", expiring);
        response.put("outOfStock", outOfStock);
        response.put("expiringDays", expiringDays);
        return response;
    }

    private static String buildAlertStatus(
        AdiMedicineStockPriceMapping mapping,
        int lowStockLimit,
        java.time.LocalDate thresholdDate
    ) {
        Integer qty = mapping.getQty();
        if (qty != null && qty == 0) {
            return "out_of_stock";
        }
        if (mapping.getExpireDate() != null && !mapping.getExpireDate().isAfter(thresholdDate)) {
            return "expiring_soon";
        }
        if (qty != null && qty <= lowStockLimit) {
            return "low_stock";
        }
        return "normal";
    }

    private static boolean filterByType(Map<String, Object> row, String type) {
        if (type == null || type.isBlank() || "all".equalsIgnoreCase(type)) {
            return !"normal".equals(row.get("status"));
        }
        String status = String.valueOf(row.get("status"));
        return switch (type.toLowerCase()) {
            case "low" -> "low_stock".equals(status);
            case "expiring" -> "expiring_soon".equals(status);
            case "out" -> "out_of_stock".equals(status);
            default -> !"normal".equals(status);
        };
    }

    private static boolean filterByStockStatus(Map<String, Object> row, String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return true;
        }
        String value = String.valueOf(row.get("status"));
        return switch (status.toLowerCase()) {
            case "low" -> "low_stock".equals(value);
            case "expiring" -> "expiring_soon".equals(value);
            case "out" -> "out_of_stock".equals(value);
            case "normal" -> "normal".equals(value);
            default -> true;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
