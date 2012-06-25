/**
 * 
 */
package fi.vm.sade.saml.userdetails.haka;

import org.springframework.security.saml.SAMLCredential;

import fi.vm.sade.saml.userdetails.AbstractIdpBasedAuthTokenProvider;
import fi.vm.sade.saml.userdetails.model.IdentityData;

/**
 * @author tommiha
 * 
 */
public class HakaAuthTokenProvider extends AbstractIdpBasedAuthTokenProvider {

    public static final String HAKA = "haka";

    @Override
    protected String getIDPUniqueKey() {
        return HAKA;
    }

    @Override
    protected String getUniqueIdentifier(SAMLCredential credential) {
        return getFirstAttributeValue(credential, "eduPersonPrincipalName");
    }

    @Override
    protected IdentityData createIdentity(SAMLCredential credential) {
        IdentityData henkilo = new IdentityData();

        henkilo.setEtunimet(getFirstAttributeValue(credential, "givenName"));
        henkilo.setSukunimi(getFirstAttributeValue(credential, "sn"));
        henkilo.setKutsumanimi(getFirstAttributeValue(credential, "givenName"));
        henkilo.setKayttajatunnus(getFirstAttributeValue(credential, "eduPersonPrincipalName"));

        henkilo.setDomainNimi(getFirstAttributeValue(credential, "schacHomeOrganization"));

        return henkilo;
    }
}
