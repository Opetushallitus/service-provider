package fi.vm.sade.saml.userdetails;

import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.saml.clients.KayttooikeusRestClient;
import fi.vm.sade.saml.exception.NoStrongIdentificationException;
import fi.vm.sade.saml.exception.UnregisteredUserException;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.saml.SAMLCredential;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractIdpBasedAuthTokenProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private OphProperties ophProperties;
    private KayttooikeusRestClient kayttooikeusRestClient;

    private boolean requireStrongIdentification;
    private String hakaRequireStrongIdentificationListAsString;

    private List<String> hakaRequireStrongIdentificationList;

    public String createAuthenticationToken(SAMLCredential credential, UserDetailsDto userDetailsDto) throws Exception {
        // Checks if Henkilo with given IdP key and identifier exists
        String henkiloOid;
        try {
            String url = ophProperties.url("kayttooikeus-service.cas.oidByIdp", getIDPUniqueKey(), userDetailsDto.getIdentifier());
            henkiloOid = kayttooikeusRestClient.get(url, String.class);
        }
        catch (Exception e) {
            if (isNotFound(e)) {
                throw new UnregisteredUserException("Authentication denied for an unregistered user: " + userDetailsDto.getIdentifier());
            } else {
                logger.error("Error in REST-client", e);
                throw e;
            }
        }

        if (this.requireStrongIdentification
                || this.hakaRequireStrongIdentificationList.contains(henkiloOid)) {
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

    public boolean isRequireStrongIdentification() {
        return requireStrongIdentification;
    }

    public void setRequireStrongIdentification(boolean requireStrongIdentification) {
        this.requireStrongIdentification = requireStrongIdentification;
    }

    public String getHakaRequireStrongIdentificationListAsString() {
        return hakaRequireStrongIdentificationListAsString;
    }

    public void setHakaRequireStrongIdentificationListAsString(String hakaRequireStrongIdentificationListAsString) {
        this.hakaRequireStrongIdentificationListAsString = hakaRequireStrongIdentificationListAsString;
        this.hakaRequireStrongIdentificationList = !"".equals(hakaRequireStrongIdentificationListAsString)
                ? Arrays.asList(hakaRequireStrongIdentificationListAsString.split(","))
                : new ArrayList<String>();
    }
}
