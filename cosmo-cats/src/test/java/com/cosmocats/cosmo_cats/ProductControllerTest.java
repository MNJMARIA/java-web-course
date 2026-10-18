package com.cosmocats.cosmo_cats;

import com.cosmocats.cosmo_cats.domain.Product;
import com.cosmocats.cosmo_cats.exception.ProductAlreadyExistsException;
import com.cosmocats.cosmo_cats.exception.ProductNotFoundException;
import com.cosmocats.cosmo_cats.mapper.ProductMapper;
import com.cosmocats.cosmo_cats.service.ProductService;
import com.cosmocats.cosmo_cats.web.ProductController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)  // піднімає тільки веб-шар
@Import(ProductMapper.class)          // mapper не входить у веб-шар, додаємо вручну
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;               // "фейковий Postman"

    @MockitoBean
    private ProductService productService; // фейковий сервіс, справжній тут не потрібен

    // JSON, який надсилає клієнт
    private String body(String name, String price) {
        return "{\"name\":\"" + name + "\",\"description\":\"test\",\"price\":" + price
                + ",\"categoryId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"}";
    }

    @Test
    void shouldCreateProduct() throws Exception {
        // сервіс повертає той самий продукт, який отримав
        when(productService.create(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Galaxy yarn", "19.99")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Galaxy yarn"));
    }

    @Test
    void shouldReturn400WhenPriceIsNegative() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Galaxy yarn", "-5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void shouldReturn409WhenNameAlreadyExists() throws Exception {
        when(productService.create(any(Product.class)))
                .thenThrow(new ProductAlreadyExistsException("Galaxy yarn"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Galaxy yarn", "19.99")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product not found"));
    }

    @Test
    void shouldReturn204WhenDeleting() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNoContent());

        verify(productService).delete(id);  // контролер справді викликав сервіс
    }
}
