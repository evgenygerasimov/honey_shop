package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.ShowcaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ShowcaseServiceIT extends TestContainerConfig {

    @Autowired
    private ShowcaseService showcaseService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void clearDb() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testGetShowcase_returnsCategoriesOrderedByShowcaseOrder() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "honey.jpg", "image/jpeg", "some-image-data".getBytes());

        Category categoryToSave1 = Category.builder()
                .name("Пыльца")
                .visible(true)
                .showcaseOrder(0)
                .visible(true)
                .build();
        categoryService.saveCategoryWithImage(categoryToSave1, image);

        Category categoryToSave2 = Category.builder()
                .name("Мед")
                .visible(true)
                .showcaseOrder(1)
                .visible(true)
                .build();
        categoryService.saveCategoryWithImage(categoryToSave2, image);

        List<Category> allCategories = categoryService.findAll(); // или categoryRepository.findAll()
        System.out.println("Категории в базе после сохранения:");
        allCategories.forEach(c ->
                System.out.println("id=" + c.getCategoryId() +
                        ", name=" + c.getName() +
                        ", visible=" + c.getVisible() +
                        ", order=" + c.getShowcaseOrder()));

        List<Category> showcase = showcaseService.getShowcase();

        assertThat(showcase).hasSize(2);
        assertThat(showcase.get(0).getShowcaseOrder()).isLessThan(showcase.get(1).getShowcaseOrder());
        assertThat(showcase.get(0).getName()).isEqualTo("Пыльца");
        assertThat(showcase.get(1).getName()).isEqualTo("Мед");
    }

    @Test
    void testReorder_updatesCategoriesAndProductsOrder() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "honey.jpg", "image/jpeg", "some-image-data".getBytes());

        Category categoryToSave1 = Category.builder()
                .name("Пыльца")
                .visible(true)
                .build();

        Category savedCategory1 = categoryService.saveCategoryWithImage(categoryToSave1, image);

        Category categoryToSave2 = Category.builder()
                .name("Мед")
                .visible(true)
                .build();

        Category savedCategory2 = categoryService.saveCategoryWithImage(categoryToSave2, image);

        Product prod1 = buildProduct(savedCategory1);
        Product prod2 = buildProduct(savedCategory1);
        Product prod3 = buildProduct(savedCategory2);
        productRepository.saveAll(List.of(prod1, prod2, prod3));

        List<UUID> newCategoryOrder = List.of(savedCategory2.getCategoryId(), savedCategory1.getCategoryId());

        Map<UUID, List<UUID>> newProductOrder = Map.of(
                savedCategory2.getCategoryId(), List.of(prod3.getProductId()),
                savedCategory1.getCategoryId(), List.of(prod2.getProductId(), prod1.getProductId())
        );

        showcaseService.reorder(newCategoryOrder, newProductOrder);

        List<Category> categories = categoryRepository.findAllByOrderByShowcaseOrderAsc();
        assertThat(categories).hasSize(2);
        assertThat(categories.get(0).getCategoryId()).isEqualTo(savedCategory2.getCategoryId());
        assertThat(categories.get(0).getShowcaseOrder()).isEqualTo(0);
        assertThat(categories.get(1).getCategoryId()).isEqualTo(savedCategory1.getCategoryId());
        assertThat(categories.get(1).getShowcaseOrder()).isEqualTo(1);

        List<Product> cat1Products = productRepository.findByCategory_CategoryIdOrderByShowcaseOrderAsc(savedCategory1.getCategoryId());
        assertThat(cat1Products).hasSize(2);
        assertThat(cat1Products.get(0).getProductId()).isEqualTo(prod2.getProductId());
        assertThat(cat1Products.get(0).getShowcaseOrder()).isEqualTo(0);
        assertThat(cat1Products.get(1).getProductId()).isEqualTo(prod1.getProductId());
        assertThat(cat1Products.get(1).getShowcaseOrder()).isEqualTo(1);

        List<Product> cat2Products = productRepository.findByCategory_CategoryIdOrderByShowcaseOrderAsc(savedCategory2.getCategoryId());
        assertThat(cat2Products).hasSize(1);
        assertThat(cat2Products.get(0).getProductId()).isEqualTo(prod3.getProductId());
        assertThat(cat2Products.get(0).getShowcaseOrder()).isEqualTo(0);
    }

    private Product buildProduct(Category category) {
        return Product.builder()
                .name("Продукт 1")
                .description("Описание продукта")
                .shortDescription("Кратко")
                .price(new BigDecimal("10.00"))
                .length(10.0)
                .width(5.0)
                .height(3.0)
                .weight(0.5)
                .images(new java.util.ArrayList<>())
                .category(category)
                .stockQuantity(100)
                .build();
    }
}
