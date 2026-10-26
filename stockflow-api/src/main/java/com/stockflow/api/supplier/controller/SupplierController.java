package com.stockflow.api.supplier.controller;

import com.stockflow.api.supplier.dto.SupplierRequest;
import com.stockflow.api.supplier.dto.SupplierResponse;
import com.stockflow.api.supplier.service.SupplierService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierResponse> findAll() {
        return supplierService.findAll();
    }

    @GetMapping("/active")
    public List<SupplierResponse> findAllActive() {
        return supplierService.findAllActive();
    }

    @GetMapping("/{id}")
    public SupplierResponse findById(@PathVariable Long id) {
        return supplierService.findById(id);
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(
            @Valid @RequestBody SupplierRequest request) {
        SupplierResponse response = supplierService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        supplierService.deactivate(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable Long id) {
        supplierService.reactivate(id);

        return ResponseEntity.noContent().build();
    }

    
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> deletePermanently(@PathVariable Long id) {
        supplierService.deletePermanently(id);

        return ResponseEntity.noContent().build();
    }

}
