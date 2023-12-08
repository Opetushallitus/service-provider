package fi.vm.sade.saml.userdetails;

import fi.vm.sade.saml.clients.KayttooikeusRestClient;
import fi.vm.sade.saml.exception.EmailVerificationException;
import fi.vm.sade.saml.exception.NoStrongIdentificationException;
import fi.vm.sade.saml.exception.PasswordChangeException;
import fi.vm.sade.saml.exception.UnregisteredUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml.SAMLCredential;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractIdpBasedAuthTokenProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private KayttooikeusRestClient kayttooikeusRestClient;

    private boolean requireStrongIdentification;
    private String hakaRequireStrongIdentificationListAsString;
    private List<String> hakaRequireStrongIdentificationList;

    private boolean emailVerificationEnabled;
    private String hakaEmailVerificationListAsString;
    private List<String> hakaEmailVerificationList;

    public static final String STRONG_IDENTIFICATION = "STRONG_IDENTIFICATION";
    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";

    public String createAuthenticationToken(SAMLCredential credential, UserDetailsDto userDetailsDto) throws Exception {
        // Checks if Henkilo with given IdP key and identifier exists
        String henkiloOid = kayttooikeusRestClient.getUserOidByIdentifier(getIDPUniqueKey(), userDetailsDto.getIdentifier())
                .orElseThrow(new Supplier<UnregisteredUserException>() {
                    @Override
                    public UnregisteredUserException get() {
                        return new UnregisteredUserException("Authentication denied for an unregistered user: " + userDetailsDto.getIdentifier(), getIDPUniqueKey());
                    }
                });

        boolean isStrongIdentificationRedirectAllowed = strongIdentificationRedirectAllowed(henkiloOid);
        boolean isEmailVerificationRedirectAllowed = emailVerificationRedirectAllowed(henkiloOid);

        String redirectCode = kayttooikeusRestClient.getRedirectCodeByOid(henkiloOid);
        if(STRONG_IDENTIFICATION.equals(redirectCode) && isStrongIdentificationRedirectAllowed) {
            throw new NoStrongIdentificationException(henkiloOid);
        } else if(EMAIL_VERIFICATION.equals(redirectCode) && isEmailVerificationRedirectAllowed) {
            throw new EmailVerificationException(henkiloOid);
        } else if (PASSWORD_CHANGE.equals(redirectCode)) {
            throw new PasswordChangeException(henkiloOid);
        }

        // Generates and returns auth token to Henkilo by OID
        return kayttooikeusRestClient.createAuthToken(henkiloOid, getIDPUniqueKey(), userDetailsDto.getIdentifier());
    }

    private boolean strongIdentificationRedirectAllowed(String henkiloOid) {
        return this.requireStrongIdentification || this.hakaRequireStrongIdentificationList.contains(henkiloOid);
    }

    private boolean emailVerificationRedirectAllowed(String henkiloOid) {
        return this.emailVerificationEnabled || this.hakaEmailVerificationList.contains(henkiloOid);
    }

    /**
     * Returns IDP unique key.
     * 
     * @return
     */
    protected abstract String getIDPUniqueKey();

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
