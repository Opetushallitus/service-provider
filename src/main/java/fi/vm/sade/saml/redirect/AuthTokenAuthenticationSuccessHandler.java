package fi.vm.sade.saml.redirect;

import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.saml.clients.KayttooikeusRestClient;
import fi.vm.sade.saml.clients.OppijanumeroRekisteriRestClient;
import fi.vm.sade.saml.entry.RequestSavingSAMLEntryPoint;
import fi.vm.sade.saml.exception.EmailVerificationException;
import fi.vm.sade.saml.exception.NoStrongIdentificationException;
import fi.vm.sade.saml.exception.UnregisteredUserException;
import fi.vm.sade.saml.userdetails.UserDetailsDto;
import fi.vm.sade.saml.userdetails.haka.HakaAuthTokenProvider;
import org.apache.commons.lang.StringUtils;
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
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthTokenAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String AUTH_TOKEN_PARAMETER = "authToken";
    public static final String HENKILO_UI_TOKEN_PARAMETER = "temporaryKutsuToken";

    private final OphProperties ophProperties;
    private KayttooikeusRestClient kayttooikeusRestClient;
    private OppijanumeroRekisteriRestClient oppijanumeroRekisteriRestClient;

    private HakaAuthTokenProvider tokenProviders;


    public AuthTokenAuthenticationSuccessHandler(OphProperties ophProperties) {
        this.ophProperties = ophProperties;
    }

    public void initialize() {
        setDefaultTargetUrl(ophProperties.url("cas.login"));
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String targetUrl = getDefaultTargetUrl();
        String finalTargetUrl = (String) request.getSession().getAttribute(RequestSavingSAMLEntryPoint.REDIRECT_KEY);
        if (finalTargetUrl != null) {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("service", finalTargetUrl);
            targetUrl = ophProperties.url("cas.login", parameters);
        }
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
                    String languageCode = oppijanumeroRekisteriRestClient.getAsiointikieli(henkiloOid);
                    String loginToken = this.kayttooikeusRestClient.createLoginToken(henkiloOid);
                    String strongIdentificationInfoRedirectUrl = this.ophProperties
                            .url("henkilo-ui.strong-identification", languageCode, loginToken);
                    getRedirectStrategy().sendRedirect(request, response, strongIdentificationInfoRedirectUrl);
                    return;
                } catch (EmailVerificationException e) {
                    String henkiloOid = e.getMessage();
                    String languageCode = oppijanumeroRekisteriRestClient.getAsiointikieli(henkiloOid);
                    String loginToken = this.kayttooikeusRestClient.createLoginToken(henkiloOid);
                    String emailVerificationUrl = this.ophProperties.url("henkilo-ui.email-verification", languageCode, loginToken);
                    getRedirectStrategy().sendRedirect(request, response, emailVerificationUrl);
                    return;
                } catch (UnregisteredUserException e) {
                    // poikkeusta käytetään virheilmoituksen näyttämiseen (kts. AuthenticationErrorHandlerServlet)
                    throw e;
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

                targetUrl += delimiter + AUTH_TOKEN_PARAMETER + "=" + URLEncoder.encode(authToken, "UTF-8");
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
            else if (StringUtils.isNotEmpty(temporaryToken) && token.getDetails() != null && token.getDetails() instanceof UserDetailsDto) {
                // Add userdetails to kayttooikeus-service.
                kayttooikeusRestClient.updateKutsuHakaIdentifier(temporaryToken, ((UserDetailsDto) token.getDetails()).getIdentifier());
                Map<String, String> queryParams = new HashMap<String, String>(){{
                    put(HENKILO_UI_TOKEN_PARAMETER, temporaryToken);
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

    public void setKayttooikeusRestClient(KayttooikeusRestClient kayttooikeusRestClient) {
        this.kayttooikeusRestClient = kayttooikeusRestClient;
    }
}
