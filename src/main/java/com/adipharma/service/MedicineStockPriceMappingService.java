package com.adipharma.service;

import com.adipharma.entity.AdiMedicineStockPriceMapping;
import com.adipharma.repository.AdiMedicineStockPriceMappingRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class MedicineStockPriceMappingService {

    private static final int MAX_RESULTS = 50;

    private final AdiMedicineStockPriceMappingRepository repository;

    public MedicineStockPriceMappingService(AdiMedicineStockPriceMappingRepository repository) {
        this.repository = repository;
    }

    public List<AdiMedicineStockPriceMapping> listTop50() {
        return repository.findAllWithMedicine(PageRequest.of(0, MAX_RESULTS, Sort.by("id").descending()))
            .getContent();
    }
}
