package fi.vm.sade.saml.redirect;

import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.saml.clients.OppijanumeroRekisteriRestClient;
import fi.vm.sade.saml.entry.RequestSavingSAMLEntryPoint;
import fi.vm.sade.saml.exception.NoStrongIdentificationException;
import fi.vm.sade.saml.userdetails.UserDetailsDto;
import fi.vm.sade.saml.userdetails.haka.HakaAuthTokenProvider;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tommiha
 *
 */
public class AuthTokenAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String AUTH_TOKEN_PARAMETER = "authToken";

    private final OphProperties ophProperties;
    private CachingRestClient kayttooikeusRestClient;
    private OppijanumeroRekisteriRestClient oppijanumeroRekisteriRestClient;

    private HakaAuthTokenProvider tokenProviders;


    public AuthTokenAuthenticationSuccessHandler(OphProperties ophProperties) {
        this.ophProperties = ophProperties;
    }

    public void initialize() {
        String registerUiUrl = ophProperties.url("registration-ui.register");
        String loginUrl = ophProperties.url("cas.login", registerUiUrl);
        setDefaultTargetUrl(loginUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String targetUrl = getDefaultTargetUrl();
        String finalTargetUrl = (String) request.getSession().getAttribute(RequestSavingSAMLEntryPoint.REDIRECT_KEY);
        final String temporaryToken = (String) request.getSession().getAttribute(RequestSavingSAMLEntryPoint.KUTSU_TEMP_TOKEN_KEY);
        
        logger.info("Target url: " + targetUrl);
        
        if (authentication instanceof AbstractAuthenticationToken) {
            AbstractAuthenticationToken token = (AbstractAuthenticationToken) authentication;
            if (StringUtils.isEmpty(temporaryToken) && token.getDetails() != null && token.getDetails() instanceof UserDetailsDto) {
                String authToken;
                try {
                    authToken = this.tokenProviders
                            .createAuthenticationToken((SAMLCredential) authentication.getCredentials(), (UserDetailsDto) token.getDetails());
                } catch (NoStrongIdentificationException e) {
                    String henkiloOid = e.getMessage();
                    String languageCodeUrl = this.ophProperties.url("oppijanumerorekisteri.henkilo.kieliKoodi", henkiloOid);
                    String languageCode = this.oppijanumeroRekisteriRestClient.get(languageCodeUrl, String.class);
                    String createLoginTokenUrl = this.ophProperties.url("kayttooikeus-service.cas.create-login-token", henkiloOid);
                    String loginToken = this.kayttooikeusRestClient.get(createLoginTokenUrl, String.class);
                    String strongIdentificationInfoRedirectUrl = this.ophProperties
                            .url("henkilo-ui.strong-identification", languageCode, loginToken);
                    getRedirectStrategy().sendRedirect(request, response, strongIdentificationInfoRedirectUrl);
                    return;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                String delimiter;
                if (targetUrl.contains("?")) {
                    delimiter = "&";
                }
                else {
                    delimiter = "?";
                }
                
                if (finalTargetUrl != null) {
                    logger.debug("Got final target url from session, adding to redirect url.");
                    // Double encode final url, otherwise final target urls part will be confused with the original url on flow: -> cas -> registration -> finaltarget
                    targetUrl += URLEncoder.encode("/" + URLEncoder.encode(finalTargetUrl, "UTF-8") , "UTF-8");
                }
                targetUrl += delimiter + AUTH_TOKEN_PARAMETER + "=" + URLEncoder.encode(authToken, "UTF-8");
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
            else if (StringUtils.isNotEmpty(temporaryToken) && token.getDetails() != null && token.getDetails() instanceof UserDetailsDto) {
                // Add userdetails to kayttooikeus-service.
                try {
                    String url = this.ophProperties.url("kayttooikeus-service.kutsu.update-identifier", temporaryToken);
                    this.kayttooikeusRestClient.put(url, MediaType.APPLICATION_JSON_VALUE, "{\"hakaIdentifier\": \"" + ((UserDetailsDto) token.getDetails()).getIdentifier() + "\"}");
                } catch (CachingRestClient.HttpException e) {
                    throw new RuntimeException("Could not update kutsu identifier", e);
                }
                Map<String, String> queryParams = new HashMap<String, String>(){{
                    put("temporaryKutsuToken", temporaryToken);
                }};
                String noAuthUrl = ophProperties.url("henkilo-ui.register", queryParams);
                getRedirectStrategy().sendRedirect(request, response, noAuthUrl);
                return;
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    public void setTokenProviders(HakaAuthTokenProvider tokenProviders) {
        this.tokenProviders = tokenProviders;
    }

    public HakaAuthTokenProvider getTokenProviders() {
        return tokenProviders;
    }

    public OppijanumeroRekisteriRestClient getOppijanumeroRekisteriRestClient() {
        return oppijanumeroRekisteriRestClient;
    }

    public void setOppijanumeroRekisteriRestClient(OppijanumeroRekisteriRestClient oppijanumeroRekisteriRestClient) {
        this.oppijanumeroRekisteriRestClient = oppijanumeroRekisteriRestClient;
    }

    public void setKayttooikeusRestClient(CachingRestClient kayttooikeusRestClient) {
        this.kayttooikeusRestClient = kayttooikeusRestClient;
    }
}
