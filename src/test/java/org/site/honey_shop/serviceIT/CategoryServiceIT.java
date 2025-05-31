package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CategoryServiceIT extends TestContainerConfig {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void clearDb() {
        categoryRepository.deleteAll();
    }

    @Test
    void testSaveCategoryWithImageCategory_success() {
        Category category = categoryService.saveCategoryByName("Мёд");

        assertThat(category.getCategoryId()).isNotNull();
        assertThat(category.getName()).isEqualTo("Мёд");

        Category fromDb = categoryRepository.findByName("Мёд");
        assertThat(fromDb).isNotNull();
        assertThat(fromDb.getName()).isEqualTo("Мёд");
    }

    @Test
    void testSaveCategoryWithImageCategory_duplicateName_throwsException() {
        categoryService.saveCategoryByName("Мёд");

        assertThatThrownBy(() -> categoryService.saveCategoryByName("Мёд"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Категория с таким названием уже существует!");
    }

    @Test
    void testFindAllCategories() {
        categoryService.saveCategoryByName("Мёд");
        categoryService.saveCategoryByName("Пыльца");

        List<Category> categories = categoryService.findAll();

        assertThat(categories).hasSize(2);
        assertThat(categories)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Мёд", "Пыльца");
    }

    @Test
    void testFindByName_existingCategory() {
        categoryService.saveCategoryByName("Прополис");

        Category category = categoryService.findByName("Прополис");

        assertThat(category).isNotNull();
        assertThat(category.getName()).isEqualTo("Прополис");
    }

    @Test
    void testFindByName_nonExistingCategory() {
        Category category = categoryService.findByName("Несуществующая");

        assertThat(category).isNull();
    }
}
