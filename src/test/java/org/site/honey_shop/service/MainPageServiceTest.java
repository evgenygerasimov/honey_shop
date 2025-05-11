package org.site.honey_shop.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.service.MainPageService;
import org.site.honey_shop.service.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainPageServiceTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private MainPageService mainPageService;

    @Test
    void testGetAllProductsByCategoryAndSortByPrice_Success() {
        // Подготовка данных
        Category buckwheat = Category.builder().name("Гречишный").build();
        Category mixedGrass = Category.builder().name("Разнотравье").build();

        Product p1 = Product.builder().name("Мёд 1").price(BigDecimal.valueOf(500)).category(buckwheat).build();
        Product p2 = Product.builder().name("Мёд 2").price(BigDecimal.valueOf(300)).category(buckwheat).build();
        Product p3 = Product.builder().name("Мед 3").price(BigDecimal.valueOf(200)).category(mixedGrass).build();
        Product p4 = Product.builder().name("Без категории").price(BigDecimal.valueOf(100)).category(null).build();

        List<Product> allProducts = List.of(p1, p2, p3, p4);
        when(productService.getAllProducts()).thenReturn(allProducts);

        Map<String, List<Product>> result = mainPageService.getAllProductsByCategoryAndSortByPrice();

        assertEquals(2, result.size());

        List<Product> buckwheatProducts = result.get("Гречишный");
        assertNotNull(buckwheatProducts);
        assertEquals(2, buckwheatProducts.size());
        assertEquals("Мёд 2", buckwheatProducts.get(0).getName());
        assertEquals("Мёд 1", buckwheatProducts.get(1).getName());

        List<Product> mixedGrassProducts = result.get("Разнотравье");
        assertNotNull(mixedGrassProducts);
        assertEquals(1, mixedGrassProducts.size());
        assertEquals("Мед 3", mixedGrassProducts.get(0).getName());

        assertFalse(result.containsKey(null));

        verify(productService).getAllProducts();
    }
}
