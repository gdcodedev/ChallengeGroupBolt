package com.bolt.customerapi.integration;

import com.bolt.customerapi.exception.InvalidCepException;
import com.bolt.customerapi.integration.dto.ViaCepResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP para consulta de endereços na API pública ViaCEP (https://viacep.com.br).
 * Utilizado para preencher automaticamente os dados de endereço a partir do CEP informado.
 */
@Component
public class ViaCepClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ViaCepClient(RestTemplate restTemplate,
                        @Value("${viacep.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Consulta o endereço correspondente ao CEP informado.
     * O CEP é normalizado (remove máscara) antes da chamada: "01310-100" → "01310100".
     *
     * @param cep CEP com ou sem máscara
     * @return dados de endereço retornados pela API ViaCEP
     * @throws InvalidCepException se o CEP não existir ou a API retornar erro
     */
    public ViaCepResponse buscar(String cep) {
        // Remove caracteres não numéricos para aceitar CEP com ou sem máscara
        String cepNormalizado = cep.replaceAll("[^0-9]", "");
        String url = String.format("%s/%s/json/", baseUrl, cepNormalizado);

        try {
            ViaCepResponse response = restTemplate.getForObject(url, ViaCepResponse.class);

            // ViaCEP retorna { "erro": true } para CEPs inexistentes (não lança exceção HTTP)
            if (response == null || Boolean.TRUE.equals(response.erro())) {
                throw new InvalidCepException(cep);
            }
            return response;
        } catch (HttpClientErrorException e) {
            throw new InvalidCepException(cep);
        }
    }
}
