/**
 * 
 */
package fi.vm.sade.saml.userdetails.haka;

import java.util.Random;

import org.springframework.security.saml.SAMLCredential;

import fi.vm.sade.authentication.service.types.AddHenkiloToOrganisaatiosDataType;
import fi.vm.sade.authentication.service.types.dto.HenkiloType;
import fi.vm.sade.authentication.service.types.dto.HenkiloTyyppiType;
import fi.vm.sade.authentication.service.types.dto.KayttajatiedotType;
import fi.vm.sade.saml.exception.UnregisteredHakaUserException;
import fi.vm.sade.saml.userdetails.AbstractIdpBasedAuthTokenProvider;
import fi.vm.sade.saml.userdetails.model.IdentityData;

/**
 * @author tommiha
 * 
 */
public class HakaAuthTokenProvider extends AbstractIdpBasedAuthTokenProvider {

    public static final String HAKA_IDP_ID = "haka";
    private boolean registrationEnabled;

    @Override
    protected String getIDPUniqueKey() {
        return HAKA_IDP_ID;
    }

    @Override
    protected String getUniqueIdentifier(SAMLCredential credential) {
        return getFirstAttributeValue(credential, "eduPersonPrincipalName");
    }

    @Override
    protected IdentityData createIdentity(SAMLCredential credential) {
        IdentityData henkilo = new IdentityData();

        String nimi = getFirstAttributeValue(credential, "givenName");
        String sukunimi = getFirstAttributeValue(credential, "sn");

        if (nimi == null || "".equals(nimi)) {
            nimi = getFirstAttributeValue(credential, "displayName");
        }

        henkilo.setEtunimet(nimi);
        henkilo.setSukunimi(sukunimi);
        henkilo.setKutsumanimi(nimi);
        
        KayttajatiedotType kt = new KayttajatiedotType();
        
        Random intGen = new Random();
        int randomInt = intGen.nextInt(900) + 100; // 100-999
        // Generated username should be ePPN without special characters + 3 random numbers
        String ePPN = getUniqueIdentifier(credential);
        StringBuffer strBuffer = new StringBuffer();
        for (char c : ePPN.toCharArray()) {
            // [0-9A-Za-z] are currently only allowed
            if (c < 48 || (c > 57 && c < 65) || (c > 90 && c < 97) || c > 122) {
                c = '-';
            }
            strBuffer.append(c);
        }
        String username = strBuffer.toString() + randomInt;
        
        kt.setUsername(username);
        henkilo.setKayttajatiedot(kt);

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



    @Override
    public String createAuthenticationToken(SAMLCredential credential) {
        HenkiloType henkilo = getServiceProviderService().getHenkiloByIDPAndIdentifier(getIDPUniqueKey(),
                getUniqueIdentifier(credential));
        // Prevents from new users from registering through Haka
        if (henkilo == null && !isRegistrationEnabled()) {
            String eppn = getFirstAttributeValue(credential, "eduPersonPrincipalName");
            logger.info("Authentication denied for an unregistered Haka user: {}", eppn);
            throw new UnregisteredHakaUserException("Authentication denied for an unregistered Haka user: " + eppn);
        }
        return super.createAuthenticationToken(credential);
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }
}
