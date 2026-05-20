package com.bolt.customerapi;

import com.bolt.customerapi.event.ClienteAnaliseMGEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;

/**
 * Ponto de entrada da aplicação Customer API.
 *
 * <p>Também atua como listener do evento {@link ClienteAnaliseMGEvent},
 * simulando a publicação no tópico {@code analise_cliente_mg} via log.
 * A implementação real do consumer não é requisito do desafio.</p>
 */
@SpringBootApplication
public class CustomerApiApplication {

    private static final Logger log = LoggerFactory.getLogger(CustomerApiApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CustomerApiApplication.class, args);
    }

    /**
     * Recebe o evento de cliente MG após o cadastro bem-sucedido.
     * Em produção, este método publicaria a mensagem em um broker (Kafka/RabbitMQ).
     */
    @EventListener
    public void onClienteMG(ClienteAnaliseMGEvent event) {
        log.info("[EVENTO] Cliente MG publicado no tópico analise_cliente_mg — id={}, documento={}",
                event.getCustomerId(), event.getDocumento());
    }
}
