package com.ecommerce.address.dto;

public record AddressResponse(Long id, String recipientName, String line1, String line2,
                              String city, String state, String postalCode, String country,
                              boolean defaultAddress) {}
