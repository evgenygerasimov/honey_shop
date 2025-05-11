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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MainPageServiceIT extends TestContainerConfig {

    @Autowired
    private MainPageService mainPageService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void clearDb() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testGetAllProductsByCategoryAndSortByPrice_success() {
        Category honey = categoryRepository.save(Category.builder().name("Honey").build());
        Category tea = categoryRepository.save(Category.builder().name("Tea").build());

        productRepository.save(createValidProduct("Acacia Honey", BigDecimal.valueOf(12.99), honey));
        productRepository.save(createValidProduct("Wildflower Honey", BigDecimal.valueOf(9.99), honey));
        productRepository.save(createValidProduct("Green Tea", BigDecimal.valueOf(5.49), tea));
        productRepository.save(createValidProduct("Black Tea", BigDecimal.valueOf(4.99), tea));

        Map<String, List<Product>> result = mainPageService.getAllProductsByCategoryAndSortByPrice();

        assertThat(result).hasSize(2);

        List<Product> honeyProducts = result.get("Honey");
        assertThat(honeyProducts).hasSize(2);
        assertThat(honeyProducts.get(0).getName()).isEqualTo("Wildflower Honey");
        assertThat(honeyProducts.get(1).getName()).isEqualTo("Acacia Honey");

        List<Product> teaProducts = result.get("Tea");
        assertThat(teaProducts).hasSize(2);
        assertThat(teaProducts.get(0).getName()).isEqualTo("Black Tea");
        assertThat(teaProducts.get(1).getName()).isEqualTo("Green Tea");
    }

    private Product createValidProduct(String name, BigDecimal price, Category category) {
        return Product.builder()
                .name(name)
                .shortDescription("Short " + name)
                .description("Full description of " + name)
                .images(singletonList("http://image.url/" + name.replace(" ", "_")))
                .price(price)
                .length(10.0)
                .width(5.0)
                .height(3.0)
                .weight(1.5)
                .stockQuantity(10)
                .category(category)
                .build();
    }
}
