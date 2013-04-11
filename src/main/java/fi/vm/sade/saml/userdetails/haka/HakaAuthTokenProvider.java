/**
 * 
 */
package fi.vm.sade.saml.userdetails.haka;

import fi.vm.sade.authentication.service.types.AddHenkiloToOrganisaatiosDataType;
import fi.vm.sade.authentication.service.types.dto.HenkiloTyyppiType;
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
        String eduPersonPrincipalName = getFirstAttributeValue(credential, "eduPersonPrincipalName");
        eduPersonPrincipalName = eduPersonPrincipalName.replace('@', '_');
        return eduPersonPrincipalName;
    }

    @Override
    protected IdentityData createIdentity(SAMLCredential credential) {
        IdentityData henkilo = new IdentityData();

        henkilo.setEtunimet(getFirstAttributeValue(credential, "givenName"));
        henkilo.setSukunimi(getFirstAttributeValue(credential, "sn"));
        henkilo.setKutsumanimi(getFirstAttributeValue(credential, "givenName"));
        henkilo.setKayttajatunnus(getUniqueIdentifier(credential));

        henkilo.setDomainNimi(getFirstAttributeValue(credential, "schacHomeOrganization"));
        henkilo.setHenkiloTyyppi(HenkiloTyyppiType.VIRKAILIJA);

        return henkilo;
    }

    @Override
    protected AddHenkiloToOrganisaatiosDataType fillExtraPersonData(SAMLCredential credential, AddHenkiloToOrganisaatiosDataType henkiloData) {
        // Fill extra fields
        return henkiloData;
    }
}
