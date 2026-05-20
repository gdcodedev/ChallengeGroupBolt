package com.bolt.customerapi.repository;

import com.bolt.customerapi.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para a entidade Customer.
 * Os métodos derivados (findBy*, existsBy*) são interpretados automaticamente pelo Spring Data.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /** Verifica se já existe um cliente com o documento informado (garante unicidade no cadastro). */
    boolean existsByDocumento(String documento);

    /** Verifica duplicidade de documento ignorando o próprio cliente — usado na atualização. */
    boolean existsByDocumentoAndIdNot(String documento, Long id);

    /** Verifica se o número de instalação já pertence a algum cliente (qualquer cliente). */
    boolean existsByConsumerUnitsNumeroInstalacao(String numeroInstalacao);

    /**
     * Verifica se o número de instalação pertence a um cliente diferente do informado.
     * Usado na atualização para permitir manter o mesmo número sem conflito.
     * Retorna contagem — COUNT(c) > 0 não é JPQL válido no Hibernate 6.
     */
    @Query("SELECT COUNT(c) FROM Customer c JOIN c.consumerUnits cu " +
           "WHERE cu.numeroInstalacao = :num AND c.id != :customerId")
    long countByNumeroInstalacaoAndCustomerIdNot(@Param("num") String num,
                                                  @Param("customerId") Long customerId);

    /** Retorna todos os clientes ativos (remoção lógica: ativo = true). */
    List<Customer> findByAtivoTrue();

    /** Retorna os últimos 20 clientes ativos em ordem decrescente de criação. */
    List<Customer> findTop20ByAtivoTrueOrderByCreatedAtDesc();

    /** Busca um cliente ativo pelo ID — retorna vazio se não existir ou estiver inativo. */
    Optional<Customer> findByIdAndAtivoTrue(Long id);
}
