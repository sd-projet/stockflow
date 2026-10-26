
package com.stockflow.api.category.service;

import com.stockflow.api.category.Category;
import com.stockflow.api.category.dto.CategoryRequest;
import com.stockflow.api.category.dto.CategoryResponse;
import com.stockflow.api.category.repository.CategoryRepository;
import com.stockflow.api.common.exception.DuplicateResourceException;
import com.stockflow.api.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.stockflow.api.common.exception.ResourceInUseException;
import com.stockflow.api.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

        @Mock
        private CategoryRepository categoryRepository;

        @Mock
        private ProductRepository productRepository;

        @InjectMocks
        private CategoryService categoryService;

        @Test
        void createShouldReturnCategoryWhenNameIsAvailable() {
                CategoryRequest request = new CategoryRequest("Informatique", "Matériel informatique");

                when(categoryRepository.existsByNameIgnoreCase("Informatique"))
                                .thenReturn(false);

                when(categoryRepository.save(any(Category.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CategoryResponse response = categoryService.create(request);

                assertEquals("Informatique", response.name());
                assertEquals("Matériel informatique", response.description());

                verify(categoryRepository).save(any(Category.class));
        }

        @Test
        void createShouldRejectDuplicateName() {
                CategoryRequest request = new CategoryRequest("Informatique", null);

                when(categoryRepository.existsByNameIgnoreCase("Informatique"))
                                .thenReturn(true);

                assertThrows(
                                DuplicateResourceException.class,
                                () -> categoryService.create(request));

                verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        void findByIdShouldRejectUnknownCategory() {
                when(categoryRepository.findById(999L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> categoryService.findById(999L));
        }

        @Test
        void updateShouldModifyExistingCategory() {
                Category category = new Category();
                category.setName("Informatique");
                category.setDescription("Ancienne description");

                when(categoryRepository.findById(1L))
                                .thenReturn(Optional.of(category));

                when(categoryRepository.save(any(Category.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CategoryRequest request = new CategoryRequest(
                                "Informatique",
                                "Nouvelle description");

                CategoryResponse response = categoryService.update(1L, request);

                assertEquals("Informatique", response.name());
                assertEquals("Nouvelle description", response.description());

                verify(categoryRepository).save(category);
        }

        @Test
        void updateShouldRejectUnknownCategory() {
                when(categoryRepository.findById(999L))
                                .thenReturn(Optional.empty());

                CategoryRequest request = new CategoryRequest("Informatique", "Description");

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> categoryService.update(999L, request));

                verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        void updateShouldRejectDuplicateName() {
                Category category = new Category();
                category.setName("Informatique");
                category.setDescription("Description");

                when(categoryRepository.findById(1L))
                                .thenReturn(Optional.of(category));

                when(categoryRepository.existsByNameIgnoreCase("Peripheriques"))
                                .thenReturn(true);

                CategoryRequest request = new CategoryRequest("Peripheriques", "Description");

                assertThrows(
                                DuplicateResourceException.class,
                                () -> categoryService.update(1L, request));

                verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        void deleteShouldThrowExceptionWhenCategoryIsUsedByProducts() {
                // Arrange
                Long categoryId = 1L;

                Category category = new Category();
                category.setName("Informatique");

                when(categoryRepository.findById(categoryId))
                        .thenReturn(Optional.of(category));

                when(productRepository.existsByCategoryId(categoryId))
                        .thenReturn(true);

                // Act & Assert
                assertThrows(
                        ResourceInUseException.class,
                        () -> categoryService.delete(categoryId)
                );

                verify(categoryRepository).findById(categoryId);
                verify(productRepository).existsByCategoryId(categoryId);
                //verifyNoInteractionsAfterLookup();
                org.mockito.Mockito.verify(categoryRepository,
                org.mockito.Mockito.never()).delete(category);
        }

}
