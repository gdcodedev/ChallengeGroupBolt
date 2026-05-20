package com.bolt.customerapi.service;

import com.bolt.customerapi.dto.request.AddressRequest;
import com.bolt.customerapi.dto.request.ConsumerUnitRequest;
import com.bolt.customerapi.dto.request.CustomerRequest;
import com.bolt.customerapi.dto.response.AddressResponse;
import com.bolt.customerapi.dto.response.ConsumerUnitResponse;
import com.bolt.customerapi.dto.response.CustomerResponse;
import com.bolt.customerapi.entity.Address;
import com.bolt.customerapi.entity.ConsumerUnit;
import com.bolt.customerapi.entity.Customer;
import com.bolt.customerapi.event.ClienteAnaliseMGEvent;
import com.bolt.customerapi.exception.CustomerNotFoundException;
import com.bolt.customerapi.exception.DocumentAlreadyExistsException;
import com.bolt.customerapi.exception.RegionNotAllowedException;
import com.bolt.customerapi.integration.ViaCepClient;
import com.bolt.customerapi.integration.dto.ViaCepResponse;
import com.bolt.customerapi.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Serviço central da aplicação. Concentra todas as regras de negócio
 * relacionadas ao cadastro, atualização, remoção e consulta de clientes.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {

    /** Estados nos quais não prestamos atendimento. */
    private static final Set<String> BLOCKED_STATES = Set.of("SP", "RS", "PR");

    private final CustomerRepository customerRepository;
    private final ViaCepClient viaCepClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Cadastra um novo cliente aplicando todas as regras de negócio:
     * unicidade de documento, validação de CEP via ViaCEP,
     * restrição de estados e publicação de evento para clientes em MG.
     */
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByDocumento(request.documento())) {
            throw new DocumentAlreadyExistsException("Documento já cadastrado: " + request.documento());
        }

        Address clienteEndereco = resolveAddress(request.endereco());
        List<ConsumerUnit> units = buildUnits(request.consumerUnits(), null);

        Customer customer = new Customer();
        customer.setNome(request.nome());
        customer.setDocumento(request.documento());
        customer.setEndereco(clienteEndereco);
        units.forEach(u -> {
            u.setCustomer(customer);
            customer.getConsumerUnits().add(u);
        });

        Customer saved = customerRepository.save(customer);
        publishMGEventIfNeeded(saved);
        return toResponse(saved);
    }

    /**
     * Atualiza os dados de um cliente ativo.
     * Revalida todas as regras de negócio com os novos dados.
     * A lista de unidades consumidoras é substituída completamente.
     */
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        if (customerRepository.existsByDocumentoAndIdNot(request.documento(), id)) {
            throw new DocumentAlreadyExistsException("Documento já cadastrado: " + request.documento());
        }

        Address clienteEndereco = resolveAddress(request.endereco());
        List<ConsumerUnit> units = buildUnits(request.consumerUnits(), id);

        customer.setNome(request.nome());
        customer.setDocumento(request.documento());
        customer.setEndereco(clienteEndereco);
        // orphanRemoval cuida da remoção das unidades antigas no banco
        customer.getConsumerUnits().clear();
        units.forEach(u -> {
            u.setCustomer(customer);
            customer.getConsumerUnits().add(u);
        });

        Customer saved = customerRepository.save(customer);
        publishMGEventIfNeeded(saved);
        return toResponse(saved);
    }

    /**
     * Realiza remoção lógica: define ativo=false sem excluir o registro.
     */
    public void delete(Long id) {
        Customer customer = customerRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        customer.setAtivo(false);
        customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findByAtivoTrue().stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return customerRepository.findByIdAndAtivoTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    /** Retorna os últimos 20 clientes ativos em ordem decrescente de cadastro. */
    @Transactional(readOnly = true)
    public List<CustomerResponse> findRecent() {
        return customerRepository.findTop20ByAtivoTrueOrderByCreatedAtDesc().stream()
                .map(this::toResponse).toList();
    }

    // -------------------------------------------------------------------------
    // Métodos auxiliares privados
    // -------------------------------------------------------------------------

    /**
     * Constrói e valida a lista de unidades consumidoras.
     * Para cada unidade: verifica unicidade do número de instalação,
     * busca o endereço via ViaCEP e bloqueia estados não atendidos.
     *
     * @param excludeCustomerId ID do cliente a ignorar na checagem de duplicidade (usado na atualização)
     */
    private List<ConsumerUnit> buildUnits(List<ConsumerUnitRequest> requests, Long excludeCustomerId) {
        List<ConsumerUnit> units = new ArrayList<>();
        for (ConsumerUnitRequest req : requests) {
            // Checagem de unicidade do número de instalação
            if (excludeCustomerId == null) {
                if (customerRepository.existsByConsumerUnitsNumeroInstalacao(req.numeroInstalacao())) {
                    throw new DocumentAlreadyExistsException("Número de instalação já cadastrado: " + req.numeroInstalacao());
                }
            } else {
                if (customerRepository.existsByNumeroInstalacaoAndCustomerIdNot(req.numeroInstalacao(), excludeCustomerId)) {
                    throw new DocumentAlreadyExistsException("Número de instalação já cadastrado: " + req.numeroInstalacao());
                }
            }

            Address addr = resolveAddress(req.endereco());

            // Rejeita unidades nos estados não atendidos
            if (BLOCKED_STATES.contains(addr.getUf())) {
                throw new RegionNotAllowedException(addr.getUf());
            }

            ConsumerUnit unit = new ConsumerUnit();
            unit.setNome(req.nome());
            unit.setNumeroInstalacao(req.numeroInstalacao());
            unit.setEndereco(addr);
            units.add(unit);
        }
        return units;
    }

    /** Consulta o endereço via ViaCEP e monta o objeto Address com os dados retornados. */
    private Address resolveAddress(AddressRequest req) {
        ViaCepResponse viaCep = viaCepClient.buscar(req.cep());
        return new Address(viaCep.cep(), viaCep.logradouro(), viaCep.bairro(),
                viaCep.localidade(), viaCep.uf(), req.numero(), req.complemento());
    }

    /** Publica o evento de análise se o cliente tiver ao menos uma unidade em MG. */
    private void publishMGEventIfNeeded(Customer customer) {
        boolean hasMG = customer.getConsumerUnits().stream()
                .anyMatch(u -> "MG".equals(u.getEndereco().getUf()));
        if (hasMG) {
            eventPublisher.publishEvent(
                    new ClienteAnaliseMGEvent(this, customer.getId(), customer.getDocumento()));
        }
    }

    // -------------------------------------------------------------------------
    // Mapeamento entidade → DTO de resposta
    // -------------------------------------------------------------------------

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getNome(), c.getDocumento(), c.isAtivo(),
                c.getCreatedAt(), c.getUpdatedAt(),
                toAddressResponse(c.getEndereco()),
                c.getConsumerUnits().stream().map(this::toUnitResponse).toList()
        );
    }

    private ConsumerUnitResponse toUnitResponse(ConsumerUnit u) {
        return new ConsumerUnitResponse(u.getId(), u.getNome(), u.getNumeroInstalacao(),
                toAddressResponse(u.getEndereco()));
    }

    private AddressResponse toAddressResponse(Address a) {
        if (a == null) return null;
        return new AddressResponse(a.getCep(), a.getLogradouro(), a.getBairro(),
                a.getLocalidade(), a.getUf(), a.getNumero(), a.getComplemento());
    }
}
