package fi.vm.sade.saml.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class SAMLCredentialsParseException extends AuthenticationServiceException {

    public SAMLCredentialsParseException(String msg, Throwable t) {
        super(msg, t);
    }

    public SAMLCredentialsParseException(String msg) {
        super(msg);
    }
}