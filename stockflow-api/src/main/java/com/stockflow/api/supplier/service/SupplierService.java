package com.stockflow.api.supplier.service;

import com.stockflow.api.common.exception.DuplicateResourceException;
import com.stockflow.api.common.exception.ResourceNotFoundException;
import com.stockflow.api.supplier.Supplier;
import com.stockflow.api.supplier.dto.SupplierRequest;
import com.stockflow.api.supplier.dto.SupplierResponse;
import com.stockflow.api.supplier.repository.SupplierRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<SupplierResponse> findAll() {
        return supplierRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<SupplierResponse> findAllActive() {
        return supplierRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SupplierResponse findById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fournisseur introuvable avec l'identifiant : " + id));

        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        String name = request.name().trim();

        if (supplierRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(
                    "Un fournisseur avec ce nom existe déjà.");
        }

        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setContactName(normalize(request.contactName()));
        supplier.setEmail(normalize(request.email()));
        supplier.setPhone(normalize(request.phone()));
        supplier.setAddress(normalize(request.address()));
        supplier.setCity(normalize(request.city()));
        supplier.setPostalCode(normalize(request.postalCode()));
        supplier.setCountry(normalize(request.country()));

        Supplier savedSupplier = supplierRepository.save(supplier);

        return toResponse(savedSupplier);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fournisseur introuvable avec l'identifiant : " + id));

        String name = request.name().trim();

        if (!supplier.getName().equalsIgnoreCase(name)
                && supplierRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(
                    "Un fournisseur avec ce nom existe déjà.");
        }

        supplier.setName(name);
        supplier.setContactName(normalize(request.contactName()));
        supplier.setEmail(normalize(request.email()));
        supplier.setPhone(normalize(request.phone()));
        supplier.setAddress(normalize(request.address()));
        supplier.setCity(normalize(request.city()));
        supplier.setPostalCode(normalize(request.postalCode()));
        supplier.setCountry(normalize(request.country()));

        Supplier updatedSupplier = supplierRepository.save(supplier);

        return toResponse(updatedSupplier);
    }

    @Transactional
    public void deactivate(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fournisseur introuvable avec l'identifiant : " + id));

        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    @Transactional
    public void reactivate(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fournisseur introuvable avec l'identifiant : " + id));

        supplier.setActive(true);
        supplierRepository.save(supplier);
    }

    @Transactional
    public void deletePermanently(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fournisseur introuvable avec l'identifiant : " + id));

        supplierRepository.delete(supplier);
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getCity(),
                supplier.getPostalCode(),
                supplier.getCountry(),
                supplier.getActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
