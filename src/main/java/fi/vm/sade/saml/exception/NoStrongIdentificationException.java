package fi.vm.sade.saml.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class NoStrongIdentificationException extends AuthenticationServiceException {

    public NoStrongIdentificationException(String msg, Throwable t) {
        super(msg, t);
    }

    public NoStrongIdentificationException(String msg) {
        super(msg);
    }
}
