package br.com.murillo.productsservice.exceptions;

import br.com.murillo.productsservice.products.dtos.ProductErrorResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ProductExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LogManager.getLogger(ProductExceptionHandler.class);

    @ExceptionHandler(value = {ProductException.class})
    protected ResponseEntity<Object> handleProductError(ProductException productException, WebRequest webRequest) {
        ProductErrorResponse productErrorResponse = new ProductErrorResponse(
                productException.getProductErrors().getMessage(),
                productException.getProductErrors().getHttpStatus().value(),
                ThreadContext.get("requestId"),
                productException.getProductId());

        LOG.error(productException.getProductErrors().getMessage());

        return handleExceptionInternal(productException, productErrorResponse, new HttpHeaders(),
                productException.getProductErrors().getHttpStatus(), webRequest);
    }
}