package com.cosmocats.cosmo_cats.domain;

public enum OrderStatus {
    PENDING("Order received, waiting for payment"),
    PAID("Payment confirmed, preparing for shipment"),
    SHIPPED("Order is on the way"),
    DELIVERED("Order delivered successfully"),
    CANCELLED("Order was cancelled");

    private final String description;

    // Constructor must be private or package-private
    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
