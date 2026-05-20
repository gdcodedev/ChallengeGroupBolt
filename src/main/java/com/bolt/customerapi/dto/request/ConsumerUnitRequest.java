package com.bolt.customerapi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConsumerUnitRequest(
        @NotBlank(message = "Nome da unidade consumidora é obrigatório") String nome,
        @NotBlank(message = "Número de instalação é obrigatório") String numeroInstalacao,
        @NotNull @Valid AddressRequest endereco
) {}
