package com.cosmocats.cosmo_cats.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

// Анотації над полями це правила перевірки. Вони повторюють контракт (YAML)
public record ProductRequest(
        // не порожня, від 3 до 100 символів
        @NotBlank
        @Size(min = 3, max = 100)
        String name,

        // необов'язкове поле, але не довше 500 символів
        @Size(max = 500)
        String description,

        // обов'язкова, більша за 0, не більше 2 цифр після коми
        @NotNull
        @Positive
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        // обов'язкове
        @NotNull
        UUID categoryId
) {
}
