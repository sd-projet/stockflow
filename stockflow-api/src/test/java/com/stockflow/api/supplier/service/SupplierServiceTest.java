
package com.stockflow.api.supplier.service;

import com.stockflow.api.common.exception.DuplicateResourceException;
import com.stockflow.api.common.exception.ResourceNotFoundException;
import com.stockflow.api.supplier.Supplier;
import com.stockflow.api.supplier.dto.SupplierRequest;
import com.stockflow.api.supplier.dto.SupplierResponse;
import com.stockflow.api.supplier.repository.SupplierRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    @Test
    void createShouldReturnSupplierWhenNameIsAvailable() {
        SupplierRequest request = new SupplierRequest(
                "Fournisseur A",
                "Jean Dupont",
                "contact@example.com",
                "0600000000",
                "10 rue de Paris",
                "Paris",
                "75001",
                "France");

        when(supplierRepository.existsByNameIgnoreCase("Fournisseur A"))
                .thenReturn(false);

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponse response = supplierService.create(request);

        assertEquals("Fournisseur A", response.name());
        assertEquals("contact@example.com", response.email());

        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createShouldRejectDuplicateName() {
        SupplierRequest request = new SupplierRequest(
                "Fournisseur A", null, null, null,
                null, null, null, null);

        when(supplierRepository.existsByNameIgnoreCase("Fournisseur A"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.create(request));

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void findByIdShouldRejectUnknownSupplier() {
        when(supplierRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.findById(999L));
    }

    @Test
    void updateShouldModifyExistingSupplier() {
        Supplier supplier = new Supplier();
        supplier.setName("Ancien nom");
        supplier.setActive(true);

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.existsByNameIgnoreCase("Nouveau nom"))
                .thenReturn(false);

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupplierRequest request = new SupplierRequest(
                "Nouveau nom",
                "Jean Dupont",
                "contact@example.com",
                "0600000000",
                "10 rue de Paris",
                "Paris",
                "75001",
                "France");

        SupplierResponse response = supplierService.update(1L, request);

        assertEquals("Nouveau nom", response.name());
        assertEquals("contact@example.com", response.email());

        verify(supplierRepository).save(supplier);
    }

    @Test
    void deactivateShouldSetSupplierAsInactive() {
        Supplier supplier = new Supplier();
        supplier.setName("Fournisseur A");
        supplier.setActive(true);

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        supplierService.deactivate(1L);

        assertFalse(supplier.getActive());
        verify(supplierRepository).save(supplier);
    }
}
