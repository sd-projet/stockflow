
package com.stockflow.api.purchaseorder.service;

import com.stockflow.api.common.exception.DuplicateResourceException;
import com.stockflow.api.common.exception.ResourceNotFoundException;
import com.stockflow.api.product.Product;
import com.stockflow.api.purchaseorder.PurchaseOrder;
import com.stockflow.api.purchaseorder.PurchaseOrderLine;
import com.stockflow.api.purchaseorder.dto.PurchaseOrderCreateRequest;
import com.stockflow.api.purchaseorder.dto.PurchaseOrderLineRequest;
import com.stockflow.api.purchaseorder.dto.PurchaseOrderLineResponse;
import com.stockflow.api.purchaseorder.dto.PurchaseOrderResponse;
import com.stockflow.api.purchaseorder.dto.PurchaseOrderUpdateRequest;
import com.stockflow.api.purchaseorder.enums.PurchaseOrderStatus;
import com.stockflow.api.purchaseorder.repository.PurchaseOrderLineRepository;
import com.stockflow.api.purchaseorder.repository.PurchaseOrderRepository;
import com.stockflow.api.supplier.Supplier;
import com.stockflow.api.supplier.repository.SupplierRepository;
import com.stockflow.api.user.AppUser;
import com.stockflow.api.user.repository.AppUserRepository;

import com.stockflow.api.purchaseorder.dto.PurchaseOrderReceiveRequest;
import com.stockflow.api.stock.MovementType;
import com.stockflow.api.stock.service.StockMovementService;

