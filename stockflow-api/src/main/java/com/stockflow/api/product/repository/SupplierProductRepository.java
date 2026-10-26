
package com.stockflow.api.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stockflow.api.product.SupplierProduct;

public interface SupplierProductRepository
        extends JpaRepository<SupplierProduct, Long> {

    List<SupplierProduct> findAllByProduct_Id(Long productId);

    List<SupplierProduct> findAllBySupplier_Id(Long supplierId);

    Optional<SupplierProduct> findByProduct_IdAndSupplier_Id(
            Long productId,
            Long supplierId);

    void deleteAllByProduct_Id(Long productId);

    void deleteByProduct_IdAndSupplier_Id(Long productId, Long supplierId);

}
