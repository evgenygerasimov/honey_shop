package org.site.honey_shop.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DeleteProductException.class)
    public ModelAndView handleDeleteProductException(DeleteProductException ex) {
        return new ModelAndView("error", "errorMessage", ex.getMessage());
    }

    @ExceptionHandler(ImageUploadException.class)
    public ModelAndView handleImageUploadException(ImageUploadException ex) {
        return new ModelAndView("error", "errorMessage", ex.getMessage());
    }

    @ExceptionHandler(MyAuthenticationException.class)
    public ModelAndView handleMyAuthenticationException(MyAuthenticationException ex) {
        return new ModelAndView("error", "errorMessage", ex.getMessage());
    }

    @ExceptionHandler(OrderCreateException.class)
    public ModelAndView handleOrderCreateException(OrderCreateException ex) {
        return new ModelAndView("error", "errorMessage", ex.getMessage());
    }

    @ExceptionHandler(ProductCreationException.class)
    public ModelAndView handleProductCreationException(ProductCreationException ex) {
        return new ModelAndView("error", "errorMessage", ex.getMessage());
    }
}

