package com.adipharma.controller;

import com.adipharma.service.CustomerSalesService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerSalesApiController {

    private final CustomerSalesService service;

    public CustomerSalesApiController(CustomerSalesService service) {
        this.service = service;
    }

    @GetMapping("/{id}/sales")
    public Map<String, Object> getSales(@PathVariable("id") Integer customerId) {
        return service.getSalesForCustomer(customerId);
    }

    @GetMapping("/{id}/sales/{saleId}/details")
    public Map<String, Object> getSaleDetails(
        @PathVariable("id") Integer customerId,
        @PathVariable("saleId") Long saleId
    ) {
        return service.getSaleDetails(customerId, saleId);
    }
}
