
package com.stockflow.api.purchaseorder.controller;

import com.stockflow.api.purchaseorder.dto.PurchaseOrderCreateRequest;
import com.stockflow.api.purchaseorder.dto.PurchaseOrderResponse;
import com.stockflow.api.purchaseorder.dto.PurchaseOrderUpdateRequest;
import com.stockflow.api.purchaseorder.service.PurchaseOrderService;
import com.stockflow.api.purchaseorder.dto.PurchaseOrderReceiveRequest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(
            PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderCreateRequest request) {
        return purchaseOrderService.createPurchaseOrder(request);
    }

    @GetMapping
    public List<PurchaseOrderResponse> getAllPurchaseOrders() {
        return purchaseOrderService.getAllPurchaseOrders();
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse getPurchaseOrderById(
            @PathVariable Long id) {
        return purchaseOrderService.getPurchaseOrderById(id);
    }

    @PutMapping("/{id}")
    public PurchaseOrderResponse updatePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderUpdateRequest request) {
        return purchaseOrderService.updatePurchaseOrder(id, request);
    }

    @PatchMapping("/{id}/order")
    public PurchaseOrderResponse orderPurchaseOrder(
            @PathVariable Long id) {
        return purchaseOrderService.orderPurchaseOrder(id);
    }

    @PatchMapping("/{id}/receive")
    public PurchaseOrderResponse receivePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderReceiveRequest request) {
        return purchaseOrderService.receivePurchaseOrder(id, request);
    }

    @PatchMapping("/{id}/archive")
    public PurchaseOrderResponse archivePurchaseOrder(@PathVariable Long id) {
        return purchaseOrderService.archivePurchaseOrder(id);
    }

    @PatchMapping("/{id}/cancel")
    public PurchaseOrderResponse cancelPurchaseOrder(@PathVariable Long id) {
        return purchaseOrderService.cancelPurchaseOrder(id);
    }

}
