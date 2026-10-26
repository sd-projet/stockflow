
package com.stockflow.api.purchaseorder.service;

import com.stockflow.api.purchaseorder.PurchaseOrder;
import com.stockflow.api.purchaseorder.enums.PurchaseOrderStatus;
import com.stockflow.api.purchaseorder.repository.PurchaseOrderLineRepository;
import com.stockflow.api.purchaseorder.repository.PurchaseOrderRepository;
import com.stockflow.api.supplier.repository.SupplierRepository;
import com.stockflow.api.user.repository.AppUserRepository;
import com.stockflow.api.product.repository.ProductRepository;
import com.stockflow.api.stock.service.StockMovementService;
import com.stockflow.api.supplier.Supplier;
import com.stockflow.api.user.AppUser;

import com.stockflow.api.purchaseorder.dto.PurchaseOrderUpdateRequest;

import com.stockflow.api.purchaseorder.PurchaseOrderLine;
import com.stockflow.api.purchaseorder.dto.PurchaseOrderReceiveRequest;
import com.stockflow.api.product.Product;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.stockflow.api.stock.MovementType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.stockflow.api.stock.dto.StockMovementResponse;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderLineRepository purchaseOrderLineRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StockMovementService stockMovementService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    @Test
    void orderPurchaseOrderShouldConfirmDraftOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0001");
        order.setStatus(PurchaseOrderStatus.DRAFT);

        try {
            Field idField = PurchaseOrder.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, 1L);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        Supplier supplier = new Supplier();
        supplier.setName("Fournisseur A");
        order.setSupplier(supplier);

        AppUser user = new AppUser();
        user.setFullName("Senebou Diarra");
        order.setCreatedBy(user);

        setId(supplier, 1L);
        setId(user, 1L);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(purchaseOrderLineRepository.findByPurchaseOrderId(1L))
                .thenReturn(java.util.List.of());

        var response = purchaseOrderService.orderPurchaseOrder(1L);

        assertEquals(PurchaseOrderStatus.ORDERED, response.status());
        verify(purchaseOrderRepository).save(order);
    }

    @Test
    void updatePurchaseOrderShouldRejectNonDraftOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setStatus(PurchaseOrderStatus.ORDERED);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        PurchaseOrderUpdateRequest request = mock(PurchaseOrderUpdateRequest.class);

        assertThrows(
                IllegalStateException.class,
                () -> purchaseOrderService.updatePurchaseOrder(1L, request));

        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void receivePurchaseOrderShouldRejectQuantityAboveRemaining() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0002");
        order.setStatus(PurchaseOrderStatus.ORDERED);

        setId(order, 1L);

        Supplier supplier = new Supplier();
        supplier.setName("Fournisseur A");
        setId(supplier, 1L);
        order.setSupplier(supplier);

        AppUser user = new AppUser();
        user.setFullName("Senebou Diarra");
        setId(user, 1L);
        order.setCreatedBy(user);

        Product product = new Product();
        setId(product, 1L);
        product.setName("Clavier");

        PurchaseOrderLine line = new PurchaseOrderLine();
        setId(line, 10L);
        line.setPurchaseOrder(order);
        line.setProduct(product);
        line.setQuantityOrdered(10);
        line.setQuantityReceived(7);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(purchaseOrderLineRepository.findByPurchaseOrderId(1L))
                .thenReturn(List.of(line));

        PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest(
                List.of(new PurchaseOrderReceiveRequest.ReceivedLine(10L, 4)));

        assertThrows(
                IllegalArgumentException.class,
                () -> purchaseOrderService.receivePurchaseOrder(1L, request));

        verifyNoInteractions(stockMovementService);
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void receivePurchaseOrderShouldSetPartiallyReceivedWhenOnlyPartOfOrderIsReceived() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0004");
        order.setStatus(PurchaseOrderStatus.ORDERED);
        setId(order, 1L);

        PurchaseOrderLine line = new PurchaseOrderLine();
        setId(line, 10L);
        line.setPurchaseOrder(order);
        line.setQuantityOrdered(10);
        line.setQuantityReceived(0);
        line.setUnitPrice(new BigDecimal("12.50"));

        Product product = new Product();
        setId(product, 1L);
        line.setProduct(product);

        Supplier supplier = new Supplier();
        setId(supplier, 1L);
        supplier.setName("Fournisseur test");
        order.setSupplier(supplier);

        AppUser user = new AppUser();
        setId(user, 1L);
        user.setFullName("Utilisateur test");
        order.setCreatedBy(user);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));
        when(purchaseOrderLineRepository.findByPurchaseOrderId(1L))
                .thenReturn(List.of(line));
        when(appUserRepository.findById(1L))
                .thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));

        try {
            PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest(
                    List.of(
                            new PurchaseOrderReceiveRequest.ReceivedLine(10L, 4)));

            when(stockMovementService.createMovement(
                    eq(1L),
                    eq(MovementType.IN),
                    eq(4),
                    any(String.class),
                    eq(1L),
                    eq(10L))).thenReturn(mock(StockMovementResponse.class));

            purchaseOrderService.receivePurchaseOrder(1L, request);

            assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, order.getStatus());
            assertEquals(4, line.getQuantityReceived());

            verify(purchaseOrderRepository).save(order);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void receivePurchaseOrderShouldSetReceivedWhenAllOrderIsReceived() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0005");
        order.setStatus(PurchaseOrderStatus.ORDERED);
        setId(order, 1L);

        PurchaseOrderLine line = new PurchaseOrderLine();
        setId(line, 10L);
        line.setPurchaseOrder(order);
        line.setQuantityOrdered(10);
        line.setQuantityReceived(0);
        line.setUnitPrice(new BigDecimal("12.50"));

        Product product = new Product();
        setId(product, 1L);
        line.setProduct(product);

        Supplier supplier = new Supplier();
        setId(supplier, 1L);
        supplier.setName("Fournisseur test");
        order.setSupplier(supplier);

        AppUser user = new AppUser();
        setId(user, 1L);
        user.setFullName("Utilisateur test");
        order.setCreatedBy(user);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));
        when(purchaseOrderLineRepository.findByPurchaseOrderId(1L))
                .thenReturn(List.of(line));
        when(appUserRepository.findById(1L))
                .thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));

        try {
            PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest(
                    List.of(
                            new PurchaseOrderReceiveRequest.ReceivedLine(10L, 10)));

            when(stockMovementService.createMovement(
                    eq(1L),
                    eq(MovementType.IN),
                    eq(10),
                    any(String.class),
                    eq(1L),
                    eq(10L))).thenReturn(mock(StockMovementResponse.class));

            purchaseOrderService.receivePurchaseOrder(1L, request);

            assertEquals(PurchaseOrderStatus.RECEIVED, order.getStatus());
            assertEquals(10, line.getQuantityReceived());

            verify(purchaseOrderRepository).save(order);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void receivePurchaseOrderShouldRejectZeroQuantity() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0006");
        order.setStatus(PurchaseOrderStatus.ORDERED);
        setId(order, 1L);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest(
                List.of(
                        new PurchaseOrderReceiveRequest.ReceivedLine(10L, 0)));

        assertThrows(
                IllegalArgumentException.class,
                () -> purchaseOrderService.receivePurchaseOrder(1L, request));

        verifyNoInteractions(stockMovementService);
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void receivePurchaseOrderShouldRejectDuplicateLines() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0007");
        order.setStatus(PurchaseOrderStatus.ORDERED);
        setId(order, 1L);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest(
                List.of(
                        new PurchaseOrderReceiveRequest.ReceivedLine(10L, 3),
                        new PurchaseOrderReceiveRequest.ReceivedLine(10L, 2)));

        assertThrows(
                IllegalArgumentException.class,
                () -> purchaseOrderService.receivePurchaseOrder(1L, request));

        verifyNoInteractions(stockMovementService);
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void receivePurchaseOrderShouldRejectNegativeQuantity() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0003");
        order.setStatus(PurchaseOrderStatus.ORDERED);
        setId(order, 1L);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest(
                List.of(new PurchaseOrderReceiveRequest.ReceivedLine(10L, -2)));

        assertThrows(
                IllegalArgumentException.class,
                () -> purchaseOrderService.receivePurchaseOrder(1L, request));

        verifyNoInteractions(stockMovementService);
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void receivePurchaseOrderShouldRejectDraftOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0008");
        order.setStatus(PurchaseOrderStatus.DRAFT);
        setId(order, 1L);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest(
                List.of(
                        new PurchaseOrderReceiveRequest.ReceivedLine(10L, 2)));

        assertThrows(
                IllegalStateException.class,
                () -> purchaseOrderService.receivePurchaseOrder(1L, request));

        verifyNoInteractions(stockMovementService);
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void receivePurchaseOrderShouldRejectCancelledOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0009");
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        setId(order, 1L);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest(
                List.of(
                        new PurchaseOrderReceiveRequest.ReceivedLine(10L, 2)));

        assertThrows(
                IllegalStateException.class,
                () -> purchaseOrderService.receivePurchaseOrder(1L, request));

        verifyNoInteractions(stockMovementService);
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void receivePurchaseOrderShouldRejectLineFromAnotherOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0010");
        order.setStatus(PurchaseOrderStatus.ORDERED);
        setId(order, 1L);

        PurchaseOrderLine line = new PurchaseOrderLine();
        setId(line, 20L);
        line.setPurchaseOrder(order);
        line.setQuantityOrdered(10);
        line.setQuantityReceived(0);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));
        when(purchaseOrderLineRepository.findByPurchaseOrderId(1L))
                .thenReturn(List.of(line));

        PurchaseOrderReceiveRequest request = new PurchaseOrderReceiveRequest(
                List.of(
                        new PurchaseOrderReceiveRequest.ReceivedLine(99L, 2)));

        assertThrows(
                IllegalArgumentException.class,
                () -> purchaseOrderService.receivePurchaseOrder(1L, request));

        verifyNoInteractions(stockMovementService);
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void cancelPurchaseOrderShouldRejectPartiallyReceivedOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("CMD-2026-0011");
        order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        setId(order, 1L);

        when(purchaseOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalStateException.class,
                () -> purchaseOrderService.cancelPurchaseOrder(1L));

        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
