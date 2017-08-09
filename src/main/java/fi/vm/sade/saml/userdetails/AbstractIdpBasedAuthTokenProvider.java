package fi.vm.sade.saml.userdetails;


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

import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.properties.OphProperties;
import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.springframework.http.HttpStatus;

/**
 * @author tommiha
 * 
 */
public abstract class AbstractIdpBasedAuthTokenProvider implements IdpBasedAuthTokenProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String henkiloUsername;
    private String henkiloPassword;
    private OphProperties ophProperties;
    private final CachingRestClient oppijanumerorekisteriRestClient = new CachingRestClient();
    private final CachingRestClient kayttooikeusRestClient = new CachingRestClient();

    @PostConstruct
    public void init() {
        String clientSubSystemCode = getClientSubSystemCode();

        oppijanumerorekisteriRestClient.setClientSubSystemCode(clientSubSystemCode);
        oppijanumerorekisteriRestClient.setWebCasUrl(ophProperties.url("cas.base"));
        oppijanumerorekisteriRestClient.setUsername(henkiloUsername);
        oppijanumerorekisteriRestClient.setPassword(henkiloPassword);
        oppijanumerorekisteriRestClient.setCasService(ophProperties.url("oppijanumerorekisteri-service.security_check"));

        kayttooikeusRestClient.setClientSubSystemCode(clientSubSystemCode);
        kayttooikeusRestClient.setWebCasUrl(ophProperties.url("cas.base"));
        kayttooikeusRestClient.setUsername(henkiloUsername);
        kayttooikeusRestClient.setPassword(henkiloPassword);
        kayttooikeusRestClient.setCasService(ophProperties.url("kayttooikeus-service.security_check"));
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
        // Checks if Henkilo with given IdP key and identifier exists
        String henkiloOid = "";
        try {
            henkiloOid = kayttooikeusRestClient.get(ophProperties.url("kayttooikeus-service.cas.oidByIdp", getIDPUniqueKey(), getUniqueIdentifier(credential)), String.class);
        }
        catch (Exception e) {
            // If user is not found, then one is created during login
            if (isNotFound(e)) {
                // Implementation may prevent new users from registering
                validateRegistration(credential);
                henkiloOid = createHenkilo(createIdentity(credential));
                createKayttajatiedot(henkiloOid, createKayttajatiedot(credential));
            } else {
                logger.error("Error in REST-client", e);
                throw e;
            }
        }

        // Generates and returns auth token to Henkilo by OID
        return kayttooikeusRestClient.get(ophProperties.url("kayttooikeus-service.cas.authTokenForOidAndIdp", henkiloOid, getIDPUniqueKey(), getUniqueIdentifier(credential)), String.class);
    }

    private static boolean isNotFound(Exception exception) {
        if (exception instanceof CachingRestClient.HttpException) {
            CachingRestClient.HttpException httpException = (CachingRestClient.HttpException) exception;
            return httpException.getStatusCode() == HttpStatus.NOT_FOUND.value();
        }
        return false;
    }

    private String createHenkilo(HenkiloCreateDto henkilo) throws IOException {
        String json = toJson(henkilo, HenkiloCreateDto.class);
        String url = ophProperties.url("oppijanumerorekisteri.henkilo");
        HttpResponse response = oppijanumerorekisteriRestClient.post(url, MediaType.APPLICATION_JSON, json);
        int statusCode = response.getStatusLine().getStatusCode();
        if (!isSuccessful(statusCode)) {
            logger.error("Error in creating new henkilo, status: {}", statusCode);
            throw new RuntimeException("Creating henkilo '" + henkilo.getSukunimi() + "' failed.");
        }
        return EntityUtils.toString(response.getEntity());
    }

    private void createKayttajatiedot(String oid, KayttajatiedotCreateDto kayttajatiedot) throws IOException {
        String json = toJson(kayttajatiedot, KayttajatiedotCreateDto.class);
        String url = ophProperties.url("kayttooikeus-service.henkilo.kayttajatiedot", oid);
        HttpResponse response = kayttooikeusRestClient.post(url, MediaType.APPLICATION_JSON, json);
        int statusCode = response.getStatusLine().getStatusCode();
        if (!isSuccessful(statusCode)) {
            logger.error("Error in creating new kayttajatiedot, status: {}", statusCode);
            throw new RuntimeException("Creating kayttajatiedot '" + kayttajatiedot.getUsername() + "' failed.");
        }
    }

    private static <T> String toJson(T value, Class<T> type) {
        ObjectMapper mapper = new ObjectMapperProvider().getContext(type);
        try {
            mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
            return mapper.writeValueAsString(value);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isSuccessful(int statusCode) {
        return HttpStatus.Series.SUCCESSFUL.equals(HttpStatus.valueOf(statusCode).series());
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
     * Code to be used with {@link CachingRestClient#clientSubSystemCode}.
     *
     * @return
     */
    protected abstract String getClientSubSystemCode();

    /**
     * Implementation may prevent new users from registering.
     *
     * @param credential
     */
    protected abstract void validateRegistration(SAMLCredential credential);

    /**
     * Creates Henkilo from SAMLCredentials.
     * 
     * @param credential
     * @return
     */
    protected abstract HenkiloCreateDto createIdentity(SAMLCredential credential);

    /**
     * Creates Kayttajatiedot from SAMLCredentials.
     *
     * @param credential
     * @return
     */
    protected abstract KayttajatiedotCreateDto createKayttajatiedot(SAMLCredential credential);

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
}
