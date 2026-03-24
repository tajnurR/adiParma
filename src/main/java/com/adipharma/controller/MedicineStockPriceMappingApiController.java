package com.adipharma.controller;

import com.adipharma.service.MedicineStockPriceMappingService;
import com.adipharma.entity.AdiMedicineStockPriceMapping;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/")
public class MedicineStockPriceMappingApiController {

    private final MedicineStockPriceMappingService service;

    public MedicineStockPriceMappingApiController(MedicineStockPriceMappingService service) {
        this.service = service;
    }

    @GetMapping("medicine-stock-price-mappings")
    public List<AdiMedicineStockPriceMapping> getMedicineStockDetailsWithLimit(
        @RequestParam(name = "q", required = false, defaultValue = "") String query
    ) {
        return service.getMedicineStockDetailsWithLimit(query);
    }

    @GetMapping("products/catalog")
    public Map<String, Object> getProductCatalog(
        @RequestParam(name = "q", required = false, defaultValue = "") String query,
        @RequestParam(name = "category", required = false, defaultValue = "") String category,
        @RequestParam(name = "page", required = false, defaultValue = "0") int page,
        @RequestParam(name = "size", required = false, defaultValue = "20") int size,
        @RequestParam(name = "sort", required = false, defaultValue = "name") String sort,
        @RequestParam(name = "dir", required = false, defaultValue = "asc") String dir
    ) {
        return service.getCatalog(query, category, page, size, sort, dir);
    }

    @GetMapping("products/catalog/categories")
    public List<String> getProductCatalogCategories() {
        return service.getCatalogCategories();
    }

    @GetMapping("products/options/generics")
    public List<Map<String, Object>> getGenericOptions() {
        return service.getGenerics();
    }

    @GetMapping("products/options/manufacturers")
    public List<Map<String, Object>> getManufacturerOptions() {
        return service.getManufacturers();
    }

    @PostMapping("products")
    public ResponseEntity<?> createProduct(@RequestBody CreateProductRequest request) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "product details are required"));
        }
        try {
            Map<String, Object> response = service.createProduct(
                request.name,
                request.code,
                request.genericId,
                request.manufacturerId,
                request.category,
                request.description,
                request.sellingPrice,
                request.costPrice,
                request.qty,
                request.requiresRx,
                request.trackExpiry
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(service.getProductDetails(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("products/{id}")
    public ResponseEntity<?> updateProduct(
        @PathVariable("id") Long id,
        @RequestBody CreateProductRequest request
    ) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "product details are required"));
        }
        try {
            Map<String, Object> response = service.updateProduct(
                id,
                request.name,
                request.code,
                request.genericId,
                request.manufacturerId,
                request.category,
                request.description,
                request.sellingPrice,
                request.costPrice,
                request.qty,
                request.requiresRx,
                request.trackExpiry
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("products/{id}/pricing")
    public ResponseEntity<?> addPricing(
        @PathVariable("id") Long id,
        @RequestBody PricingRequest request
    ) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "pricing details are required"));
        }
        try {
            return ResponseEntity.ok(
                service.addPricing(id, request.sellingPrice, request.costPrice, request.qty)
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("stock-alerts")
    public Map<String, Object> getStockAlerts(
        @RequestParam(name = "q", required = false, defaultValue = "") String query,
        @RequestParam(name = "type", required = false, defaultValue = "all") String type,
        @RequestParam(name = "page", required = false, defaultValue = "0") int page,
        @RequestParam(name = "size", required = false, defaultValue = "15") int size,
        @RequestParam(name = "sort", required = false, defaultValue = "sku") String sort,
        @RequestParam(name = "dir", required = false, defaultValue = "asc") String dir
    ) {
        return service.getStockAlerts(query, type, page, size, sort, dir, 10, 30);
    }

    @GetMapping("stock-alerts/summary")
    public Map<String, Object> getStockAlertSummary() {
        return service.getStockAlertSummary(10, 30);
    }

    public static class CreateProductRequest {
        public String name;
        public String code;
        public Long genericId;
        public Long manufacturerId;
        public String category;
        public String description;
        public BigDecimal sellingPrice;
        public BigDecimal costPrice;
        public Integer qty;
        public Boolean requiresRx;
        public Boolean trackExpiry;
    }

    public static class PricingRequest {
        public BigDecimal sellingPrice;
        public BigDecimal costPrice;
        public Integer qty;
    }
}
