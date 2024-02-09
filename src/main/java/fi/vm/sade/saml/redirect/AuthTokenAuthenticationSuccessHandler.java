package fi.vm.sade.saml.redirect;

import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.saml.clients.KayttooikeusRestClient;
import fi.vm.sade.saml.clients.OppijanumeroRekisteriRestClient;
import fi.vm.sade.saml.entry.RequestSavingSAMLEntryPoint;
import fi.vm.sade.saml.exception.EmailVerificationException;
import fi.vm.sade.saml.exception.NoStrongIdentificationException;
import fi.vm.sade.saml.exception.PasswordChangeException;
import fi.vm.sade.saml.exception.UnregisteredUserException;
import fi.vm.sade.saml.userdetails.AbstractIdpBasedAuthTokenProvider;
import fi.vm.sade.saml.userdetails.UserDetailsDto;
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
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthTokenAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String AUTH_TOKEN_PARAMETER = "authToken";
    public static final String HENKILO_UI_TOKEN_PARAMETER = "temporaryKutsuToken";

    private final OphProperties ophProperties;
    private KayttooikeusRestClient kayttooikeusRestClient;
    private OppijanumeroRekisteriRestClient oppijanumeroRekisteriRestClient;

    private Map<String, AbstractIdpBasedAuthTokenProvider> tokenProviders;


    public AuthTokenAuthenticationSuccessHandler(OphProperties ophProperties) {
        this.ophProperties = ophProperties;
    }

    public void initialize() {
        setDefaultTargetUrl(ophProperties.url("cas.login"));
    }

    private String loginTokenUrl(String henkiloOid, String urlKey) {
        String languageCode = oppijanumeroRekisteriRestClient.getAsiointikieli(henkiloOid);
        String loginToken = kayttooikeusRestClient.createLoginToken(henkiloOid);
        return ophProperties.url(urlKey, languageCode, loginToken);
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
        
        if (authentication instanceof AbstractAuthenticationToken token) {
            if (StringUtils.isEmpty(temporaryToken) && token.getDetails() != null && token.getDetails() instanceof UserDetailsDto userDetails) {
                String authToken;
                try {
                    AbstractIdpBasedAuthTokenProvider tokenProvider = tokenProviders.get(userDetails.getAuthenticationMethod());
                    authToken = tokenProvider.createAuthenticationToken((SAMLCredential) authentication.getCredentials(), userDetails);
                } catch (NoStrongIdentificationException e) {
                    getRedirectStrategy().sendRedirect(request, response, loginTokenUrl(e.getMessage(), "henkilo-ui.strong-identification"));
                    return;
                } catch (EmailVerificationException e) {
                    getRedirectStrategy().sendRedirect(request, response, loginTokenUrl(e.getMessage(), "henkilo-ui.email-verification"));
                    return;
                } catch (PasswordChangeException e) {
                    getRedirectStrategy().sendRedirect(request, response, loginTokenUrl(e.getMessage(), "henkilo-ui.password-change"));
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
            else if (StringUtils.isNotEmpty(temporaryToken) && token.getDetails() != null && token.getDetails() instanceof UserDetailsDto details) {
                // Add userdetails to kayttooikeus-service.
                kayttooikeusRestClient.updateKutsuHakaIdentifier(temporaryToken, details.getIdentifier());
                Map<String, String> queryParams = Map.of(HENKILO_UI_TOKEN_PARAMETER, temporaryToken);
                String noAuthUrl = ophProperties.url("henkilo-ui.register", queryParams);
                getRedirectStrategy().sendRedirect(request, response, noAuthUrl);
                return;
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    public void setTokenProviders(Map<String, AbstractIdpBasedAuthTokenProvider> tokenProviders) {
        this.tokenProviders = tokenProviders;
    }

    public Map<String, AbstractIdpBasedAuthTokenProvider> getTokenProviders() {
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
