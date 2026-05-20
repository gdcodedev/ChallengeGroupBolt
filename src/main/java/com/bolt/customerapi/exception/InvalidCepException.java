package com.bolt.customerapi.exception;

public class InvalidCepException extends RuntimeException {
    public InvalidCepException(String cep) {
        super("CEP inválido ou não encontrado: " + cep);
    }
}
