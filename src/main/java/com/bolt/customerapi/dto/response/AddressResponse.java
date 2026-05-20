package com.bolt.customerapi.dto.response;

public record AddressResponse(
        String cep,
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        String numero,
        String complemento
) {}
