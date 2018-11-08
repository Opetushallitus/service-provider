package fi.vm.sade.saml.userdetails;

import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.saml.clients.KayttooikeusRestClient;
import fi.vm.sade.saml.exception.EmailVerificationException;
import fi.vm.sade.saml.exception.NoStrongIdentificationException;
import fi.vm.sade.saml.exception.UnregisteredUserException;
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

    private boolean emailVerificationEnabled;
    private String hakaEmailVerificationListAsString;
    private List<String> hakaEmailVerificationList;

    public static final String STRONG_IDENTIFICATION = "STRONG_IDENTIFICATION";
    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";

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
                logger.error("Error in REST-client while fetching henkiloOid", e);
                throw e;
            }
        }

        // Where to redirect. null for no redirect
        if( userMayBeRedirectedToStrongIdentification(henkiloOid) || userMayBeRedirectedToEmailVerification(henkiloOid) ) {
            String redirectCode;
            try {
                String loginRedirectUrl = this.ophProperties.url("kayttooikeus-service.cas.login.redirect.oidHenkilo", henkiloOid);
                redirectCode = this.kayttooikeusRestClient.get(loginRedirectUrl, String.class);
            } catch (Exception e) {
                if (isNotFound(e)) {
                    throw new UnregisteredUserException("Authentication denied for an unregistered user: " + userDetailsDto.getIdentifier());
                } else {
                    logger.error("Error in REST-client while fetching loginRedirectType", e);
                    throw e;
                }
            }

            if(STRONG_IDENTIFICATION.equals(redirectCode)) {
                throw new NoStrongIdentificationException(henkiloOid);
            } else if(EMAIL_VERIFICATION.equals(redirectCode)) {
                throw new EmailVerificationException(henkiloOid);
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

    private boolean userMayBeRedirectedToStrongIdentification(String henkiloOid) {
        return this.requireStrongIdentification || this.hakaRequireStrongIdentificationList.contains(henkiloOid);
    }

    private boolean userMayBeRedirectedToEmailVerification(String henkiloOid) {
        return this.emailVerificationEnabled || this.hakaEmailVerificationList.contains(henkiloOid);
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

    public boolean isEmailVerificationEnabled() {
        return this.emailVerificationEnabled;
    }

    public void setEmailVerificationEnabled(boolean emailVerificationEnabled) {
        this.emailVerificationEnabled = emailVerificationEnabled;
    }

    public String getHakaEmailVerificationListAsString() {
        return this.hakaEmailVerificationListAsString;
    }

    public void setHakaEmailVerificationListAsString(String hakaEmailVerificationListAsString) {
        this.hakaEmailVerificationListAsString = hakaEmailVerificationListAsString;
        this.hakaEmailVerificationList = !"".equals(hakaEmailVerificationListAsString)
                ? Arrays.asList(hakaEmailVerificationListAsString.split(","))
                : new ArrayList<String>();
    }
}
