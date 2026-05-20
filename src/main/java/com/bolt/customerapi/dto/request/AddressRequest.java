package com.bolt.customerapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank(message = "CEP é obrigatório") String cep,
        String numero,
        String complemento
) {}
