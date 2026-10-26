
package com.stockflow.api.purchaseorder.repository;

import com.stockflow.api.purchaseorder.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long> {

    boolean existsByOrderNumberIgnoreCase(String orderNumber);

    long countByStatus(
            com.stockflow.api.purchaseorder.enums.PurchaseOrderStatus status);

    Optional<PurchaseOrder> findByOrderNumberIgnoreCase(String orderNumber);
}
