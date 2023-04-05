package fi.vm.sade.saml.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class UnregisteredUserException extends AuthenticationServiceException {
    private final String idpType;

    public UnregisteredUserException(String msg, String idpType) {
        super(msg);
        this.idpType = idpType;
    }

    public String getIdpType() {
        return idpType;
    }
}
