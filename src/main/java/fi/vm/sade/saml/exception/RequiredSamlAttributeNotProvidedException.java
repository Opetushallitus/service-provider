package fi.vm.sade.saml.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class RequiredSamlAttributeNotProvidedException extends AuthenticationServiceException {

    public RequiredSamlAttributeNotProvidedException(String msg, Throwable t) {
        super(msg, t);
    }

    public RequiredSamlAttributeNotProvidedException(String msg) {
        super(msg);
    }
}
