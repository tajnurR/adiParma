package com.adipharma.controller;

import com.adipharma.service.MedicineStockPriceMappingService;
import com.adipharma.entity.AdiMedicineStockPriceMapping;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medicine-stock-price-mappings")
public class MedicineStockPriceMappingApiController {

    private final MedicineStockPriceMappingService service;

    public MedicineStockPriceMappingApiController(MedicineStockPriceMappingService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdiMedicineStockPriceMapping> listTop50() {
        return service.listTop50();
    }
}
