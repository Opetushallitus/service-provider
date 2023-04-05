package fi.vm.sade.saml.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

/**
 * Because this extends AuthenticationException this will be caught on SAMLProcessingFilter and is finally
 * passed to HakaAuthenticationFailureHandler when used in DelegatingUserDetailsService which is injected to
 * authentication manager
 */
public class RequiredSamlAttributeNotProvidedException extends AuthenticationServiceException {
    private final String idpType;

    public RequiredSamlAttributeNotProvidedException(String msg, String idpType) {
        super(msg);
        this.idpType = idpType;
    }

    public String getIdpType() {
        return idpType;
    }
}
