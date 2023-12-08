package fi.vm.sade.saml.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class PasswordChangeException extends AuthenticationServiceException {

    public PasswordChangeException(String msg, Throwable t) {
        super(msg, t);
    }

    public PasswordChangeException(String msg) {
        super(msg);
    }

}