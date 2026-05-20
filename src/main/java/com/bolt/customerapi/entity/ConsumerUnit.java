package com.bolt.customerapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa uma unidade consumidora vinculada a um cliente.
 * Exemplos: "Minha casa", "Meu comércio".
 * O número de instalação deve ser único globalmente — não pode ser compartilhado entre clientes.
 */
@Entity
@Table(name = "consumer_unit")
@Getter
@Setter
@NoArgsConstructor
public class ConsumerUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome identificador da unidade consumidora (ex.: "Residência Principal"). */
    @Column(nullable = false)
    private String nome;

    /** Número de instalação único no sistema — regra de negócio impede duplicidade entre clientes. */
    @Column(nullable = false, unique = true)
    private String numeroInstalacao;

    /** Endereço preenchido via ViaCEP. O UF é validado para rejeitar SP, RS e PR. */
    @Embedded
    private Address endereco;

    /** Cliente proprietário desta unidade consumidora. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
