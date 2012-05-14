/**
 * 
 */
package fi.vm.sade.saml.userdetails.haka;

import org.springframework.security.saml.SAMLCredential;

import fi.vm.sade.auth.model.dto.HenkiloDTO;
import fi.vm.sade.saml.userdetails.AbstractIdpBasedAuthTokenProvider;

/**
 * @author tommiha
 *
 */
public class HakaAuthTokenProvider extends AbstractIdpBasedAuthTokenProvider {

    public static final String HAKA = "haka";
    
    /*
     * (non-Javadoc)
     * @see fi.vm.sade.saml.userdetails.IdpBasedAuthTokenProvider#createAuthenticationToken(org.springframework.security.saml.SAMLCredential)
     */
    @Override
    public String createAuthenticationToken(SAMLCredential credential) {
        HenkiloDTO henkilo = null;
        String tunnistuksenTunniste = getIDPUniqueKey() + ":" + getFirstAttributeValue(credential, "eduPersonPrincipalName");
        if(getUserManagementService().henkiloExists(tunnistuksenTunniste)) {
            henkilo = getUserManagementService().getHenkiloByTunniste(tunnistuksenTunniste);
        } else {
            henkilo = new HenkiloDTO();
            henkilo.setEtunimet(getFirstAttributeValue(credential, "givenName"));
            henkilo.setSukunimi(getFirstAttributeValue(credential, "sn"));
            // TODO: Create OID
            henkilo.setOidHenkilo(getFirstAttributeValue(credential, "eduPersonPrincipalName"));
            henkilo.setTunnistuksenTunniste(tunnistuksenTunniste);
            henkilo.setHetu(getFirstAttributeValue(credential, "eduPersonPrincipalName"));
            henkilo = getUserManagementService().addHenkilo(henkilo);
        }
        return getAuthenticationService().login(henkilo);
    }

    @Override
    protected String getIDPUniqueKey() {
        return HAKA;
    }

}
