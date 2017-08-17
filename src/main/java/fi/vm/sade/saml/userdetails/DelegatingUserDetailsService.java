/**
 * 
 */
package fi.vm.sade.saml.userdetails;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import fi.vm.sade.saml.exception.SAMLCredentialsParseException;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.schema.XSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

import static fi.vm.sade.saml.userdetails.haka.HakaAuthTokenProvider.E_PNN;

/**
 * @author tommiha
 *
 */
public class DelegatingUserDetailsService implements SAMLUserDetailsService {

    private final static Logger logger = LoggerFactory.getLogger(DelegatingUserDetailsService.class);
    
    /* (non-Javadoc)
     * @see org.springframework.security.saml.userdetails.SAMLUserDetailsService#loadUserBySAML(org.springframework.security.saml.SAMLCredential)
     */
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        UserDetailsDto userDetailsDto = new UserDetailsDto();
        userDetailsDto.setHenkiloCreateDto(createIdentity(credential));
        userDetailsDto.setKayttajatiedotCreateDto(createKayttajatiedot(credential));
        userDetailsDto.setIdentifier(getUniqueIdentifier(credential));

        return userDetailsDto;
    }

    private static HenkiloCreateDto createIdentity(SAMLCredential credential) {
        HenkiloCreateDto henkilo = new HenkiloCreateDto();

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
        henkilo.setHenkiloTyyppi("VIRKAILIJA");

        logger.info("Creating henkilo data: {}", henkilo);

        return henkilo;
    }

    public static String getFirstAttributeValue(SAMLCredential credential, String attributeName) {
        Attribute attrib = null;
        for (Attribute attr : credential.getAttributes()) {
            if (attr.getName() != null && attr.getName().equalsIgnoreCase(attributeName)) {
                attrib = attr;
                break;
            }
        }

        if (attrib == null) {
            logger.debug("Could not find attribute {}", attributeName);
            return null;
        }

        XMLObject obj = attrib.getAttributeValues().get(0);
        if (obj instanceof XSString) {
            return ((XSString) obj).getValue();
        }
        if (obj instanceof XSAny) {
            return ((XSAny) obj).getTextContent();
        }

        logger.error("Could not parse field {} of type {}.", obj.getElementQName(), obj.getSchemaType());
        throw new SAMLCredentialsParseException("Could not parse field " + obj.getElementQName() + " of type "+ obj.getSchemaType());
    }

    private static KayttajatiedotCreateDto createKayttajatiedot(SAMLCredential credential) {
        Random intGen = new Random();
        int randomInt = intGen.nextInt(900) + 100; // 100-999
        // Generated username should be ePPN without special characters + 3 random numbers
        String ePPN = getUniqueIdentifier(credential);
        StringBuilder strBuffer = new StringBuilder();
        for (char c : ePPN.toCharArray()) {
            // [0-9A-Za-z] are currently only allowed
            if (c < 48 || (c > 57 && c < 65) || (c > 90 && c < 97) || c > 122) {
                c = '-';
            }
            strBuffer.append(c);
        }
        String username = strBuffer.toString() + randomInt;
        KayttajatiedotCreateDto kt = new KayttajatiedotCreateDto();
        kt.setUsername(username);

        logger.info("Creating kayttajatiedot data: {}", kt);

        return kt;
    }

    private static String getUniqueIdentifier(SAMLCredential credential) {
        String firstAttrValue = getFirstAttributeValue(credential, E_PNN);
        if(firstAttrValue == null) {
            List<String> attrNames = Collections.emptyList();
            for(Attribute attr : credential.getAttributes()) {
                attrNames.add(attr.getFriendlyName());
            }
            String attrsString = StringUtils.join(attrNames, ",");
            logger.warn("Could not find matching attribute for name {}, \nall attributes [{}]", E_PNN, attrsString);
        }
        return firstAttrValue;
    }

}
