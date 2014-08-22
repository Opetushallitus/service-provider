/**
 * 
 */
package fi.vm.sade.saml.userdetails.haka;

import java.io.IOException;
import java.util.Random;

import org.springframework.security.saml.SAMLCredential;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.authentication.model.Kayttajatiedot;
import fi.vm.sade.authentication.service.types.AddHenkiloToOrganisaatiosDataType;
import fi.vm.sade.saml.exception.UnregisteredHakaUserException;
import fi.vm.sade.saml.userdetails.AbstractIdpBasedAuthTokenProvider;

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
    protected Henkilo createIdentity(SAMLCredential credential) {
//        IdentityData henkilo = new IdentityData();
        Henkilo henkilo = new Henkilo();

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
        henkilo.setHenkiloTyyppi(HenkiloTyyppi.VIRKAILIJA);
        
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
        Kayttajatiedot kt = new Kayttajatiedot();
        kt.setUsername(username);
        henkilo.setKayttajatiedot(kt);

        logger.info("Creating henkilo data: {}", henkilo);

        return henkilo;
    }

    @Override
    public String createAuthenticationToken(SAMLCredential credential) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(getHenkiloRestUrl());
        sb.append("cas/auth/idp/");
        sb.append(getIDPUniqueKey());
        sb.append("?idpid=");
        sb.append(getUniqueIdentifier(credential));
        
        String henkiloOid = null;
        try {
            henkiloOid = getHenkiloRestClient().get(sb.toString(), String.class);
        }
        catch (IOException ioe) {
            logger.error("REST client", ioe);
        }
        // Prevents from new users from registering through Haka
        if (henkiloOid == null && !isRegistrationEnabled()) {
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
