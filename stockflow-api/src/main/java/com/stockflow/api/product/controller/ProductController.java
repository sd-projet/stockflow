
package com.stockflow.api.product.controller;

import com.stockflow.api.product.dto.ProductRequest;
import com.stockflow.api.product.dto.ProductResponse;
import com.stockflow.api.product.service.ProductService;
import com.stockflow.api.product.dto.SupplierProductRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> findAll() {
        return productService.findAll();
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProductResponse>> findAllActive() {
        return ResponseEntity.ok(productService.findAllActive());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponse>> findLowStockProducts() {
        return ResponseEntity.ok(productService.findLowStockProducts());
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @PostMapping("/{id}/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse addSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierProductRequest request) {
        return productService.addSupplier(id, request);
    }

    @PutMapping("/{id}/suppliers/{supplierId}")
    public ProductResponse updateSupplier(
            @PathVariable Long id,
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierProductRequest request) {
        return productService.updateSupplier(id, supplierId, request);
    }

    @DeleteMapping("/{id}/suppliers/{supplierId}")
    public ResponseEntity<ProductResponse> removeSupplier(
            @PathVariable Long id,
            @PathVariable Long supplierId) {
        return ResponseEntity.ok(
                productService.removeSupplier(id, supplierId));
    }

    @PatchMapping("/{id}/reactivate")
    public ProductResponse reactivate(@PathVariable Long id) {
        return productService.reactivate(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        productService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
