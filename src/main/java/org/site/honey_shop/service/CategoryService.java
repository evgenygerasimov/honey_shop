package org.site.honey_shop.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category save(String categoryName) {
        if (categoryRepository.existsByName(categoryName)) {
            log.error("Category name {} already exist", categoryName);
            throw new IllegalArgumentException("Категория с таким названием уже существует!");
        }
        Category category = Category.builder()
                .name(categoryName)
                .build();
        log.info("Attempt to save category: {}", categoryName);
        return categoryRepository.save(category);
    }

    public List<Category> findAll() {
        log.info("Get all categories");
        return categoryRepository.findAll();
    }

    public Category findByName(String name) {
        log.info("Get category by name: {}", name);
        return categoryRepository.findByName(name);
    }
}
