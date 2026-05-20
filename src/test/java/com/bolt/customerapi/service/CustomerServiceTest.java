package com.bolt.customerapi.service;

import com.bolt.customerapi.dto.request.AddressRequest;
import com.bolt.customerapi.dto.request.ConsumerUnitRequest;
import com.bolt.customerapi.dto.request.CustomerRequest;
import com.bolt.customerapi.entity.Customer;
import com.bolt.customerapi.exception.CustomerNotFoundException;
import com.bolt.customerapi.exception.DocumentAlreadyExistsException;
import com.bolt.customerapi.exception.RegionNotAllowedException;
import com.bolt.customerapi.integration.ViaCepClient;
import com.bolt.customerapi.integration.dto.ViaCepResponse;
import com.bolt.customerapi.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private ViaCepClient viaCepClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, viaCepClient, eventPublisher);
    }

    // --- Helpers ---
    private ViaCepResponse viaCepMG() {
        return new ViaCepResponse("30130-110", "Rua Teste", "Centro", "Belo Horizonte", "MG", null);
    }
    private ViaCepResponse viaCepSC() {
        return new ViaCepResponse("88010-000", "Rua X", "Centro", "Florianópolis", "SC", null);
    }
    private ViaCepResponse viaCepSP() {
        return new ViaCepResponse("01310-100", "Av Paulista", "Bela Vista", "São Paulo", "SP", null);
    }
    private ViaCepResponse viaCepRS() {
        return new ViaCepResponse("90010-000", "Rua X", "Centro", "Porto Alegre", "RS", null);
    }
    private ViaCepResponse viaCepPR() {
        return new ViaCepResponse("80010-000", "Rua X", "Centro", "Curitiba", "PR", null);
    }

    private CustomerRequest requestWithUnit(String unitCep) {
        return new CustomerRequest(
                "João Silva", "123.456.789-00",
                new AddressRequest("88010-000", "100", null),
                List.of(new ConsumerUnitRequest("Minha casa", "9001234",
                        new AddressRequest(unitCep, "1", null)))
        );
    }

    // --- Testes de criar ---

    @Test
    void shouldThrowWhenDocumentoAlreadyExists() {
        when(customerRepository.existsByDocumento("123.456.789-00")).thenReturn(true);

        CustomerRequest req = new CustomerRequest("João", "123.456.789-00",
                new AddressRequest("88010-000", "1", null), List.of());

        assertThatThrownBy(() -> customerService.create(req))
                .isInstanceOf(DocumentAlreadyExistsException.class)
                .hasMessageContaining("123.456.789-00");

        verify(viaCepClient, never()).buscar(anyString());
    }

    @Test
    void shouldThrowWhenUnitIsInSP() {
        when(customerRepository.existsByDocumento(any())).thenReturn(false);
        when(customerRepository.existsByConsumerUnitsNumeroInstalacao(any())).thenReturn(false);
        when(viaCepClient.buscar("88010-000")).thenReturn(viaCepSC());
        when(viaCepClient.buscar("01310-100")).thenReturn(viaCepSP());

        assertThatThrownBy(() -> customerService.create(requestWithUnit("01310-100")))
                .isInstanceOf(RegionNotAllowedException.class)
                .hasMessageContaining("SP");
    }

    @Test
    void shouldThrowWhenUnitIsInRS() {
        when(customerRepository.existsByDocumento(any())).thenReturn(false);
        when(customerRepository.existsByConsumerUnitsNumeroInstalacao(any())).thenReturn(false);
        when(viaCepClient.buscar("88010-000")).thenReturn(viaCepSC());
        when(viaCepClient.buscar("90010-000")).thenReturn(viaCepRS());

        assertThatThrownBy(() -> customerService.create(requestWithUnit("90010-000")))
                .isInstanceOf(RegionNotAllowedException.class)
                .hasMessageContaining("RS");
    }

    @Test
    void shouldThrowWhenUnitIsInPR() {
        when(customerRepository.existsByDocumento(any())).thenReturn(false);
        when(customerRepository.existsByConsumerUnitsNumeroInstalacao(any())).thenReturn(false);
        when(viaCepClient.buscar("88010-000")).thenReturn(viaCepSC());
        when(viaCepClient.buscar("80010-000")).thenReturn(viaCepPR());

        assertThatThrownBy(() -> customerService.create(requestWithUnit("80010-000")))
                .isInstanceOf(RegionNotAllowedException.class)
                .hasMessageContaining("PR");
    }

    @Test
    void shouldPublishMGEventWhenUnitIsInMG() {
        when(customerRepository.existsByDocumento(any())).thenReturn(false);
        when(customerRepository.existsByConsumerUnitsNumeroInstalacao(any())).thenReturn(false);
        when(viaCepClient.buscar("88010-000")).thenReturn(viaCepSC());
        when(viaCepClient.buscar("30130-110")).thenReturn(viaCepMG());

        Customer saved = new Customer();
        saved.setId(1L);
        saved.setDocumento("123.456.789-00");
        when(customerRepository.save(any())).thenReturn(saved);

        customerService.create(requestWithUnit("30130-110"));

        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void shouldNotPublishEventWhenNoUnitInMG() {
        when(customerRepository.existsByDocumento(any())).thenReturn(false);
        when(customerRepository.existsByConsumerUnitsNumeroInstalacao(any())).thenReturn(false);
        when(viaCepClient.buscar(any())).thenReturn(viaCepSC());

        Customer saved = new Customer();
        saved.setId(1L);
        saved.setDocumento("123.456.789-00");
        when(customerRepository.save(any())).thenReturn(saved);

        customerService.create(requestWithUnit("88010-000"));

        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- Testes de deletar ---

    @Test
    void shouldDeactivateCustomerOnDelete() {
        Customer customer = new Customer();
        customer.setId(1L);
        when(customerRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(customer));

        customerService.delete(1L);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().isAtivo()).isFalse();
    }

    @Test
    void shouldThrowWhenCustomerNotFoundOnDelete() {
        when(customerRepository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("99");
    }
}
