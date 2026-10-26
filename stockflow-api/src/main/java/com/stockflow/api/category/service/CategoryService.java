
package com.stockflow.api.category.service;

import com.stockflow.api.category.Category;
import com.stockflow.api.category.dto.CategoryResponse;
import com.stockflow.api.category.dto.CategoryRequest;
import com.stockflow.api.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockflow.api.common.exception.DuplicateResourceException;
import com.stockflow.api.common.exception.ResourceNotFoundException;

import com.stockflow.api.product.repository.ProductRepository;
import com.stockflow.api.common.exception.ResourceInUseException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable avec l'identifiant : " + id));

        return toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(
                    "Une catégorie avec ce nom existe déjà");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(normalizeDescription(request.description()));

        Category savedCategory = categoryRepository.save(category);

        return toResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable avec l'identifiant : " + id));

        String name = request.name().trim();

        if (!category.getName().equalsIgnoreCase(name)
                && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(
                    "Une catégorie avec ce nom existe déjà");
        }

        category.setName(name);
        category.setDescription(normalizeDescription(request.description()));

        Category updatedCategory = categoryRepository.save(category);

        return toResponse(updatedCategory);
    }

    
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable avec l'identifiant : " + id));

        if (productRepository.existsByCategoryId(id)) {
            throw new ResourceInUseException(
                    "Impossible de supprimer cette catégorie : "
                    + "elle est associée à un ou plusieurs produits.");
        }

        categoryRepository.delete(category);
    }


    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription());
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
