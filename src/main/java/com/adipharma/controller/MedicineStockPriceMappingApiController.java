package com.adipharma.controller;

import com.adipharma.service.MedicineStockPriceMappingService;
import com.adipharma.entity.AdiMedicineStockPriceMapping;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/")
public class MedicineStockPriceMappingApiController {

    private final MedicineStockPriceMappingService service;

    public MedicineStockPriceMappingApiController(MedicineStockPriceMappingService service) {
        this.service = service;
    }

    @GetMapping("medicine-stock-price-mappings")
    public List<AdiMedicineStockPriceMapping> getMedicineStockDetailsWithLimit() {
        return service.getMedicineStockDetailsWithLimit();
    }
}
