package com.adipharma.controller;

import com.adipharma.service.MedicineStockPriceMappingService;
import com.adipharma.entity.AdiMedicineStockPriceMapping;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
