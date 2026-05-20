package com.bolt.customerapi.controller;

import com.bolt.customerapi.dto.request.AddressRequest;
import com.bolt.customerapi.dto.request.ConsumerUnitRequest;
import com.bolt.customerapi.dto.request.CustomerRequest;
import com.bolt.customerapi.integration.ViaCepClient;
import com.bolt.customerapi.integration.dto.ViaCepResponse;
import com.bolt.customerapi.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração que sobem o contexto Spring completo com H2 em memória.
 * O ViaCepClient é mockado para evitar chamadas reais à API externa.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CustomerRepository customerRepository;
    @MockBean  private ViaCepClient viaCepClient;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    private ViaCepResponse viaCepSC() {
        return new ViaCepResponse("88010-000", "Rua XV", "Centro", "Florianópolis", "SC", null);
    }
    private ViaCepResponse viaCepSP() {
        return new ViaCepResponse("01310-100", "Av Paulista", "Bela Vista", "São Paulo", "SP", null);
    }

    private CustomerRequest validRequest(String documento, String numInstalacao) {
        return new CustomerRequest("João Silva", documento,
                new AddressRequest("88010-000", "100", null),
                List.of(new ConsumerUnitRequest("Minha casa", numInstalacao,
                        new AddressRequest("88010-000", "1", null))));
    }

    @Test
    void shouldCreateCustomerAndReturn201() throws Exception {
        when(viaCepClient.buscar(anyString())).thenReturn(viaCepSC());

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("123.456.789-00", "9001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.consumerUnits", hasSize(1)));
    }

    @Test
    void shouldReturn409WhenDocumentoAlreadyExists() throws Exception {
        when(viaCepClient.buscar(anyString())).thenReturn(viaCepSC());
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("123.456.789-00", "9001"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("123.456.789-00", "9002"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturn422WhenUnitInBlockedState() throws Exception {
        when(viaCepClient.buscar("88010-000")).thenReturn(viaCepSC());
        when(viaCepClient.buscar("01310-100")).thenReturn(viaCepSP());

        CustomerRequest req = new CustomerRequest("João", "999.999.999-00",
                new AddressRequest("88010-000", "1", null),
                List.of(new ConsumerUnitRequest("Casa", "9003",
                        new AddressRequest("01310-100", "1", null))));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void shouldReturnCustomerById() throws Exception {
        when(viaCepClient.buscar(anyString())).thenReturn(viaCepSC());
        String response = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("111.222.333-44", "8001"))))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/customers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        mockMvc.perform(get("/api/customers/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldLogicallyDeleteCustomer() throws Exception {
        when(viaCepClient.buscar(anyString())).thenReturn(viaCepSC());
        String response = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("555.666.777-88", "7001"))))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/customers/" + id))
                .andExpect(status().isNoContent());

        // Cliente inativo deve retornar 404
        mockMvc.perform(get("/api/customers/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAtMost20InRecent() throws Exception {
        when(viaCepClient.buscar(anyString())).thenReturn(viaCepSC());
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    validRequest("00" + i + ".000.000-00", "100" + i))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/customers/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(20))));
    }
}
