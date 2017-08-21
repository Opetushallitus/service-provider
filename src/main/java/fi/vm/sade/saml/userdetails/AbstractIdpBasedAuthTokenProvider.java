package fi.vm.sade.saml.userdetails;

import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.saml.clients.KayttooikeusRestClient;
import fi.vm.sade.saml.clients.OppijanumeroRekisteriRestClient;
import fi.vm.sade.saml.exception.NoStrongIdentificationException;
import org.apache.commons.lang.BooleanUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.saml.SAMLCredential;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

public abstract class AbstractIdpBasedAuthTokenProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private OphProperties ophProperties;
    private OppijanumeroRekisteriRestClient oppijanumerorekisteriRestClient;
    private KayttooikeusRestClient kayttooikeusRestClient;

    private boolean requireStrongIdentification;

    public String createAuthenticationToken(SAMLCredential credential, UserDetailsDto userDetailsDto) throws Exception {
        // Checks if Henkilo with given IdP key and identifier exists
        String henkiloOid;
        try {
            henkiloOid = kayttooikeusRestClient.get(ophProperties.url("kayttooikeus-service.cas.oidByIdp",
                    getIDPUniqueKey(), userDetailsDto.getIdentifier()), String.class);
        }
        catch (Exception e) {
            // If user is not found, then one is created during login
            if (isNotFound(e)) {
                // Implementation may prevent new users from registering
                validateRegistration(credential);
                henkiloOid = createHenkilo(userDetailsDto.getHenkiloCreateDto());
                createKayttajatiedot(henkiloOid, userDetailsDto.getKayttajatiedotCreateDto());
            } else {
                logger.error("Error in REST-client", e);
                throw e;
            }
        }

        if(this.requireStrongIdentification) {
            String vahvaTunnistusUrl = this.ophProperties.url("kayttooikeus-service.cas.vahva-tunnistus", henkiloOid);
            Boolean vahvastiTunnistettu = this.kayttooikeusRestClient.get(vahvaTunnistusUrl, Boolean.class);
            if (BooleanUtils.isFalse(vahvastiTunnistettu)) {
                throw new NoStrongIdentificationException(henkiloOid);
            }
        }

        // Generates and returns auth token to Henkilo by OID
        return kayttooikeusRestClient.get(ophProperties.url("kayttooikeus-service.cas.authTokenForOidAndIdp",
                henkiloOid, getIDPUniqueKey(), userDetailsDto.getIdentifier()), String.class);
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
     * Returns IDP unique key.
     * 
     * @return
     */
    protected abstract String getIDPUniqueKey();


    public OphProperties getOphProperties() {
        return ophProperties;
    }

    public void setOphProperties(OphProperties ophProperties) {
        this.ophProperties = ophProperties;
    }

    public void setKayttooikeusRestClient(KayttooikeusRestClient kayttooikeusRestClient) {
        this.kayttooikeusRestClient = kayttooikeusRestClient;
    }

    public KayttooikeusRestClient getKayttooikeusRestClient() {
        return kayttooikeusRestClient;
    }

    public void setOppijanumerorekisteriRestClient(OppijanumeroRekisteriRestClient oppijanumerorekisteriRestClient) {
        this.oppijanumerorekisteriRestClient = oppijanumerorekisteriRestClient;
    }

    public OppijanumeroRekisteriRestClient getOppijanumerorekisteriRestClient() {
        return oppijanumerorekisteriRestClient;
    }

    public boolean isRequireStrongIdentification() {
        return requireStrongIdentification;
    }

    public void setRequireStrongIdentification(boolean requireStrongIdentification) {
        this.requireStrongIdentification = requireStrongIdentification;
    }
}
