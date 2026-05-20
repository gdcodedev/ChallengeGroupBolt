package com.bolt.customerapi.controller;

import com.bolt.customerapi.dto.request.CustomerRequest;
import com.bolt.customerapi.dto.response.CustomerResponse;
import com.bolt.customerapi.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para gerenciamento de clientes.
 * Todos os endpoints delegam a lógica de negócio ao {@link CustomerService}.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Gerenciamento de clientes")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar cliente")
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover cliente (remoção lógica)")
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }

    @GetMapping
    @Operation(summary = "Listar todos os clientes ativos")
    public List<CustomerResponse> findAll() {
        return customerService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter cliente por ID")
    public CustomerResponse findById(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @GetMapping("/recent")
    @Operation(summary = "Listar últimos 20 clientes em ordem decrescente de cadastro")
    public List<CustomerResponse> findRecent() {
        return customerService.findRecent();
    }
}
