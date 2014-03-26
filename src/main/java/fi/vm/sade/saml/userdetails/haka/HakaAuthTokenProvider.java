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
    private final int USERNAME_1ST_PART = 5;
    private final int USERNAME_2ND_PART = 3;
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
        /* This username generator uses 5 first characters from lastname,
         * followed by 3 first characters from firstname plus additional
         * random number to prevent duplicates
         */
        int endIndex1st = USERNAME_1ST_PART;
        int endIndex2nd = USERNAME_2ND_PART;
        if (sukunimi.length() < USERNAME_1ST_PART) {
            endIndex1st = sukunimi.length() - 1;
        }
        if (nimi.length() < USERNAME_2ND_PART) {
            endIndex2nd = nimi.length() - 1;
        }
        Random intGen = new Random();
        int randomInt = intGen.nextInt(900) + 100; // 100-999
        // Generated username should be e.g "lastnfir123"
        String username = sukunimi.substring(0, endIndex1st) + nimi.substring(0, endIndex2nd) + randomInt;
        
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
