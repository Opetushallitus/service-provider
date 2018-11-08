package fi.vm.sade.saml.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class EmailVerificationException extends AuthenticationServiceException {

    public EmailVerificationException(String msg, Throwable t) {
        super(msg, t);
    }

    public EmailVerificationException(String msg) {
        super(msg);
    }

}