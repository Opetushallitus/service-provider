package fi.vm.sade.saml.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class UnregisteredHakaUserException extends AuthenticationServiceException {

    public UnregisteredHakaUserException(String msg, Throwable t) {
        super(msg, t);
    }

    public UnregisteredHakaUserException(String msg) {
        super(msg);
    }
}
