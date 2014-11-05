package fi.vm.sade.saml.userdetails;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.http.HttpResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml.SAMLCredential;

import fi.vm.sade.authentication.service.types.AddHenkiloDataType;
import fi.vm.sade.authentication.service.types.AddHenkiloToOrganisaatiosDataType;
import fi.vm.sade.authentication.service.types.dto.HenkiloType;
import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.authentication.model.OrganisaatioHenkilo;
import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.organisaatio.api.search.OrganisaatioHakutulos;
import fi.vm.sade.organisaatio.api.search.OrganisaatioPerustieto;
import fi.vm.sade.saml.userdetails.model.IdentityData;

/**
 * @author tommiha
 * 
 */
public abstract class AbstractIdpBasedAuthTokenProvider implements IdpBasedAuthTokenProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String henkiloWebCasUrl;
    private String henkiloUsername;
    private String henkiloPassword;
    private String henkiloCasService;
    private String henkiloRestUrl;
    private CachingRestClient henkiloRestClient = new CachingRestClient();
    
    private String organisaatioRestUrl;
    private CachingRestClient organisaatioRestClient = new CachingRestClient();
    
    @PostConstruct
    public void init() {
        henkiloRestClient.setWebCasUrl(henkiloWebCasUrl);
        henkiloRestClient.setUsername(henkiloUsername);
        henkiloRestClient.setPassword(henkiloPassword);
        henkiloRestClient.setCasService(henkiloCasService + "/j_spring_cas_security_check");
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
        
        StringBuffer sb = new StringBuffer();
        sb.append(henkiloRestUrl);
        sb.append("cas/auth/idp/");
        sb.append(getIDPUniqueKey());
        sb.append("?idpid=");
        sb.append(getUniqueIdentifier(credential));
        
        logger.error("DEBUG::trying to verify ePPN = " + getUniqueIdentifier(credential));
        
        // Checks if Henkilo with given IdP key and identifier exists
        String henkiloOid = null;
        try {
            henkiloOid = henkiloRestClient.get(sb.toString(), String.class);
        }
        catch (Exception e) {
            logger.error("Error in REST-client", e);
        }
        
        logger.error("DEBUG::henkiloOid = " + henkiloOid);
        // If user is not found, then one is created during login
        if (henkiloOid.equals("null")) {
            Henkilo addHenkilo = createIdentity(credential);
            
            logger.error("DEBUG::new henkilo model created");
            sb = null;
            sb = new StringBuffer();
            sb.append(henkiloRestUrl);
            sb.append("cas/auth/henkilo");
            
            String henkiloJson = "";
            try {
                mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
                henkiloJson = mapper.writeValueAsString(addHenkilo);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            HttpResponse response = henkiloRestClient.post(sb.toString(), "application/json", henkiloJson);
            if (response.getStatusLine().getStatusCode() != 200) {
                logger.error("Error in creating new henkilo, status: " + response.getStatusLine().getStatusCode());
                throw new RuntimeException("Creating henkilo '" + addHenkilo.getKayttajatiedot().getUsername() + "' failed.");
            }
            logger.error("DEBUG::new henkilo entity created");
            
            henkiloOid = response.getEntity().toString();
            
            addOrganisaatioHenkilos(credential, henkiloOid);
            
            logger.error("DEBUG::henkilo's organizations handled");
        }
        
        sb = null;
        sb = new StringBuffer();
        sb.append(henkiloRestUrl);
        sb.append("cas/auth/oid/");
        sb.append(henkiloOid);
        sb.append("?idpkey=");
        sb.append(getIDPUniqueKey());
        sb.append("&idpid=");
        sb.append(getUniqueIdentifier(credential));
        // Generates and returns auth token to Henkilo by OID
        return henkiloRestClient.get(sb.toString(), String.class);
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
            
            StringBuffer sb = new StringBuffer();
            sb.append(henkiloRestUrl);
            sb.append("cas/auth/");
            sb.append(henkiloOid);
            sb.append("/orghenkilo");
            
            String orgHenkiloJson = "";
            try {
                mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
                orgHenkiloJson = mapper.writeValueAsString(ohdata);
                
                HttpResponse response = henkiloRestClient.post(sb.toString(), "application/json", orgHenkiloJson);
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
            return null;
        }

        XMLObject obj = attrib.getAttributeValues().get(0);
        if (obj instanceof XSString) {
            return ((XSString) obj).getValue();
        }
        return null;
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

    public String getHenkiloWebCasUrl() {
        return henkiloWebCasUrl;
    }

    public void setHenkiloWebCasUrl(String henkiloWebCasUrl) {
        this.henkiloWebCasUrl = henkiloWebCasUrl;
    }

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

    public String getHenkiloCasService() {
        return henkiloCasService;
    }

    public void setHenkiloCasService(String henkiloCasService) {
        this.henkiloCasService = henkiloCasService;
    }

    public String getOrganisaatioRestUrl() {
        return organisaatioRestUrl;
    }

    public void setOrganisaatioRestUrl(String organisaatioRestUrl) {
        this.organisaatioRestUrl = organisaatioRestUrl;
    }

    public String getHenkiloRestUrl() {
        return henkiloRestUrl;
    }

    public void setHenkiloRestUrl(String henkiloRestUrl) {
        this.henkiloRestUrl = henkiloRestUrl;
    }

    public CachingRestClient getHenkiloRestClient() {
        return henkiloRestClient;
    }
}
