/**
 * 
 */
package fi.vm.sade.saml.userdetails.haka;

import fi.vm.sade.authentication.service.types.AddHenkiloDataType;
import fi.vm.sade.authentication.service.types.AddHenkiloToOrganisaatiosDataType;
import fi.vm.sade.authentication.service.types.dto.HenkiloType;
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
        eduPersonPrincipalName = eduPersonPrincipalName.replace('@', '-');
        return eduPersonPrincipalName;
    }

    @Override
    protected IdentityData createIdentity(SAMLCredential credential) {
        IdentityData henkilo = new IdentityData();

        String nimi = getFirstAttributeValue(credential, "givenName");

        if(nimi == null || "".equals(nimi)) {
            nimi = getFirstAttributeValue(credential, "displayName");
        }

        henkilo.setEtunimet(nimi);
        henkilo.setSukunimi(getFirstAttributeValue(credential, "sn"));
        henkilo.setKutsumanimi(nimi);
        henkilo.setKayttajatunnus(getUniqueIdentifier(credential));

        henkilo.setDomainNimi(getFirstAttributeValue(credential, "schacHomeOrganization"));
        henkilo.setHenkiloTyyppi(HenkiloTyyppiType.VIRKAILIJA);

        logger.info("Creating henkilo data: {}", henkilo);

        return henkilo;
    }

    @Override
    protected AddHenkiloToOrganisaatiosDataType fillExtraPersonData(SAMLCredential credential, AddHenkiloToOrganisaatiosDataType henkiloData) {
        // Fill extra fields
        return henkiloData;
    }


    // TODO: This is just temp solution for release 8.0 (december 2013)
    // Prevents from new users from registering through HAKA
    @Override
    public String createAuthenticationToken(SAMLCredential credential) {
        HenkiloType henkilo = getServiceProviderService().getHenkiloByIDPAndIdentifier(getIDPUniqueKey(),
                getUniqueIdentifier(credential));
        if (henkilo == null) {
            String eppn = getFirstAttributeValue(credential, "eduPersonPrincipalName");
            logger.info("Authentication denied for an unregistered Haka user: {}", eppn);
            return null;
        }
        return super.createAuthenticationToken(credential);
    }
}
