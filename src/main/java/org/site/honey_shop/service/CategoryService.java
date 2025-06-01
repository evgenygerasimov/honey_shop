package org.site.honey_shop.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.exception.ImageUploadException;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    @Value("${myapp.upload.image.directory}")
    public String UPLOAD_DIRECTORY;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public Category saveCategoryByName(String categoryName) {
        if (categoryRepository.existsByName(categoryName)) {
            log.error("Category name {} already exist", categoryName);
            throw new IllegalArgumentException("Категория с таким названием уже существует!");
        }
        Category category = Category.builder()
                .name(categoryName)
                .build();
        log.info("Attempt to save category by name: {}", categoryName);
        return categoryRepository.save(category);
    }

    public Category saveCategoryWithImage(Category category, MultipartFile image) {
        String imageUrl = imageSelectionProcessing(image);
        if (categoryRepository.existsByName(category.getName())) {
            throw new IllegalArgumentException("Категория с таким названием уже существует!");
        }
         category = Category.builder()
                .name(category.getName())
                .imageUrl(imageUrl)
                .visible(category.getVisible())
                .build();
        log.info("Attempt to save category: {}", category.getName());
        return categoryRepository.save(category);
    }

    public Category updateCategoryName(Category category) {
        Category existCategory = categoryRepository.findById(category.getCategoryId()).orElseThrow(()
                -> new IllegalArgumentException("Category not found"));
        existCategory.setName(category.getName());
        log.info("Update category: {}", category.getName());
        return categoryRepository.save(existCategory);
    }

    public Category fullUpdateCategory(Category category, MultipartFile image) {
        String imageUrl = imageSelectionProcessing(image);
        Category existCategory = categoryRepository.findById(category.getCategoryId()).orElseThrow(()
                -> new IllegalArgumentException("Category not found"));
        existCategory.setName(category.getName());
        if (!imageUrl.isEmpty()) {
            existCategory.setImageUrl(imageUrl);
        }
        existCategory.setVisible(category.getVisible());
        log.info("Full update category: {}", category.getName());
        return categoryRepository.save(existCategory);
    }

    public List<Category> findAll() {
        log.info("Get all categories");
        return categoryRepository.findAll(Sort.by("name"));
    }

    public List<Category> findAllTrueVisibleCategories() {
        log.info("Get all visible categories");
        return categoryRepository.findAllByVisibleTrueOrderByShowcaseOrderAsc();
    }

    public Category findById(UUID id) {
        log.info("Get category by id: {}", id);
        return categoryRepository.findById(id).orElse(null);
    }

    public Category findByName(String name) {
        log.info("Get category by name: {}", name);
        return categoryRepository.findByName(name);
    }

    public void deleteCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена"));

        List<Product> products = productRepository.findAllByCategory(category);
        for (Product product : products) {
            product.setCategory(null);
            log.info("Set category of product {} to null", product.getName());
        }
        productRepository.saveAll(products);
        log.info("Delete category: {}", category.getName());
        categoryRepository.delete(category);
    }

    public String imageSelectionProcessing(MultipartFile image) {
        String imagUrl = "";
        if (image != null && !image.isEmpty()) {
            String fileName = image.getOriginalFilename();
            if (fileName != null && !fileName.trim().isEmpty()) {
                Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY, fileName);
                imagUrl = "/assets/img/" + fileName;
                try {
                    Files.createDirectories(fileNameAndPath.getParent());
                    Files.write(fileNameAndPath, image.getBytes());
                } catch (IOException e) {
                    log.error("Error while uploading image file for user profile.", e);
                    throw new ImageUploadException("Ошибка загрузки изображения!");
                }
            }
        }
        return imagUrl;
    }

    public boolean removeImageFromCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        category.setImageUrl(null);
        categoryRepository.save(category);
        return true;
    }
}
