/**
 * 
 */
package fi.vm.sade.saml.userdetails.haka;

import fi.vm.sade.saml.exception.UnregisteredHakaUserException;
import fi.vm.sade.saml.userdetails.AbstractIdpBasedAuthTokenProvider;
import org.springframework.security.saml.SAMLCredential;

import fi.vm.sade.saml.userdetails.DelegatingUserDetailsService;

public class HakaAuthTokenProvider extends AbstractIdpBasedAuthTokenProvider {

    public static final String HAKA_IDP_ID = "haka";
    private boolean registrationEnabled;
    public static final String E_PNN = "urn:oid:1.3.6.1.4.1.5923.1.1.1.6";
    public static final String CLIENT_SUB_SYSTEM_CODE = "authentication.haka";

    @Override
    protected String getIDPUniqueKey() {
        return HAKA_IDP_ID;
    }

    @Override
    protected String getClientSubSystemCode() {
        return CLIENT_SUB_SYSTEM_CODE;
    }

    @Override
    protected void validateRegistration(SAMLCredential credential) {
        if (!isRegistrationEnabled()) {
            String eppn = DelegatingUserDetailsService.getFirstAttributeValue(credential, E_PNN);
            logger.info("Authentication denied for an unregistered Haka user: {}", eppn);
            throw new UnregisteredHakaUserException("Authentication denied for an unregistered Haka user: " + eppn);
        }
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }
}
