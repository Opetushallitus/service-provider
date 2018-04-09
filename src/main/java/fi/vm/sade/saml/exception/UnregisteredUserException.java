package fi.vm.sade.saml.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class UnregisteredUserException extends AuthenticationServiceException {

    public UnregisteredUserException(String msg, Throwable t) {
        super(msg, t);
    }

    public UnregisteredUserException(String msg) {
        super(msg);
    }
}
