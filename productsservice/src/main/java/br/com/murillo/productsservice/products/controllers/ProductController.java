package br.com.murillo.productsservice.products.controllers;

import br.com.murillo.productsservice.events.dtos.EventType;
import br.com.murillo.productsservice.events.services.EventsPublisher;
import br.com.murillo.productsservice.products.exceptions.ProductException;
import br.com.murillo.productsservice.products.dtos.ProductDTO;
import br.com.murillo.productsservice.products.enums.ProductErrors;
import br.com.murillo.productsservice.products.models.Product;
import br.com.murillo.productsservice.products.repositories.ProductRepository;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@XRayEnabled
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Logger LOG = LogManager.getLogger(ProductController.class);
    private final ProductRepository productRepository;
    private EventsPublisher eventsPublisher;

    @Autowired
    public ProductController(ProductRepository productRepository, EventsPublisher eventsPublisher) {
        this.productRepository = productRepository;
        this.eventsPublisher = eventsPublisher;
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(required = false) String code) throws ProductException {
        if (code != null) {
            LOG.info("Get product by code: {}", code);

            Product productByCode = productRepository.getByCode(code).join();
            if (productByCode != null) {
                return new ResponseEntity<>(new ProductDTO(productByCode), HttpStatus.OK);
            } else {
                throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, null);
            }
        } else {
            LOG.info("Get all products");

            List<ProductDTO> productsDTO = new ArrayList<>();

            productRepository.getAll().items().subscribe(product ->
                    productsDTO.add(new ProductDTO(product))).join();

            return new ResponseEntity<>(productsDTO, HttpStatus.OK);
        }
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
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO)
            throws ProductException, JsonProcessingException, ExecutionException, InterruptedException {
        Product productCreated = ProductDTO.toProduct(productDTO);
        productCreated.setId(UUID.randomUUID().toString());

        CompletableFuture<Void> productCompletableFeature = productRepository.create(productCreated);

        CompletableFuture<PublishResponse> publishResponseCompletableFuture = eventsPublisher
                .sendProductEvent(productCreated, EventType.PRODUCT_CREATED, "hannah@email.com.br");

        CompletableFuture.allOf(productCompletableFeature, publishResponseCompletableFuture).join();
        PublishResponse publishResponse = publishResponseCompletableFuture.get();
        ThreadContext.put("messageId", publishResponse.messageId());

        LOG.info("Product created - ID: {}", productCreated.getId());

        return new ResponseEntity<>(new ProductDTO(productCreated), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDTO> deleteProductById(@PathVariable String id)
            throws ProductException, JsonProcessingException {
        Product productDeleted = productRepository.deleteById(id).join();

        if (productDeleted != null) {
            PublishResponse publishResponse = eventsPublisher
                    .sendProductEvent(productDeleted, EventType.PRODUCT_DELETED, "doralice@email.com.br")
                    .join();
            ThreadContext.put("messageId", publishResponse.messageId());

            LOG.info("Product deleted - ID: {}", productDeleted.getId());
            return new ResponseEntity<>(new ProductDTO(productDeleted), HttpStatus.OK);
        }
        else
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable String id, @RequestBody ProductDTO productDTO)
            throws ProductException, JsonProcessingException {
        try {
            Product productUpdated = productRepository.update(ProductDTO.toProduct(productDTO), id).join();

            PublishResponse publishResponse = eventsPublisher
                    .sendProductEvent(productUpdated, EventType.PRODUCT_UPDATED, "matilde@email.com.br")
                    .join();
            ThreadContext.put("messageId", publishResponse.messageId());

            LOG.info("Product updated - ID: {}", productUpdated.getId());

            return new ResponseEntity<>(new ProductDTO(productUpdated), HttpStatus.OK);
        } catch (CompletionException e) {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }
}