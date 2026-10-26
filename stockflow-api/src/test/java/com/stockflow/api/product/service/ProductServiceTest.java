package com.stockflow.api.product.service;

import com.stockflow.api.category.repository.CategoryRepository;
import com.stockflow.api.product.repository.ProductRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stockflow.api.category.Category;

import com.stockflow.api.product.Product;
import com.stockflow.api.product.dto.ProductRequest;
import com.stockflow.api.product.dto.ProductResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockflow.api.common.exception.ResourceNotFoundException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockflow.api.common.exception.DuplicateResourceException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

        @Mock
        private ProductRepository productRepository;

        @Mock
        private CategoryRepository categoryRepository;

        @InjectMocks
        private ProductService productService;

        @Test
        void findAllShouldReturnAllProducts() {
                // Arrange
                Category category = new Category();
                category.setName("Informatique");

                Product product = new Product();
                product.setReference("PROD-001");
                product.setName("Ordinateur portable");
                product.setCategory(category);

                when(productRepository.findAll())
                                .thenReturn(List.of(product));

                // Act
                List<ProductResponse> result = productService.findAll();

                // Assert
                assertEquals(1, result.size());
                assertEquals("PROD-001", result.get(0).reference());
                assertEquals("Ordinateur portable", result.get(0).name());

                verify(productRepository).findAll();
        }

        @Test
        void findByIdShouldReturnProductWhenItExists() {
                // Arrange
                Long productId = 1L;

                Category category = new Category();
                category.setName("Informatique");

                Product product = new Product();
                product.setReference("PROD-001");
                product.setName("Ordinateur portable");
                product.setCategory(category);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));

                // Act
                ProductResponse result = productService.findById(productId);

                // Assert
                assertEquals("PROD-001", result.reference());
                assertEquals("Ordinateur portable", result.name());

                verify(productRepository).findById(productId);
        }

        @Test
        void findByIdShouldThrowExceptionWhenProductDoesNotExist() {
                // Arrange
                Long productId = 999L;

                when(productRepository.findById(productId))
                                .thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(
                                ResourceNotFoundException.class,
                                () -> productService.findById(productId));

                verify(productRepository).findById(productId);
        }

        @Test
        void createShouldSaveProductWhenDataIsValid() {
                // Arrange
                Category category = new Category();
                category.setId(1L);

                category.setName("Informatique");

                when(productRepository.existsByReferenceIgnoreCase("PROD-003"))
                                .thenReturn(false);

                when(categoryRepository.findById(1L))
                                .thenReturn(Optional.of(category));

                when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                
                ProductRequest request = new ProductRequest(
                        " PROD-003 ",
                        " Souris sans fil ",
                        " Souris de test ",
                        1L,
                        2,
                        " pièce ",
                        new BigDecimal("4.00"),
                        List.of()
                );


                // Act
                ProductResponse result = productService.create(request);

                // Assert
                assertEquals("PROD-003", result.reference());
                assertEquals("Souris sans fil", result.name());
                assertEquals("Souris de test", result.description());
                assertEquals(1L, result.categoryId());
                assertEquals("Informatique", result.categoryName());
                assertEquals(2, result.minimumStock());
                assertEquals("pièce", result.unit());
                assertEquals(0, result.quantityInStock());

                verify(productRepository).save(org.mockito.ArgumentMatchers.any(Product.class));
        }

        @Test
        void createShouldThrowExceptionWhenReferenceAlreadyExists() {
                // Arrange
                when(productRepository.existsByReferenceIgnoreCase("PROD-001"))
                                .thenReturn(true);

                ProductRequest request = new ProductRequest(
                        "PROD-001",
                        "Ordinateur portable",
                        "Ordinateur de test",
                        1L,
                        5,
                        "pièce",
                        new BigDecimal("4.00"),
                        List.of()
                );

                // Act & Assert
                assertThrows(
                                DuplicateResourceException.class,
                                () -> productService.create(request));

                verify(productRepository)
                                .existsByReferenceIgnoreCase("PROD-001");

                verify(productRepository, org.mockito.Mockito.never()).save(
                                org.mockito.ArgumentMatchers.any(Product.class));
        }

        @Test
        void createShouldThrowExceptionWhenCategoryDoesNotExist() {
                // Arrange
                when(productRepository.existsByReferenceIgnoreCase("PROD-004"))
                                .thenReturn(false);

                when(categoryRepository.findById(999L))
                                .thenReturn(Optional.empty());

                ProductRequest request = new ProductRequest(
                        "PROD-004",
                        "Écran",
                        "Écran professionnel",
                        999L,
                        3,
                        "pièce",
                        new BigDecimal("4.00"),
                        List.of()
                );

                // Act & Assert
                assertThrows(
                                ResourceNotFoundException.class,
                                () -> productService.create(request));

                verify(productRepository, org.mockito.Mockito.never())
                                .save(org.mockito.ArgumentMatchers.any(Product.class));
        }

        @Test
        void createShouldThrowExceptionWhenReferenceDiffersOnlyByCase() {
                // Arrange
                when(productRepository.existsByReferenceIgnoreCase("prod-001"))
                                .thenReturn(true);

                ProductRequest request = new ProductRequest(
                        "prod-001",
                        "Ordinateur portable",
                        "Ordinateur de test",
                        1L,
                        5,
                        "pièce",
                        new BigDecimal("4.00"),
                        List.of()
                );

                // Act & Assert
                assertThrows(
                                DuplicateResourceException.class,
                                () -> productService.create(request));

                verify(productRepository)
                                .existsByReferenceIgnoreCase("prod-001");
        }

        @Test
        void updateShouldModifyProductWhenDataIsValid() {
                // Arrange
                Long productId = 1L;

                Category category = new Category();
                category.setId(1L);
                category.setName("Informatique");

                Product product = new Product();
                product.setReference("PROD-001");
                product.setName("Ancien nom");
                product.setCategory(category);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));
                when(productRepository.existsByReferenceIgnoreCase("PROD-002"))
                                .thenReturn(false);
                when(categoryRepository.findById(1L))
                                .thenReturn(Optional.of(category));
                when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ProductRequest request = new ProductRequest(
                        "PROD-002",
                        "Nouveau nom",
                        "Nouvelle description",
                        1L,
                        5,
                        "pièce",
                        new BigDecimal("4.00"),
                        List.of()
                );

                // Act
                ProductResponse result = productService.update(productId, request);

                // Assert
                assertEquals("PROD-002", result.reference());
                assertEquals("Nouveau nom", result.name());
                assertEquals("Nouvelle description", result.description());
                assertEquals(1L, result.categoryId());
                assertEquals(5, result.minimumStock());

                verify(productRepository).save(product);
        }

        @Test
        void updateShouldThrowExceptionWhenProductDoesNotExist() {
                // Arrange
                Long productId = 999L;

                when(productRepository.findById(productId))
                                .thenReturn(Optional.empty());

                ProductRequest request = new ProductRequest(
                        "PROD-009",
                        "Produit test",
                        "Description test",
                        1L,
                        5,
                        "pièce",
                        new BigDecimal("4.00"),
                        List.of()
                );

                // Act & Assert
                assertThrows(
                                ResourceNotFoundException.class,
                                () -> productService.update(productId, request));

                verify(productRepository, org.mockito.Mockito.never())
                                .save(org.mockito.ArgumentMatchers.any(Product.class));
        }

        @Test
        void updateShouldThrowExceptionWhenReferenceAlreadyExists() {
                // Arrange
                Long productId = 1L;

                Category category = new Category();
                category.setId(1L);
                category.setName("Informatique");

                Product product = new Product();
                product.setReference("PROD-001");
                product.setName("Ordinateur portable");
                product.setCategory(category);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));
                when(productRepository.existsByReferenceIgnoreCase("PROD-002"))
                                .thenReturn(true);

                ProductRequest request = new ProductRequest(
                        "PROD-002",
                        "Nouveau nom",
                        "Description test",
                        1L,
                        5,
                        "pièce",
                        new BigDecimal("4.00"),
                        List.of()
                );

                // Act & Assert
                assertThrows(
                                DuplicateResourceException.class,
                                () -> productService.update(productId, request));

                verify(productRepository, org.mockito.Mockito.never())
                                .save(org.mockito.ArgumentMatchers.any(Product.class));
        }

        @Test
        void updateShouldThrowExceptionWhenCategoryDoesNotExist() {
                // Arrange
                Long productId = 1L;

                Category oldCategory = new Category();
                oldCategory.setId(1L);
                oldCategory.setName("Informatique");

                Product product = new Product();
                product.setReference("PROD-001");
                product.setName("Ordinateur portable");
                product.setCategory(oldCategory);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));
                when(productRepository.existsByReferenceIgnoreCase("PROD-002"))
                                .thenReturn(false);
                when(categoryRepository.findById(999L))
                                .thenReturn(Optional.empty());

                ProductRequest request = new ProductRequest(
                        "PROD-002",
                        "Nouveau nom",
                        "Description test",
                        999L,
                        5,
                        "pièce",
                        new BigDecimal("4.00"),
                        List.of()
                );

                // Act & Assert
                assertThrows(
                                ResourceNotFoundException.class,
                                () -> productService.update(productId, request));

                verify(productRepository, org.mockito.Mockito.never())
                                .save(org.mockito.ArgumentMatchers.any(Product.class));
        }

        @Test
        void deactivateShouldSetProductAsInactive() {
                // Arrange
                Long productId = 1L;

                Product product = new Product();
                product.setReference("PROD-001");
                product.setName("Ordinateur portable");
                product.setActive(true);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));

                // Act
                productService.deactivate(productId);

                // Assert
                assertEquals(false, product.getActive());
                verify(productRepository).save(product);
        }

        @Test
        void deactivateShouldThrowExceptionWhenProductDoesNotExist() {
                // Arrange
                Long productId = 999L;

                when(productRepository.findById(productId))
                                .thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(
                                ResourceNotFoundException.class,
                                () -> productService.deactivate(productId));

                verify(productRepository, org.mockito.Mockito.never())
                                .save(org.mockito.ArgumentMatchers.any(Product.class));
        }

        @Test
        void updateShouldThrowExceptionWhenProductIsInactive() {
                // Arrange
                Long productId = 1L;

                Product product = new Product();
                product.setReference("PROD-001");
                product.setName("Ordinateur portable");
                product.setActive(false);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));

                ProductRequest request = new ProductRequest(
                        "PROD-001",
                        "Nouveau nom",
                        "Description test",
                        1L,
                        5,
                        "pièce",
                        new BigDecimal("4.00"),
                        List.of()
                );

                // Act & Assert
                assertThrows(
                                IllegalStateException.class,
                                () -> productService.update(productId, request));

                verify(productRepository, org.mockito.Mockito.never())
                                .save(org.mockito.ArgumentMatchers.any(Product.class));
        }

        @Test
        void updateShouldAllowModificationWhenProductIsActive() {
                // Arrange
                Long productId = 2L;

                Category category = new Category();
                category.setId(1L);
                category.setName("Informatique");

                Product product = new Product();
                product.setReference("PROD-002");
                product.setName("Clavier sans fil");
                product.setCategory(category);
                product.setActive(true);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));
                when(categoryRepository.findById(1L))
                                .thenReturn(Optional.of(category));
                when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ProductRequest request = new ProductRequest(
                        "PROD-002",
                        "Clavier sans fil amélioré",
                        "Clavier de test",
                        1L,
                        3,
                        "pièce",
                        new BigDecimal("4.00"),
                        List.of()
                );

                // Act
                ProductResponse result = productService.update(productId, request);

                // Assert
                assertEquals("Clavier sans fil amélioré", result.name());
                assertEquals(3, result.minimumStock());
                verify(productRepository).save(product);
        }

        @Test
        void updateShouldNotChangeStockQuantity() {
                // Arrange
                Long productId = 2L;

                Category category = new Category();
                category.setId(1L);
                category.setName("Informatique");

                Product product = new Product();
                product.setReference("PROD-002");
                product.setName("Clavier");
                product.setCategory(category);
                product.setQuantityInStock(15);
                product.setMinimumStock(5);
                product.setActive(true);

                when(productRepository.findById(productId))
                                .thenReturn(Optional.of(product));
                when(categoryRepository.findById(1L))
                                .thenReturn(Optional.of(category));
                when(productRepository.save(any(Product.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ProductRequest request = new ProductRequest(
                        "PROD-002",
                        "Clavier amélioré",
                        "Description modifiée",
                        1L,
                        8,
                        "pièce",
                        new BigDecimal("4.00"),
                        List.of()
                );

                // Act
                ProductResponse result = productService.update(productId, request);

                // Assert
                assertEquals(15, result.quantityInStock());
                assertEquals(8, result.minimumStock());
        }

        @Test
        void findAllActiveShouldReturnOnlyActiveProducts() {
                // Arrange
                Category category = new Category();
                category.setId(1L);
                category.setName("Informatique");

                Product activeProduct = new Product();
                activeProduct.setReference("PROD-002");
                activeProduct.setName("Clavier");
                activeProduct.setCategory(category);
                activeProduct.setActive(true);

                when(productRepository.findByActiveTrue())
                                .thenReturn(List.of(activeProduct));

                // Act
                List<ProductResponse> result = productService.findAllActive();

                // Assert
                assertEquals(1, result.size());
                assertEquals("PROD-002", result.get(0).reference());
                assertEquals(true, result.get(0).active());

                verify(productRepository).findByActiveTrue();
        }

        @Test
        void findAllActiveShouldReturnEmptyListWhenNoActiveProductsExist() {
                // Arrange
                when(productRepository.findByActiveTrue())
                                .thenReturn(List.of());

                // Act
                List<ProductResponse> result = productService.findAllActive();

                // Assert
                assertTrue(result.isEmpty());

                verify(productRepository).findByActiveTrue();
        }

}