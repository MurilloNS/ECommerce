package br.com.murillo.productsservice.products.controllers;

import br.com.murillo.productsservice.products.dtos.ProductDTO;
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
    public ResponseEntity<?> getProductById(@PathVariable String id) {
        Product product = productRepository.getById(id).join();

        if (product != null)
            return new ResponseEntity<>(new ProductDTO(product), HttpStatus.OK);
        else
            return new ResponseEntity<>("Product not found!", HttpStatus.NO_CONTENT);
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
    public ResponseEntity<?> deleteProductById(@PathVariable String id) {
        Product productDeleted = productRepository.deleteById(id).join();

        if (productDeleted != null) {
            LOG.info("Product deleted - ID: {}", productDeleted.getId());
            return new ResponseEntity<>(new ProductDTO(productDeleted), HttpStatus.OK);
        }
        else
            return new ResponseEntity<>("Product not found!", HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody ProductDTO productDTO) {
        try {
            Product productUpdated = productRepository.update(ProductDTO.toProduct(productDTO), id).join();

            LOG.info("Product updated - ID: {}", productUpdated.getId());

            return new ResponseEntity<>(new ProductDTO(productUpdated), HttpStatus.OK);
        } catch (CompletionException e) {
            return new ResponseEntity<>("Product not found!", HttpStatus.NO_CONTENT);
        }
    }
}