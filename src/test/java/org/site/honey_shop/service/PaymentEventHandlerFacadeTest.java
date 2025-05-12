package org.site.honey_shop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentEventHandlerFacadeTest {

    @Mock
    private PaymentEventHandlerService paymentEventHandlerService;

    private PaymentEventHandlerFacade facade;

    @BeforeEach
    void setUp() {
        facade = new PaymentEventHandlerFacade(paymentEventHandlerService, new ObjectMapper());
    }


    @Test
    void testHandlePaymentSucceeded() {
        String payload = """
                {
                  "event": "payment.succeeded",
                  "object": {
                    "description": "Order #123"
                  }
                }
                """;

        facade.eventHandler(payload);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(paymentEventHandlerService).handlePaymentSucceeded(captor.capture());
        assert captor.getValue().get("event").equals("payment.succeeded");
    }

    @Test
    void testHandlePaymentCanceled() {
        String payload = """
                {
                  "event": "payment.canceled",
                  "object": {
                    "description": "Order #456"
                  }
                }
                """;

        facade.eventHandler(payload);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(paymentEventHandlerService).handlePaymentCanceled(captor.capture());
        assert captor.getValue().get("event").equals("payment.canceled");
    }

    @Test
    void testHandleRefundSucceeded() {
        String payload = """
                {
                  "event": "refund.succeeded",
                  "object": {
                    "description": "Order #789"
                  }
                }
                """;

        facade.eventHandler(payload);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(paymentEventHandlerService).handleRefundSucceeded(captor.capture());
        assert captor.getValue().get("event").equals("refund.succeeded");
    }

    @Test
    void testHandleUnknownEvent() {
        String payload = """
                {
                  "event": "unknown.event",
                  "object": {
                    "description": "Some order"
                  }
                }
                """;

        facade.eventHandler(payload);

        verifyNoInteractions(paymentEventHandlerService);
    }

    @Test
    void testHandleInvalidJson() {
        String invalidJson = "{ this is not valid json }";

        facade.eventHandler(invalidJson);

        verifyNoInteractions(paymentEventHandlerService);
    }
}
