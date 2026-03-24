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
        if (costPrice != null && costPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("cost price must be a positive number");
        }
        if (qty == null || qty < 0) {
            throw new IllegalArgumentException("stock quantity must be zero or greater");
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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
