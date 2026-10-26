
package com.stockflow.api.product.repository;

import com.stockflow.api.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByReferenceIgnoreCase(String reference);

    boolean existsByCategoryId(Long categoryId);

    List<Product> findByActiveTrue();

    long countByActiveTrue();

    @Query("""
            SELECT SUM(p.quantityInStock * p.unitPrice)
            FROM Product p
            WHERE p.active = true
            """)
    java.math.BigDecimal calculateStockValue();

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND p.quantityInStock <= p.minimumStock
            """)
    List<Product> findLowStockProducts();
}
