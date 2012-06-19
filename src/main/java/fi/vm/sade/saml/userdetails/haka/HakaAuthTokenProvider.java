/**
 * 
 */
package fi.vm.sade.saml.userdetails.haka;

import org.springframework.security.saml.SAMLCredential;

import fi.vm.sade.authentication.service.types.AddHenkiloData;
import fi.vm.sade.saml.userdetails.AbstractIdpBasedAuthTokenProvider;

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
    protected AddHenkiloData createIdentity(SAMLCredential credential) {
        AddHenkiloData henkilo = new AddHenkiloData();

        henkilo.setEtunimet(getFirstAttributeValue(credential, "givenName"));
        henkilo.setSukunimi(getFirstAttributeValue(credential, "sn"));
        henkilo.setKutsumanimi(getFirstAttributeValue(credential, "givenName"));
        henkilo.setKayttajatunnus(getFirstAttributeValue(credential, "eduPersonPrincipalName"));

        henkilo.setDomainNimi(getFirstAttributeValue(credential, "schacHomeOrganization"));

        // TODO: remove this
        henkilo.setDomainNimi("www.esa.fi");
        return henkilo;
    }
}
