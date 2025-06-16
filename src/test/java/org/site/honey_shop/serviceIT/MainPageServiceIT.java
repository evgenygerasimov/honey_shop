package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.site.honey_shop.service.MainPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MainPageServiceIT extends TestContainerConfig {

    @Autowired
    private MainPageService mainPageService;

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
    void testGetCategorizedProductsSorted_returnsOnlyVisibleCategoriesAndVisibleProductsSorted() {
        Category visibleCat1 = categoryRepository.save(Category.builder().name("Мёд").visible(true).showcaseOrder(1).build());
        Category visibleCat2 = categoryRepository.save(Category.builder().name("Пыльца").visible(true).showcaseOrder(2).build());
        Category invisibleCat = categoryRepository.save(Category.builder().name("Прополис").visible(false).showcaseOrder(3).build());

        productRepository.save(createProduct("Продукт B", visibleCat1, 2));
        productRepository.save(createProduct("Продукт A", visibleCat1, 1));
        productRepository.save(createProduct("Продукт X", visibleCat2, 1));
        productRepository.save(createProduct("Скрытый продукт", visibleCat1, 3, false));
        productRepository.save(createProduct("Невидимый продукт", invisibleCat, 1));

        Map<String, List<Product>> result = mainPageService.getCategorizedProductsSorted();

        assertThat(result).hasSize(2);
        assertThat(result.keySet()).containsExactly("Мёд", "Пыльца");

        List<Product> honeyProducts = result.get("Мёд");
        assertThat(honeyProducts).hasSize(2);
        assertThat(honeyProducts).extracting(Product::getName).containsExactly("Продукт A", "Продукт B");

        List<Product> pollenProducts = result.get("Пыльца");
        assertThat(pollenProducts).hasSize(1);
        assertThat(pollenProducts.get(0).getName()).isEqualTo("Продукт X");
    }

    private Product createProduct(String name, Category category, int showcaseOrder) {
        return createProduct(name, category, showcaseOrder, true);
    }

    private Product createProduct(String name, Category category, int showcaseOrder, boolean showInShowcase) {
        return Product.builder()
                .name(name)
                .shortDescription("Кратко")
                .description("Описание")
                .price(new BigDecimal("100.00"))
                .length(10.0)
                .width(5.0)
                .height(2.0)
                .weight(1.0)
                .stockQuantity(10)
                .images(List.of("img1.jpg"))
                .category(category)
                .showcaseOrder(showcaseOrder)
                .showInShowcase(showInShowcase)
                .build();
    }
}
