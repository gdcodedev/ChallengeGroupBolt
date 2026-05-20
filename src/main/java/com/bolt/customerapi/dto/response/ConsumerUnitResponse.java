package com.bolt.customerapi.dto.response;

public record ConsumerUnitResponse(
        Long id,
        String nome,
        String numeroInstalacao,
        AddressResponse endereco
) {}
