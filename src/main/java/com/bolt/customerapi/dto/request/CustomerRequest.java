package com.bolt.customerapi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CustomerRequest(
        @NotBlank(message = "Nome é obrigatório") String nome,
        @NotBlank(message = "Documento é obrigatório") String documento,
        @NotNull @Valid AddressRequest endereco,
        @NotNull @Valid List<ConsumerUnitRequest> consumerUnits
) {}
