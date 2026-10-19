package com.cosmocats.cosmo_cats;

import com.cosmocats.cosmo_cats.domain.PageResult;
import com.cosmocats.cosmo_cats.domain.Product;
import com.cosmocats.cosmo_cats.exception.InvalidPageRequestException;
import com.cosmocats.cosmo_cats.exception.ProductAlreadyExistsException;
import com.cosmocats.cosmo_cats.exception.ProductNotFoundException;
import com.cosmocats.cosmo_cats.mapper.ProductMapper;
import com.cosmocats.cosmo_cats.service.ProductService;
import com.cosmocats.cosmo_cats.web.ProductController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
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

    // готовий продукт для відповідей сервісу
    private Product product(UUID id) {
        return new Product(id, "Galaxy yarn", "test", new BigDecimal("19.99"),
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    }

    // ---------- GET ----------

    @Test
    void shouldReturnProductById() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id)).thenReturn(product(id));

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Galaxy yarn"));
    }

    @Test
    void shouldReturnPageOfProducts() throws Exception {
        PageResult<Product> page = new PageResult<>(List.of(product(UUID.randomUUID())), 0, 20, 1, 1);
        when(productService.findPage(0, 20)).thenReturn(page);   // 0 і 20 це значення за замовчуванням

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Galaxy yarn"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn400WhenPageRequestIsInvalid() throws Exception {
        when(productService.findPage(0, 0))
                .thenThrow(new InvalidPageRequestException("Parameter 'size' must be between 1 and 100."));

        mockMvc.perform(get("/api/products").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenIdIsNotUuid() throws Exception {
        mockMvc.perform(get("/api/products/ne-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ---------- PUT ----------

    @Test
    void shouldReturn201WhenPutCreatesProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.upsert(any(Product.class)))
                .thenReturn(new ProductService.UpsertResult(product(id), true));

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Galaxy yarn", "19.99")))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn200WhenPutUpdatesProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.upsert(any(Product.class)))
                .thenReturn(new ProductService.UpsertResult(product(id), false));

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Galaxy yarn", "19.99")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenPutBodyIsInvalid() throws Exception {
        mockMvc.perform(put("/api/products/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Galaxy yarn", "-5")))
                .andExpect(status().isBadRequest());
    }

    // ---------- зламані дані ----------
    @Test
    void shouldReturn400WhenJsonIsBroken() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }


    // межі з контракту: ці довжини назви мають ПРОЙТИ
    @ParameterizedTest(name = "name of {0} chars is valid")
    @ValueSource(ints = {3, 100})
    void shouldAcceptNameOnLengthBoundary(int length) throws Exception {
        when(productService.create(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("a".repeat(length), "0.01")))
                .andExpect(status().isCreated());
    }

    // а ці мають бути ВІДХИЛЕНІ
    @ParameterizedTest(name = "name of {0} chars is invalid")
    @ValueSource(ints = {2, 101})
    void shouldRejectNameOutsideBoundary(int length) throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("a".repeat(length), "19.99")))
                .andExpect(status().isBadRequest());
    }

    // chaos-сценарії: помилка має бути у форматі RFC 9457
    @Test
    void shouldReturnProblemJsonForUnsupportedMethod() throws Exception {
        mockMvc.perform(patch("/api/products/{id}", UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldReturnProblemJsonWhenContentTypeIsNotJson() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