import com.stockflow.api.product.SupplierProduct;
import com.stockflow.api.product.repository.SupplierProductRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Year;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PurchaseOrderService {

        private final PurchaseOrderRepository purchaseOrderRepository;
        private final PurchaseOrderLineRepository purchaseOrderLineRepository;
        private final SupplierRepository supplierRepository;
        private final AppUserRepository appUserRepository;
        private final com.stockflow.api.product.repository.ProductRepository productRepository;
        private final JdbcTemplate jdbcTemplate;
        private final StockMovementService stockMovementService;
        private final SupplierProductRepository supplierProductRepository;

        public PurchaseOrderService(
                        PurchaseOrderRepository purchaseOrderRepository,
                        PurchaseOrderLineRepository purchaseOrderLineRepository,
                        SupplierRepository supplierRepository,
                        AppUserRepository appUserRepository,
                        com.stockflow.api.product.repository.ProductRepository productRepository,
                        JdbcTemplate jdbcTemplate,
                        StockMovementService stockMovementService,
                        SupplierProductRepository supplierProductRepository

        ) {
                this.purchaseOrderRepository = purchaseOrderRepository;
                this.purchaseOrderLineRepository = purchaseOrderLineRepository;
                this.supplierRepository = supplierRepository;
                this.appUserRepository = appUserRepository;
                this.productRepository = productRepository;
                this.jdbcTemplate = jdbcTemplate;
                this.stockMovementService = stockMovementService;
                this.supplierProductRepository = supplierProductRepository;
        }

        @Transactional
        public PurchaseOrderResponse createPurchaseOrder(
                        PurchaseOrderCreateRequest request) {
                String orderNumber;

                if (request.orderNumber() == null || request.orderNumber().isBlank()) {
                        orderNumber = generateOrderNumber();
                } else {
                        orderNumber = request.orderNumber().trim();

                        if (purchaseOrderRepository.existsByOrderNumberIgnoreCase(orderNumber)) {
                                throw new DuplicateResourceException(
                                                "Une commande portant ce numéro existe déjà.");
                        }
                }

                validateDates(request.orderDate(), request.expectedDeliveryDate());

                Supplier supplier = supplierRepository.findById(request.supplierId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Fournisseur introuvable avec l'identifiant : "
                                                                + request.supplierId()));

                AppUser createdBy = getAuthenticatedUser();

                PurchaseOrder order = new PurchaseOrder();
                order.setOrderNumber(orderNumber);
                order.setSupplier(supplier);
                order.setStatus(PurchaseOrderStatus.DRAFT);
                order.setOrderDate(request.orderDate());
                order.setExpectedDeliveryDate(request.expectedDeliveryDate());
                order.setNotes(normalizeNotes(request.notes()));
                order.setCreatedBy(createdBy);

                purchaseOrderRepository.saveAndFlush(order);

                for (PurchaseOrderLineRequest lineRequest : request.lines()) {
                        Product product = productRepository.findById(lineRequest.productId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Produit introuvable avec l'identifiant : "
                                                                        + lineRequest.productId()));

                        PurchaseOrderLine line = new PurchaseOrderLine();
                        line.setPurchaseOrder(order);
                        line.setProduct(product);
                        line.setQuantityOrdered(lineRequest.quantityOrdered());
                        line.setQuantityReceived(0);
                        line.setUnitPrice(
                                        getSupplierUnitPrice(supplier.getId(), product.getId()));

                        purchaseOrderLineRepository.save(line);
                }

                return toResponse(order);
        }

        public List<PurchaseOrderResponse> getAllPurchaseOrders() {
                return purchaseOrderRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        public PurchaseOrderResponse getPurchaseOrderById(Long id) {
                return toResponse(findOrder(id));
        }

        @Transactional
        public PurchaseOrderResponse updatePurchaseOrder(
                        Long id,
                        PurchaseOrderUpdateRequest request) {
                PurchaseOrder order = findOrder(id);

                if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
                        throw new IllegalStateException(
                                        "Seules les commandes en brouillon peuvent être modifiées.");
                }

                String orderNumber = request.orderNumber().trim();

                boolean numberChanged = !order.getOrderNumber().equalsIgnoreCase(orderNumber);

                if (numberChanged
                                && purchaseOrderRepository
                                                .existsByOrderNumberIgnoreCase(orderNumber)) {
                        throw new DuplicateResourceException(
                                        "Une commande portant ce numéro existe déjà.");
                }

                validateDates(request.orderDate(), request.expectedDeliveryDate());

                Supplier supplier = supplierRepository.findById(request.supplierId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Fournisseur introuvable avec l'identifiant : "
                                                                + request.supplierId()));

                List<PurchaseOrderLine> existingLines = purchaseOrderLineRepository.findByPurchaseOrderId(id);

                purchaseOrderLineRepository.deleteAll(existingLines);
                purchaseOrderLineRepository.flush();

                order.setOrderNumber(orderNumber);
                order.setSupplier(supplier);
                order.setOrderDate(request.orderDate());
                order.setExpectedDeliveryDate(request.expectedDeliveryDate());
                order.setNotes(normalizeNotes(request.notes()));

                for (PurchaseOrderLineRequest lineRequest : request.lines()) {
                        Product product = productRepository.findById(lineRequest.productId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Produit introuvable avec l'identifiant : "
                                                                        + lineRequest.productId()));

                        PurchaseOrderLine line = new PurchaseOrderLine();
                        line.setPurchaseOrder(order);
                        line.setProduct(product);
                        line.setQuantityOrdered(lineRequest.quantityOrdered());
                        line.setQuantityReceived(0);
                        line.setUnitPrice(
                                        getSupplierUnitPrice(supplier.getId(), product.getId()));

                        purchaseOrderLineRepository.save(line);
                }

                purchaseOrderRepository.save(order);

                return toResponse(order);
        }

        private BigDecimal getSupplierUnitPrice(Long supplierId, Long productId) {
                SupplierProduct supplierProduct = supplierProductRepository
                                .findByProduct_IdAndSupplier_Id(productId, supplierId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Le produit " + productId
                                                                + " n'est pas associé au fournisseur " + supplierId
                                                                + "."));

                return supplierProduct.getUnitPrice();
        }

        private String generateOrderNumber() {
                int year = Year.now().getValue();

                Long sequence = jdbcTemplate.queryForObject(
                                """
                                                INSERT INTO purchase_order_counters (year, last_number)
                                                VALUES (?, 1)
                                                ON CONFLICT (year)
                                                DO UPDATE SET last_number =
                                                    purchase_order_counters.last_number + 1
                                                RETURNING last_number
                                                """,
                                Long.class,
                                year);

                return String.format("CMD-%d-%04d", year, sequence);
        }

        @Transactional
        public PurchaseOrderResponse orderPurchaseOrder(Long id) {
                PurchaseOrder order = findOrder(id);

                if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
                        throw new IllegalStateException(
                                        "Seules les commandes en brouillon peuvent être confirmées.");
                }

                order.setStatus(PurchaseOrderStatus.ORDERED);

                purchaseOrderRepository.save(order);

                return toResponse(order);
        }

        @Transactional
        public PurchaseOrderResponse receivePurchaseOrder(
                        Long id,
                        PurchaseOrderReceiveRequest request) {

                PurchaseOrder order = findOrder(id);

                if (order.getStatus() != PurchaseOrderStatus.ORDERED
                                && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
                        throw new IllegalStateException(
                                        "Seules les commandes confirmées ou partiellement reçues peuvent être réceptionnées.");
                }

                if (request == null || request.lines() == null || request.lines().isEmpty()) {
                        throw new IllegalArgumentException(
                                        "La réception doit contenir au moins une ligne.");
                }

                Set<Long> receivedLineIds = new HashSet<>();

                for (PurchaseOrderReceiveRequest.ReceivedLine receivedLine : request.lines()) {
                        if (receivedLine == null) {
                                throw new IllegalArgumentException(
                                                "Une ligne de réception ne peut pas être vide.");
                        }

                        if (receivedLine.lineId() == null || receivedLine.lineId() <= 0) {
                                throw new IllegalArgumentException(
                                                "L'identifiant de la ligne de commande doit être positif.");
                        }

                        if (receivedLine.quantityReceived() == null
                                        || receivedLine.quantityReceived() <= 0) {
                                throw new IllegalArgumentException(
                                                "La quantité reçue doit être strictement positive.");
                        }

                        if (!receivedLineIds.add(receivedLine.lineId())) {
                                throw new IllegalArgumentException(
                                                "Une même ligne de commande ne peut pas apparaître plusieurs fois dans une réception.");
                        }
                }

                List<PurchaseOrderLine> orderLines = purchaseOrderLineRepository.findByPurchaseOrderId(id);

                // Valider toutes les lignes avant de modifier le stock.
                for (PurchaseOrderReceiveRequest.ReceivedLine receivedLine : request.lines()) {

                        PurchaseOrderLine line = orderLines.stream()
                                        .filter(orderLine -> orderLine.getId().equals(receivedLine.lineId()))
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "La ligne " + receivedLine.lineId()
                                                                        + " n'appartient pas à cette commande."));

                        int remaining = line.getQuantityOrdered() - line.getQuantityReceived();

                        if (receivedLine.quantityReceived() > remaining) {
                                throw new IllegalArgumentException(
                                                "La quantité reçue dépasse la quantité restante pour la ligne "
                                                                + line.getId() + ".");
                        }
                }

                Long performedBy = getAuthenticatedUser().getId();

                // Enregistrer les mouvements et actualiser les quantités reçues.
                for (PurchaseOrderReceiveRequest.ReceivedLine receivedLine : request.lines()) {

                        PurchaseOrderLine line = orderLines.stream()
                                        .filter(orderLine -> orderLine.getId().equals(receivedLine.lineId()))
                                        .findFirst()
                                        .orElseThrow();

                        stockMovementService.createMovement(
                                        line.getProduct().getId(),
                                        MovementType.IN,
                                        receivedLine.quantityReceived(),
                                        "Réception commande " + order.getOrderNumber(),
                                        performedBy,
                                        line.getId());

                        line.setQuantityReceived(
                                        line.getQuantityReceived() + receivedLine.quantityReceived());
                }

                boolean fullyReceived = orderLines.stream()
                                .allMatch(line -> line.getQuantityReceived().equals(line.getQuantityOrdered()));

                boolean anyReceived = orderLines.stream()
                                .anyMatch(line -> line.getQuantityReceived() > 0);


                if (fullyReceived) {
                        order.setStatus(PurchaseOrderStatus.RECEIVED);

                        if (order.getReceivedDate() == null) {
                                order.setReceivedDate(java.time.LocalDate.now());
                        }
                } else if (anyReceived) {
                        order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
                }

                purchaseOrderRepository.save(order);

                return toResponse(order);
        }

        @Transactional
        public PurchaseOrderResponse cancelPurchaseOrder(Long id) {
                PurchaseOrder order = findOrder(id);

                if (order.getStatus() != PurchaseOrderStatus.DRAFT
                                && order.getStatus() != PurchaseOrderStatus.ORDERED) {
                        throw new IllegalStateException(
                                        "Seules les commandes en brouillon ou confirmées peuvent être annulées.");
                }

                order.setStatus(PurchaseOrderStatus.CANCELLED);

                purchaseOrderRepository.save(order);

                return toResponse(order);
        }

        private PurchaseOrder findOrder(Long id) {
                return purchaseOrderRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Commande fournisseur introuvable avec l'identifiant : "
                                                                + id));
        }

        private AppUser getAuthenticatedUser() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null
                                || !(authentication.getPrincipal() instanceof AppUser user)) {
                        throw new IllegalStateException(
                                        "Impossible d'identifier l'utilisateur connecté.");
                }

                return appUserRepository.findById(user.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Utilisateur connecté introuvable."));
        }

        private void validateDates(
                        LocalDate orderDate,
                        LocalDate expectedDeliveryDate) {
                if (orderDate != null
                                && expectedDeliveryDate != null
                                && expectedDeliveryDate.isBefore(orderDate)) {
                        throw new IllegalArgumentException(
                                        "La date de livraison prévue ne peut pas précéder la date de commande.");
                }
        }

        private String normalizeNotes(String notes) {
                if (notes == null || notes.isBlank()) {
                        return null;
                }

                return notes.trim();
        }

        @Transactional
        public PurchaseOrderResponse archivePurchaseOrder(Long id) {
                PurchaseOrder order = findOrder(id);
                if (order.isArchived()) {
                        throw new IllegalStateException("Cette commande est déjà archivée.");
                }
                order.setArchived(true);
                purchaseOrderRepository.save(order);
                return toResponse(order);
        }

        private PurchaseOrderResponse toResponse(PurchaseOrder order) {
                List<PurchaseOrderLineResponse> lines = purchaseOrderLineRepository.findByPurchaseOrderId(order.getId())
                                .stream()
                                .map(this::toLineResponse)
                                .toList();

                BigDecimal totalAmount = lines.stream()
                                .map(PurchaseOrderLineResponse::totalPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new PurchaseOrderResponse(
                                order.getId(),
                                order.getOrderNumber(),
                                order.getSupplier().getId(),
                                order.getSupplier().getName(),
                                order.getStatus(),
                                order.getOrderDate(),
                                order.getExpectedDeliveryDate(),
                                order.getReceivedDate(),
                                order.getNotes(),
                                order.isArchived(),
                                order.getCreatedBy().getId(),
                                order.getCreatedBy().getFullName(),
                                order.getCreatedAt(),
                                order.getUpdatedAt(),
                                lines,
                                totalAmount);
        }

        private PurchaseOrderLineResponse toLineResponse(PurchaseOrderLine line) {
                BigDecimal totalPrice = line.getUnitPrice()
                                .multiply(BigDecimal.valueOf(line.getQuantityOrdered()));

                return new PurchaseOrderLineResponse(
                                line.getId(),
                                line.getProduct().getId(),
                                line.getProduct().getName(),
                                line.getQuantityOrdered(),
                                line.getQuantityReceived(),
                                line.getUnitPrice(),
                                totalPrice);
        }
}
