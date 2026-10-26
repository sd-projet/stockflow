package com.stockflow.api.category.repository;

import com.stockflow.api.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);
    // boolean existsByIdAndProductsIsNotEmpty(Long id);
}
