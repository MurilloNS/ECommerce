package br.com.murillo.productsservice.products.controllers;

import br.com.murillo.productsservice.exceptions.ProductException;
import br.com.murillo.productsservice.products.dtos.ProductDTO;
import br.com.murillo.productsservice.products.enums.ProductErrors;
import br.com.murillo.productsservice.products.models.Product;
import br.com.murillo.productsservice.products.repositories.ProductRepository;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@XRayEnabled
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Logger LOG = LogManager.getLogger(ProductController.class);
    private final ProductRepository productRepository;

    @Autowired
    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        LOG.info("Get all products");

        List<ProductDTO> productsDTO = new ArrayList<>();

        productRepository.getAll().items().subscribe(product ->
                productsDTO.add(new ProductDTO(product))).join();

        return new ResponseEntity<>(productsDTO, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable String id) throws ProductException {
        Product product = productRepository.getById(id).join();

        if (product != null) {
            LOG.info("Get product by its id: {}", id);
            return new ResponseEntity<>(new ProductDTO(product), HttpStatus.OK);
        }
        else
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        Product productCreated = ProductDTO.toProduct(productDTO);
        productCreated.setId(UUID.randomUUID().toString());

        productRepository.create(productCreated).join();

        LOG.info("Product created - ID: {}", productCreated.getId());

        return new ResponseEntity<>(new ProductDTO(productCreated), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDTO> deleteProductById(@PathVariable String id) throws ProductException {
        Product productDeleted = productRepository.deleteById(id).join();

        if (productDeleted != null) {
            LOG.info("Product deleted - ID: {}", productDeleted.getId());
            return new ResponseEntity<>(new ProductDTO(productDeleted), HttpStatus.OK);
        }
        else
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable String id, @RequestBody ProductDTO productDTO) throws ProductException {
        try {
            Product productUpdated = productRepository.update(ProductDTO.toProduct(productDTO), id).join();

            LOG.info("Product updated - ID: {}", productUpdated.getId());

            return new ResponseEntity<>(new ProductDTO(productUpdated), HttpStatus.OK);
        } catch (CompletionException e) {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }
}