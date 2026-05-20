package com.bolt.customerapi.event;

import org.springframework.context.ApplicationEvent;

public class ClienteAnaliseMGEvent extends ApplicationEvent {

    private final Long customerId;
    private final String documento;

    public ClienteAnaliseMGEvent(Object source, Long customerId, String documento) {
        super(source);
        this.customerId = customerId;
        this.documento = documento;
    }

    public Long getCustomerId() { return customerId; }
    public String getDocumento() { return documento; }
}
