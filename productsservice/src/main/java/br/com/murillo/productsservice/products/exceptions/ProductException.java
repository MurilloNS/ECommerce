package br.com.murillo.productsservice.products.exceptions;

import br.com.murillo.productsservice.products.enums.ProductErrors;
import jakarta.annotation.Nullable;

public class ProductException extends Exception {
    private final ProductErrors productErrors;

    @Nullable
    private final String productId;

    public ProductException(ProductErrors productErrors, @Nullable String productId) {
        this.productErrors = productErrors;
        this.productId = productId;
    }

    public ProductErrors getProductErrors() {
        return productErrors;
    }

    @Nullable
    public String getProductId() {
        return productId;
    }
}