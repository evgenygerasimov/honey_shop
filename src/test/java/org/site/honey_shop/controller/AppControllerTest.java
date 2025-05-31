package org.site.honey_shop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.site.honey_shop.entity.Order;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.service.MainPageService;
import org.site.honey_shop.service.PaymentCashService;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MainPageService mainPageService;

    @Mock
    private PaymentCashService paymentCashService;

    @InjectMocks
    private AppController appController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        var viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders
                .standaloneSetup(appController)
                .setViewResolvers(viewResolver)
                .build();
    }
    @Mock
    private MockHttpSession session;

//    @Test
//    void testShowHomePage() throws Exception {
//        Map<String, List<Product>> mockCategorizedProducts = Map.of(
//                "Category1", List.of(new Product(), new Product())
//        );
//        when(mainPageService.getAllProductsByCategoryAndSortByPrice()).thenReturn(mockCategorizedProducts);
//        when(paymentCashService.getPaymentSuccess(session.getId())).thenReturn(true);
//
//        mockMvc.perform(get("/").session(session))
//                .andExpect(status().isOk())
//                .andExpect(view().name("index"))
//                .andExpect(model().attribute("categorizedProducts", mockCategorizedProducts))
//                .andExpect(model().attribute("successPayment", true));
//    }

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