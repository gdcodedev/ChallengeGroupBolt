package com.bolt.customerapi.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerResponse(
        Long id,
        String nome,
        String documento,
        boolean ativo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        AddressResponse endereco,
        List<ConsumerUnitResponse> consumerUnits
) {}
