package fi.vm.sade.saml.userdetails;

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
import fi.vm.sade.organisaatio.api.model.OrganisaatioService;
import fi.vm.sade.organisaatio.api.model.types.OrganisaatioDTO;
import fi.vm.sade.organisaatio.api.model.types.OrganisaatioSearchCriteriaDTO;
import fi.vm.sade.saml.userdetails.model.IdentityData;

/**
 * @author tommiha
 * 
 */
public abstract class AbstractIdpBasedAuthTokenProvider implements IdpBasedAuthTokenProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private OrganisaatioService organisaatioService;
    
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
        // Checks if Henkilo with given IdP key and identifier exists
        String henkiloOid = henkiloRestClient.get(sb.toString(), String.class);
        // If user is not found, then one is created during login
        if (henkiloOid == null) {
            Henkilo addHenkilo = createIdentity(credential);
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
                throw new RuntimeException("Creating henkilo '" + addHenkilo.getKayttajatiedot().getUsername() + "' failed.");
            }
            henkiloOid = response.getEntity().toString();
            
            addOrganisaatioHenkilos(credential, henkiloOid);
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
        List<AddHenkiloToOrganisaatiosDataType> ohdatas = new ArrayList<AddHenkiloToOrganisaatiosDataType>();

        OrganisaatioSearchCriteriaDTO criteria = new OrganisaatioSearchCriteriaDTO();
        // urn:oid:1.3.6.1.4.1.25178.1.2.9 = schacHomeOrganization
        criteria.setOrganisaatioDomainNimi(getFirstAttributeValue(credential, "urn:oid:1.3.6.1.4.1.25178.1.2.9"));
        List<OrganisaatioDTO> list = organisaatioService.searchOrganisaatios(criteria);

        // Ei pitäisi koskaan tulla yli yhtä kappaletta. Jos kumminkin
        // tulee, otetaan ensimmäinen..
        if (list != null && list.size() > 0) {
            OrganisaatioHenkilo ohdata = new OrganisaatioHenkilo();

            OrganisaatioDTO organisaatioDTO = list.get(0);
            ohdata.setOrganisaatioOid(organisaatioDTO.getOid());
            
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
            catch (Exception e) {
                throw new RuntimeException(e);
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

    /**
     * This method can be used to fill extra data such as work title, phone numbers etc. to henkilo data.
     * @param henkiloData
     * @return
     */
    protected abstract AddHenkiloToOrganisaatiosDataType fillExtraPersonData(SAMLCredential credential, AddHenkiloToOrganisaatiosDataType henkiloData);

//    public List<String> getSupportedProviders() {
//        return supportedProviders;
//    }

    /**
     * Entity IDs (from SAML provider metadata) supported by this token
     * provider.
     * 
     * @param supportedProviders
     */
//    public void setSupportedProviders(List<String> supportedProviders) {
//        this.supportedProviders = supportedProviders;
//    }

    public OrganisaatioService getOrganisaatioService() {
        return organisaatioService;
    }

    public void setOrganisaatioService(OrganisaatioService organisaatioService) {
        this.organisaatioService = organisaatioService;
    }

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
