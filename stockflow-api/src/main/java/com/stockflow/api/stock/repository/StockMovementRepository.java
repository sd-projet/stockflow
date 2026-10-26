
package com.stockflow.api.stock.repository;

import com.stockflow.api.stock.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<StockMovement> findAllByOrderByCreatedAtDesc();

    List<StockMovement> findTop8ByOrderByCreatedAtDesc();
}
