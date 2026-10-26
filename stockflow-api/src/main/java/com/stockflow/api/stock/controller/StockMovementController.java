
package com.stockflow.api.stock.controller;

import com.stockflow.api.stock.dto.StockMovementRequest;
import com.stockflow.api.stock.dto.StockMovementResponse;
import com.stockflow.api.stock.service.StockMovementService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.stockflow.api.user.AppUser;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(
            StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @GetMapping
    public List<StockMovementResponse> findAll() {
        return stockMovementService.findAll();
    }

    @GetMapping("/product/{productId}")
    public List<StockMovementResponse> findByProductId(
            @PathVariable Long productId) {
        return stockMovementService.findByProductId(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse create(
            @Valid @RequestBody StockMovementRequest request,
            Authentication authentication) {

        AppUser user = (AppUser) authentication.getPrincipal();

        return stockMovementService.createMovement(
                request.productId(),
                request.type(),
                request.quantity(),
                request.reason(),
                user.getId(),
                request.purchaseOrderLineId());
    }
}
