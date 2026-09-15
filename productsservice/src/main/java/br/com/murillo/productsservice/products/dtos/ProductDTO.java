package br.com.murillo.productsservice.products.dtos;

import br.com.murillo.productsservice.products.models.Product;
import com.fasterxml.jackson.annotation.JsonInclude;

public record ProductDTO(String id, String name, String code, float price, String model,
                         @JsonInclude(JsonInclude.Include.NON_NULL) String url) {
    public ProductDTO(Product product) {
        this(product.getId(), product.getProductName(), product.getCode(), product.getPrice(), product.getModel(),
                product.getProductUrl());
    }

    static public Product toProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setId(product.getId());
        product.setProductName(productDTO.name);
        product.setCode(productDTO.code);
        product.setPrice(productDTO.price);
        product.setModel(productDTO.model);
        product.setProductUrl(productDTO.url());

        return product;
    }
}