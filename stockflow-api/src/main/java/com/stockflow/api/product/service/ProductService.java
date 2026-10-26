
package com.stockflow.api.product.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockflow.api.category.Category;
import com.stockflow.api.category.repository.CategoryRepository;
import com.stockflow.api.common.exception.DuplicateResourceException;
import com.stockflow.api.common.exception.ResourceNotFoundException;
import com.stockflow.api.product.Product;
import com.stockflow.api.product.SupplierProduct;
import com.stockflow.api.product.dto.ProductRequest;
import com.stockflow.api.product.dto.ProductResponse;
import com.stockflow.api.product.dto.SupplierProductRequest;
import com.stockflow.api.product.dto.SupplierProductResponse;
import com.stockflow.api.product.repository.ProductRepository;
import com.stockflow.api.product.repository.SupplierProductRepository;
import com.stockflow.api.supplier.Supplier;
import com.stockflow.api.supplier.repository.SupplierRepository;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            SupplierRepository supplierRepository,
            SupplierProductRepository supplierProductRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.supplierProductRepository = supplierProductRepository;
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> findAllActive() {
        return productRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> findLowStockProducts() {
        return productRepository.findLowStockProducts()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit introuvable avec l'identifiant : " + id));

        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String reference = request.reference().trim();

        if (productRepository.existsByReferenceIgnoreCase(reference)) {
            throw new DuplicateResourceException(
                    "Un produit avec cette référence existe déjà");
        }

        Category category = findCategory(request.categoryId());

        // Valider les fournisseurs avant de créer le produit.
        List<Supplier> suppliers = validateSuppliers(request.suppliers());

        Product product = new Product();
        product.setReference(reference);
        product.setName(request.name().trim());
        product.setDescription(normalizeDescription(request.description()));
        product.setCategory(category);
        product.setMinimumStock(request.minimumStock());
        product.setUnit(request.unit().trim());
        product.setUnitPrice(request.unitPrice());

        Product savedProduct = productRepository.save(product);

        saveSupplierAssociations(savedProduct, request.suppliers(), suppliers);

        return toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit introuvable avec l'identifiant : " + id));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalStateException(
                    "Impossible de modifier un produit désactivé.");
        }

        String reference = request.reference().trim();

        if (!product.getReference().equalsIgnoreCase(reference)
                && productRepository.existsByReferenceIgnoreCase(reference)) {
            throw new DuplicateResourceException(
                    "Un produit avec cette référence existe déjà");
        }

        Category category = findCategory(request.categoryId());

        // Vérifier les fournisseurs avant de modifier le produit.
        List<Supplier> suppliers = validateSuppliers(request.suppliers());

        product.setReference(reference);
        product.setName(request.name().trim());
        product.setDescription(normalizeDescription(request.description()));
        product.setCategory(category);
        product.setMinimumStock(request.minimumStock());
        product.setUnit(request.unit().trim());
        product.setUnitPrice(request.unitPrice());

        Product updatedProduct = productRepository.save(product);

        // Synchroniser les fournisseurs associés au produit.
        supplierProductRepository.deleteAllByProduct_Id(id);
        saveSupplierAssociations(
                updatedProduct,
                request.suppliers(),
                suppliers);

        return toResponse(updatedProduct);
    }

    @Transactional
    public ProductResponse addSupplier(Long productId, SupplierProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit introuvable avec l'identifiant : " + productId));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalStateException(
                    "Impossible de modifier un produit désactivé.");
        }

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fournisseur introuvable avec l'identifiant : "
                                + request.supplierId()));

        if (!Boolean.TRUE.equals(supplier.getActive())) {
            throw new IllegalArgumentException(
                    "Impossible d'associer un fournisseur désactivé.");
        }

        if (supplierProductRepository
                .findByProduct_IdAndSupplier_Id(productId, request.supplierId())
                .isPresent()) {
            throw new DuplicateResourceException(
                    "Ce fournisseur est déjà associé à ce produit.");
        }

        SupplierProduct association = new SupplierProduct();
        association.setProduct(product);
        association.setSupplier(supplier);
        association.setSupplierReference(
                normalizeSupplierReference(request.supplierReference()));
        association.setUnitPrice(request.unitPrice());

        supplierProductRepository.save(association);

        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateSupplier(
            Long productId,
            Long supplierId,
            SupplierProductRequest request) {

        if (!supplierId.equals(request.supplierId())) {
            throw new IllegalArgumentException(
                    "L'identifiant du fournisseur ne correspond pas à la demande.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit introuvable avec l'identifiant : " + productId));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalStateException(
                    "Impossible de modifier un produit désactivé.");
        }

        SupplierProduct association = supplierProductRepository
                .findByProduct_IdAndSupplier_Id(productId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Association fournisseur-produit introuvable."));

        if (!Boolean.TRUE.equals(association.getSupplier().getActive())) {
            throw new IllegalArgumentException(
                    "Impossible de modifier l'association d'un fournisseur désactivé.");
        }

        association.setSupplierReference(
                normalizeSupplierReference(request.supplierReference()));
        association.setUnitPrice(request.unitPrice());

        supplierProductRepository.save(association);

        return toResponse(product);
    }

    @Transactional
    public ProductResponse removeSupplier(Long productId, Long supplierId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit introuvable avec l'identifiant : " + productId));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalStateException(
                    "Impossible de modifier un produit désactivé.");
        }

        SupplierProduct association = supplierProductRepository
                .findByProduct_IdAndSupplier_Id(productId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Association fournisseur-produit introuvable."));

        supplierProductRepository.delete(association);

        return toResponse(product);
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit introuvable avec l'identifiant : " + id));

        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public ProductResponse reactivate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit introuvable avec l'identifiant : " + id));

        product.setActive(true);

        return toResponse(productRepository.save(product));
    }

    private List<Supplier> validateSuppliers(
            List<SupplierProductRequest> supplierRequests) {

        Set<Long> supplierIds = new HashSet<>();

        for (SupplierProductRequest supplierRequest : supplierRequests) {
            Long supplierId = supplierRequest.supplierId();

            if (!supplierIds.add(supplierId)) {
                throw new IllegalArgumentException(
                        "Un même fournisseur ne peut pas être associé "
                                + "plusieurs fois au produit.");
            }

            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Fournisseur introuvable avec l'identifiant : "
                                    + supplierId));

            if (!Boolean.TRUE.equals(supplier.getActive())) {
                throw new IllegalArgumentException(
                        "Le fournisseur " + supplier.getName()
                                + " est désactivé et ne peut pas être associé "
                                + "à un produit.");
            }
        }

        return supplierRequests.stream()
                .map(request -> supplierRepository.findById(request.supplierId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Fournisseur introuvable avec l'identifiant : "
                                        + request.supplierId())))
                .toList();
    }

    private void saveSupplierAssociations(
            Product product,
            List<SupplierProductRequest> supplierRequests,
            List<Supplier> suppliers) {

        for (int i = 0; i < supplierRequests.size(); i++) {
            SupplierProductRequest request = supplierRequests.get(i);
            Supplier supplier = suppliers.get(i);

            SupplierProduct association = new SupplierProduct();
            association.setProduct(product);
            association.setSupplier(supplier);
            association.setSupplierReference(
                    normalizeSupplierReference(request.supplierReference()));
            association.setUnitPrice(request.unitPrice());

            supplierProductRepository.save(association);
        }
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable avec l'identifiant : "
                                + categoryId));
    }

    private ProductResponse toResponse(Product product) {
        List<SupplierProductResponse> suppliers = supplierProductRepository.findAllByProduct_Id(product.getId())
                .stream()
                .map(association -> new SupplierProductResponse(
                        association.getSupplier().getId(),
                        association.getSupplier().getName(),
                        association.getSupplierReference(),
                        association.getUnitPrice()))
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getReference(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getQuantityInStock(),
                product.getMinimumStock(),
                product.getUnit(),
                product.getUnitPrice(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                suppliers);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    private String normalizeSupplierReference(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }

        return reference.trim();
    }
}
