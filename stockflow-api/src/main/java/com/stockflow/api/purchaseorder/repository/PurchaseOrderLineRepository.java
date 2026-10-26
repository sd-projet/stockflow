
package com.stockflow.api.purchaseorder.repository;

import com.stockflow.api.purchaseorder.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderLineRepository
        extends JpaRepository<PurchaseOrderLine, Long> {

    List<PurchaseOrderLine> findByPurchaseOrderId(Long purchaseOrderId);

    boolean existsByPurchaseOrderIdAndProductId(
            Long purchaseOrderId,
            Long productId
    );
}
