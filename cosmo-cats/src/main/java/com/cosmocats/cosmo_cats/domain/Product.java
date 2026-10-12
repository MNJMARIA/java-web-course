package com.cosmocats.cosmo_cats.domain;

import java.math.BigDecimal;
import java.util.UUID;

//record це клас, який Java сама наповнює конструктором, геттерами
public record Product (
        UUID id,
        String name,
        String description,
        BigDecimal price,
        UUID categoryId
) {

}
