
package com.stockflow.api.stock.service;

import com.stockflow.api.common.exception.ResourceNotFoundException;
import com.stockflow.api.product.Product;
import com.stockflow.api.product.repository.ProductRepository;
import com.stockflow.api.stock.MovementType;
import com.stockflow.api.stock.StockMovement;
import com.stockflow.api.stock.dto.StockMovementResponse;
import com.stockflow.api.stock.repository.StockMovementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    public StockMovementService(
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public StockMovementResponse createMovement(
            Long productId,
            MovementType type,
            int quantity,
            String reason,
            Long performedBy,
            Long purchaseOrderLineId) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "La quantité doit être strictement positive.");
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Le type de mouvement est obligatoire.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Le motif du mouvement est obligatoire.");
        }

        if (performedBy == null) {
            throw new IllegalArgumentException(
                    "L'identifiant de l'utilisateur est obligatoire.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit introuvable avec l'identifiant : " + productId));

        if (!product.getActive()) {
            throw new IllegalStateException(
                    "Impossible de modifier le stock d'un produit désactivé.");
        }

        int quantityBefore = product.getQuantityInStock();
        int quantityAfter;

        switch (type) {
            case IN -> quantityAfter = Math.addExact(quantityBefore, quantity);
            case OUT -> {
                if (quantity > quantityBefore) {
                    throw new IllegalArgumentException(
                            "Stock insuffisant pour effectuer cette sortie.");
                }
                quantityAfter = quantityBefore - quantity;
            }
            case ADJUSTMENT -> quantityAfter = quantity;
            default -> throw new IllegalArgumentException(
                    "Type de mouvement invalide.");
        }

        product.setQuantityInStock(quantityAfter);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setType(type);
        movement.setQuantityBefore(quantityBefore);
        movement.setQuantityAfter(quantityAfter);
        movement.setReason(reason.trim());
        movement.setPerformedBy(performedBy);
        movement.setPurchaseOrderLineId(purchaseOrderLineId);

        StockMovement savedMovement = stockMovementRepository.save(movement);

        return toResponse(savedMovement);
    }

    public List<StockMovementResponse> findAll() {
        return stockMovementRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<StockMovementResponse> findByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException(
                    "Produit introuvable avec l'identifiant : " + productId);
        }

        return stockMovementRepository
                .findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private StockMovementResponse toResponse(StockMovement movement) {
        Product product = movement.getProduct();

        return new StockMovementResponse(
                movement.getId(),
                product.getId(),
                product.getReference(),
                product.getName(),
                movement.getType(),
                movement.getQuantityBefore(),
                movement.getQuantityAfter(),
                movement.getReason(),
                movement.getPurchaseOrderLineId(),
                movement.getPerformedBy(),
                movement.getCreatedAt());
    }
}
