package com.bolt.customerapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade principal que representa um cliente no sistema.
 * A remoção de clientes é lógica: o campo {@code ativo} é definido como {@code false}
 * em vez de excluir o registro do banco de dados.
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    /** Documento único por cliente (CPF/CNPJ). Validado antes da persistência. */
    @Column(nullable = false, unique = true)
    private String documento;

    /** Endereço preenchido automaticamente via consulta à API ViaCEP pelo CEP informado. */
    @Embedded
    private Address endereco;

    /**
     * Unidades consumidoras vinculadas ao cliente.
     * CascadeType.ALL garante que inserções/atualizações/remoções se propagam.
     * orphanRemoval remove unidades que saírem da lista na atualização.
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ConsumerUnit> consumerUnits = new ArrayList<>();

    /** Indica se o cliente está ativo. Falso representa remoção lógica. */
    private boolean ativo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** Executado automaticamente pelo JPA antes do primeiro INSERT. */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.ativo = true;
    }

    /** Executado automaticamente pelo JPA antes de cada UPDATE. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
