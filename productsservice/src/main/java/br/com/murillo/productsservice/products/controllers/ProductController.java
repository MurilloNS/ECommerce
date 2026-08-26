package br.com.murillo.productsservice.products.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Logger LOG = LogManager.getLogger(ProductController.class);

    @GetMapping
    public String getAllProducts() {
        LOG.info("Get all products");
        return "All products";
    }
}