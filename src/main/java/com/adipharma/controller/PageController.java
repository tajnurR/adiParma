package com.adipharma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({ "/pos", "/customers", "/products", "/stock", "/stock-alerts", "/reports", "/transactions", "/settings", "/profile" })
    public String pages() {
        return "home/index";
    }

    @GetMapping("/customers/{id}/sales")
    public String customerSales() {
        return "home/index";
    }
}
