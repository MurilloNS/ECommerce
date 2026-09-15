package br.com.murillo.productsservice.products.dtos;

import br.com.murillo.productsservice.products.models.Product;

public record ProductDTO(String id, String name, String code, float price, String model) {
    public ProductDTO(Product product) {
        this(product.getId(), product.getProductName(), product.getCode(), product.getPrice(), product.getModel());
    }

    static public Product toProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setId(product.getId());
        product.setProductName(productDTO.name);
        product.setCode(productDTO.code);
        product.setPrice(productDTO.price);
        product.setModel(productDTO.model);

        return product;
    }
}