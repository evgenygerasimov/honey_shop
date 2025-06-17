package org.site.honey_shop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.entity.Order;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.MainPageService;
import org.site.honey_shop.service.PaymentCashService;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class MainPageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MainPageService mainPageService;

    @Mock
    private PaymentCashService paymentCashService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private MockHttpSession session;

    private MainPageController mainPageController;

    @BeforeEach
    void setUp() {
        when(session.getId()).thenReturn("mock-session-id");

        mainPageController = new MainPageController(mainPageService, paymentCashService, categoryService, session);

        var viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(mainPageController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void testHomePage() throws Exception {
        Product product = new Product();
        product.setName("Test Product");
        Category category = new Category();
        category.setName("Category 1");

        Map<String, List<Product>> categorizedProducts = Map.of(
                category.getName(), List.of(product)
        );

        List<Category> visibleCategories = List.of(category);

        when(mainPageService.getCategorizedProductsSorted()).thenReturn(categorizedProducts);
        when(paymentCashService.getPaymentSuccess("mock-session-id")).thenReturn(true);
        when(categoryService.findAllTrueVisibleCategories()).thenReturn(visibleCategories);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("categorizedProducts"))
                .andExpect(model().attribute("categorizedProducts", categorizedProducts))
                .andExpect(model().attributeExists("successPayment"))
                .andExpect(model().attribute("successPayment", true))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("categories", visibleCategories));

        verify(mainPageService).getCategorizedProductsSorted();
        verify(paymentCashService).getPaymentSuccess("mock-session-id");
        verify(categoryService).findAllTrueVisibleCategories();
    }

    @Test
    void testCart() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"));
    }

    @Test
    void testCheckout() throws Exception {
        mockMvc.perform(get("/checkout"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("order", instanceOf(Order.class)));
    }
}
