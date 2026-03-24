package com.adipharma.service;

import com.adipharma.repository.AdiMedicineDetailsRepository;
import com.adipharma.repository.AdiMedicineStockPriceMappingRepository;
import com.adipharma.repository.AdiPointSalesDetailsRepository;
import com.adipharma.repository.AdiPointSalesMasterRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ReportsService {

    private static final int TOP_PRODUCTS_LIMIT = 5;

    private final AdiPointSalesMasterRepository salesMasterRepository;
    private final AdiPointSalesDetailsRepository salesDetailsRepository;
    private final AdiMedicineDetailsRepository medicineDetailsRepository;
    private final AdiMedicineStockPriceMappingRepository stockRepository;

    public ReportsService(
        AdiPointSalesMasterRepository salesMasterRepository,
        AdiPointSalesDetailsRepository salesDetailsRepository,
        AdiMedicineDetailsRepository medicineDetailsRepository,
        AdiMedicineStockPriceMappingRepository stockRepository
    ) {
        this.salesMasterRepository = salesMasterRepository;
        this.salesDetailsRepository = salesDetailsRepository;
        this.medicineDetailsRepository = medicineDetailsRepository;
        this.stockRepository = stockRepository;
    }

    public Map<String, Object> getReport(LocalDate startDate, LocalDate endDate, int lowStockLimit, int expiringDays) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        long totalSales = salesMasterRepository.countSalesBetween(start, end);
        BigDecimal totalRevenue = salesMasterRepository.sumRevenueBetween(start, end);
        BigDecimal avgOrderValue = totalSales == 0
            ? BigDecimal.ZERO
            : totalRevenue.divide(BigDecimal.valueOf(totalSales), 2, java.math.RoundingMode.HALF_UP);

        List<Object[]> topRows = salesDetailsRepository.findTopSellingProducts(
            start,
            end,
            PageRequest.of(0, TOP_PRODUCTS_LIMIT)
        );
        List<Map<String, Object>> topProducts = new ArrayList<>();
        for (Object[] row : topRows) {
            Map<String, Object> item = new HashMap<>();
            item.put("product", row[0]);
            item.put("qty", row[1]);
            item.put("revenue", row[2]);
            topProducts.add(item);
        }

        long totalProducts = medicineDetailsRepository.count();
        BigDecimal stockValue = stockRepository.sumStockValue();
        long lowStock = stockRepository.countLowStock(lowStockLimit);
        long expiringSoon = stockRepository.countExpiringSoon(LocalDate.now().plusDays(expiringDays));

        Map<String, Object> response = new HashMap<>();
        response.put("totalSales", totalSales);
        response.put("totalRevenue", totalRevenue);
        response.put("avgOrderValue", avgOrderValue);
        response.put("topProducts", topProducts);
        response.put("totalProducts", totalProducts);
        response.put("stockValue", stockValue);
        response.put("lowStock", lowStock);
        response.put("expiringSoon", expiringSoon);
        return response;
    }
}
