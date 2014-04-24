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
        // urn:oid:1.3.6.1.4.1.5923.1.1.1.6 = ePPN
        return getFirstAttributeValue(credential, "urn:oid:1.3.6.1.4.1.5923.1.1.1.6");
    }

    @Override
    protected IdentityData createIdentity(SAMLCredential credential) {
        IdentityData henkilo = new IdentityData();

        // urn:oid:2.5.4.42 = givenName
        String nimi = getFirstAttributeValue(credential, "urn:oid:2.5.4.42");
        // urn:oid:2.5.4.4 = sn
        String sukunimi = getFirstAttributeValue(credential, "urn:oid:2.5.4.4");

        if (nimi == null || "".equals(nimi)) {
            // urn:oid:2.16.840.1.113730.3.1.241 = displayName
            nimi = getFirstAttributeValue(credential, "urn:oid:2.16.840.1.113730.3.1.241");
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

        // urn:oid:1.3.6.1.4.1.25178.1.2.9 = schacHomeOrganization
        henkilo.setDomainNimi(getFirstAttributeValue(credential, "urn:oid:1.3.6.1.4.1.25178.1.2.9"));
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
            // urn:oid:1.3.6.1.4.1.5923.1.1.1.6 = ePPN
            String eppn = getFirstAttributeValue(credential, "urn:oid:1.3.6.1.4.1.5923.1.1.1.6");
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
