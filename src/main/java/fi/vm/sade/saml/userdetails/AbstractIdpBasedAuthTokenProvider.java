package fi.vm.sade.saml.userdetails;

import java.io.IOException;

import javax.annotation.PostConstruct;

import fi.vm.sade.saml.exception.SAMLCredentialsParseException;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.schema.XSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml.SAMLCredential;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.authentication.model.OrganisaatioHenkilo;
import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.organisaatio.api.search.OrganisaatioHakutulos;
import fi.vm.sade.organisaatio.api.search.OrganisaatioPerustieto;
import fi.vm.sade.properties.OphProperties;

/**
 * @author tommiha
 * 
 */
public abstract class AbstractIdpBasedAuthTokenProvider implements IdpBasedAuthTokenProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String henkiloUsername;
    private String henkiloPassword;
    private OphProperties ophProperties;
    private CachingRestClient henkiloRestClient = new CachingRestClient();
    
    private String organisaatioRestUrl;

    @PostConstruct
    public void init() {
        henkiloRestClient.setWebCasUrl(ophProperties.url("cas.base"));
        henkiloRestClient.setUsername(henkiloUsername);
        henkiloRestClient.setPassword(henkiloPassword);
        henkiloRestClient.setCasService(ophProperties.url("henkilo.security_check"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * fi.vm.sade.saml.userdetails.IdpBasedAuthTokenProvider#providesToken(java
     * .lang.String)
     */
    @Override
    public boolean providesToken(String idp) {
//        if (supportedProviders != null) {
//            return supportedProviders.contains(idp);
//        }
//        return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fi.vm.sade.saml.userdetails.IdpBasedAuthTokenProvider#createAuthenticationToken(org.springframework.security.saml.SAMLCredential)
     */
    @Override
    public String createAuthenticationToken(SAMLCredential credential) throws Exception {
        ObjectMapper mapper = new ObjectMapperProvider().getContext(Henkilo.class);
        
        // Checks if Henkilo with given IdP key and identifier exists
        String henkiloOid = "";
        try {
            henkiloOid = henkiloRestClient.get(ophProperties.url("henkilo.cas.auth.idp", getIDPUniqueKey(), getUniqueIdentifier(credential)), String.class);
        }
        catch (Exception e) {
            logger.error("Error in REST-client", e);
        }
        
        // If user is not found, then one is created during login
        if (henkiloOid.equalsIgnoreCase("none")) {
            Henkilo addHenkilo = createIdentity(credential);
            
            String henkiloJson = "";
            try {
                mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
                henkiloJson = mapper.writeValueAsString(addHenkilo);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            HttpResponse response = henkiloRestClient.post(ophProperties.url("henkilo.cas.auth.henkilo"), "application/json", henkiloJson);
            if (response.getStatusLine().getStatusCode() != 200) {
                logger.error("Error in creating new henkilo, status: " + response.getStatusLine().getStatusCode());
                throw new RuntimeException("Creating henkilo '" + addHenkilo.getKayttajatiedot().getUsername() + "' failed.");
            }
            henkiloOid = EntityUtils.toString(response.getEntity());
            
            addOrganisaatioHenkilos(credential, henkiloOid);
        }
        
        // Generates and returns auth token to Henkilo by OID
        return henkiloRestClient.get(ophProperties.url("henkilo.cas.auth.oid", henkiloOid, getIDPUniqueKey(), getUniqueIdentifier(credential)), String.class);
    }

    private void addOrganisaatioHenkilos(SAMLCredential credential, String henkiloOid) {
        ObjectMapper mapper = new ObjectMapperProvider().getContext(OrganisaatioHenkilo.class);

        // urn:oid:1.3.6.1.4.1.25178.1.2.9 = schacHomeOrganization e.g. domain name: tut.fi
        String domainName = getFirstAttributeValue(credential, "urn:oid:1.3.6.1.4.1.25178.1.2.9");
        // TODO!! Domain nimelle pitää tehdä käsittely tähän!!!
        
        OrganisaatioHakutulos organisaatio = null;
        /* TODO!! Kun domain nimi saadaan asetettua...
        try {
            organisaatio = organisaatioRestClient.get(organisaatioRestUrl, OrganisaatioHakutulos.class);
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        */

        // Ei pitäisi koskaan tulla yli yhtä kappaletta. Jos kumminkin
        // tulee, otetaan ensimmäinen..
        if (organisaatio != null && organisaatio.getNumHits() > 0 &&
                organisaatio.getOrganisaatiot() != null && !organisaatio.getOrganisaatiot().isEmpty()) {
            OrganisaatioHenkilo ohdata = new OrganisaatioHenkilo();

            OrganisaatioPerustieto perusTieto = organisaatio.getOrganisaatiot().get(0);
            ohdata.setOrganisaatioOid(perusTieto.getOid());
            
            String orgHenkiloJson = "";
            try {
                mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
                orgHenkiloJson = mapper.writeValueAsString(ohdata);
                
                HttpResponse response = henkiloRestClient.post(ophProperties.url("henkilo.cas.auth.orghenkilo", henkiloOid), "application/json", orgHenkiloJson);
                if (response.getStatusLine().getStatusCode() != 200) {
                    logger.warn("Creating org.henkilo failed.");
                }
            }
            catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    protected String getFirstAttributeValue(SAMLCredential credential, String attributeName) {
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

    /**
     * Creates Henkilo from SAMLCredentials.
     * 
     * @param credential
     * @return
     */
    protected abstract Henkilo createIdentity(SAMLCredential credential);

    /**
     * Returns IDP unique key.
     * 
     * @return
     */
    protected abstract String getIDPUniqueKey();

    /**
     * 
     * @param credential
     * @return
     */
    protected abstract String getUniqueIdentifier(SAMLCredential credential);

    public String getHenkiloUsername() {
        return henkiloUsername;
    }

    public void setHenkiloUsername(String henkiloUsername) {
        this.henkiloUsername = henkiloUsername;
    }

    public String getHenkiloPassword() {
        return henkiloPassword;
    }

    public void setHenkiloPassword(String henkiloPassword) {
        this.henkiloPassword = henkiloPassword;
    }

    public OphProperties getOphProperties() {
        return ophProperties;
    }

    public void setOphProperties(OphProperties ophProperties) {
        this.ophProperties = ophProperties;
    }

    public String getOrganisaatioRestUrl() {
        return organisaatioRestUrl;
    }

    public void setOrganisaatioRestUrl(String organisaatioRestUrl) {
        this.organisaatioRestUrl = organisaatioRestUrl;
    }

    public CachingRestClient getHenkiloRestClient() {
        return henkiloRestClient;
    }
}
