package com.bolt.customerapi.integration;

import com.bolt.customerapi.exception.InvalidCepException;
import com.bolt.customerapi.integration.dto.ViaCepResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViaCepClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ViaCepClient viaCepClient;

    @BeforeEach
    void setUp() {
        viaCepClient = new ViaCepClient(restTemplate, "https://viacep.com.br/ws");
    }

    @Test
    void shouldReturnAddressForValidCep() {
        var response = new ViaCepResponse("30130-110", "Rua Teste", "Centro", "Belo Horizonte", "MG", null);
        when(restTemplate.getForObject("https://viacep.com.br/ws/30130110/json/", ViaCepResponse.class))
                .thenReturn(response);

        ViaCepResponse result = viaCepClient.buscar("30130-110");

        assertThat(result.uf()).isEqualTo("MG");
        assertThat(result.localidade()).isEqualTo("Belo Horizonte");
    }

    @Test
    void shouldThrowInvalidCepExceptionWhenErroFieldIsTrue() {
        var response = new ViaCepResponse(null, null, null, null, null, true);
        when(restTemplate.getForObject("https://viacep.com.br/ws/00000000/json/", ViaCepResponse.class))
                .thenReturn(response);

        assertThatThrownBy(() -> viaCepClient.buscar("00000-000"))
                .isInstanceOf(InvalidCepException.class)
                .hasMessageContaining("00000-000");
    }

    @Test
    void shouldThrowInvalidCepExceptionOnHttpClientError() {
        when(restTemplate.getForObject("https://viacep.com.br/ws/00000000/json/", ViaCepResponse.class))
                .thenThrow(HttpClientErrorException.class);

        assertThatThrownBy(() -> viaCepClient.buscar("00000-000"))
                .isInstanceOf(InvalidCepException.class);
    }
}
