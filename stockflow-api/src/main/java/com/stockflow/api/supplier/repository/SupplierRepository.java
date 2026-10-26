package com.stockflow.api.supplier.repository;

import com.stockflow.api.supplier.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Supplier> findByActiveTrue();
}
