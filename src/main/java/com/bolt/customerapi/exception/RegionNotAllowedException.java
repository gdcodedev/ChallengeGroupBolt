package com.bolt.customerapi.exception;

public class RegionNotAllowedException extends RuntimeException {
    public RegionNotAllowedException(String uf) {
        super("Não atendemos clientes na região: " + uf);
    }
}
