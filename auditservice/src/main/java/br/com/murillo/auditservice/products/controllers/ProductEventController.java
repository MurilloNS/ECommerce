package br.com.murillo.auditservice.products.controllers;

import br.com.murillo.auditservice.products.dtos.ProductEventApiDTO;
import br.com.murillo.auditservice.products.dtos.ProductEventApiPageDTO;
import br.com.murillo.auditservice.products.models.ProductEvent;
import br.com.murillo.auditservice.products.repositories.ProductEventRepository;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@XRayEnabled
@RestController
@RequestMapping("/api/products/events")
public class ProductEventController {
    private final ProductEventRepository productEventRepository;

    @Autowired
    public ProductEventController(ProductEventRepository productEventRepository) {
        this.productEventRepository = productEventRepository;
    }

    @GetMapping
    public ResponseEntity<ProductEventApiPageDTO> getAll(@RequestParam String eventType,
                                                         @RequestParam(defaultValue = "5") int limit,
                                                         @RequestParam(required = false) String from,
                                                         @RequestParam(required = false) String to,
                                                         @RequestParam(required = false) String exclusiveStartTimestamp) {
        List<ProductEventApiDTO> productEventApiDTOList = new ArrayList<>();

        SdkPublisher<Page<ProductEvent>> productEventsPublisher = (from != null && to != null)
                ? productEventRepository.findByTypeAndRange(eventType, exclusiveStartTimestamp, from, to, limit)
                : productEventRepository.findByType(eventType, exclusiveStartTimestamp, limit);

        AtomicReference<String> lastEvaluatedTimestamp = new AtomicReference<>();

        productEventsPublisher.subscribe(productEventPage -> {
              productEventApiDTOList.addAll(productEventPage.items().stream().map(ProductEventApiDTO::new).toList());

              if (productEventPage.lastEvaluatedKey() != null) {
                  lastEvaluatedTimestamp.set(productEventPage.lastEvaluatedKey().get("sk").s());
              }
        }).join();

        return new ResponseEntity<>(new ProductEventApiPageDTO(
                productEventApiDTOList, lastEvaluatedTimestamp.get(), productEventApiDTOList.size()), HttpStatus.OK);
    }
}