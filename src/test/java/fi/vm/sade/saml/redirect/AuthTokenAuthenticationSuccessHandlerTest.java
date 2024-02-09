package fi.vm.sade.saml.redirect;

import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.saml.clients.KayttooikeusRestClient;
import fi.vm.sade.saml.clients.OppijanumeroRekisteriRestClient;
import fi.vm.sade.saml.entry.RequestSavingSAMLEntryPoint;
import fi.vm.sade.saml.exception.PasswordChangeException;
import fi.vm.sade.saml.exception.UnregisteredUserException;
import fi.vm.sade.saml.userdetails.UserDetailsDto;
import fi.vm.sade.saml.userdetails.haka.HakaAuthTokenProvider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.RedirectStrategy;

import java.util.Map;

public class AuthTokenAuthenticationSuccessHandlerTest {

    private AuthTokenAuthenticationSuccessHandler handler;

    private HttpServletRequest httpRequestMock;
    private HttpServletResponse httpResponseMock;
    private HttpSession httpSessionMock;
    private RedirectStrategy redirectStrategyMock;
    private HakaAuthTokenProvider hakaAuthTokenProviderMock;
    private OppijanumeroRekisteriRestClient onrClient;
    private KayttooikeusRestClient kayttooikeusRestClient;
    private final TestingAuthenticationToken authentication = new TestingAuthenticationToken(null, null);

    @Before
    public void setup() {
        httpRequestMock = mock(HttpServletRequest.class);
        httpResponseMock = mock(HttpServletResponse.class);
        httpSessionMock = mock(HttpSession.class);
        redirectStrategyMock = mock(RedirectStrategy.class);
        hakaAuthTokenProviderMock = mock(HakaAuthTokenProvider.class);
        onrClient = mock(OppijanumeroRekisteriRestClient.class);
        kayttooikeusRestClient = mock(KayttooikeusRestClient.class);
        authentication.setDetails(new UserDetailsDto("haka", null));
        when(httpRequestMock.getSession()).thenReturn(httpSessionMock);

        OphProperties ophProperties = new OphProperties("/service-provider-oph.properties");
        ophProperties.addDefault("host.cas", "virkailija.opintopolku.fi");
        ophProperties.addDefault("host.virkailija", "virkailija.opintopolku.fi");
        ophProperties.addDefault("henkilo-ui.password-change", "${url-virkailija}/henkilo-ui/kayttaja/salasananvaihto/$1/$2");

        handler = new AuthTokenAuthenticationSuccessHandler(ophProperties);
        handler.setRedirectStrategy(redirectStrategyMock);
        handler.setTokenProviders(Map.of("haka", hakaAuthTokenProviderMock));
        handler.setOppijanumeroRekisteriRestClient(onrClient);
        handler.setKayttooikeusRestClient(kayttooikeusRestClient);
        handler.initialize();
    }

    @Test
    public void onAuthenticationSuccessShouldRespectRedirectKey() throws Exception {
        when(httpSessionMock.getAttribute(eq(RequestSavingSAMLEntryPoint.REDIRECT_KEY)))
                .thenReturn("https://virkailija.opintopolku.fi/virkailijan-tyopoyta/authenticate");
        when(hakaAuthTokenProviderMock.createAuthenticationToken(any(), any()))
                .thenReturn("authtoken123");

        handler.onAuthenticationSuccess(httpRequestMock, httpResponseMock, authentication);

        verify(redirectStrategyMock).sendRedirect(eq(httpRequestMock), eq(httpResponseMock),
                eq("https://virkailija.opintopolku.fi/cas/login?service=https%3A%2F%2Fvirkailija.opintopolku.fi%2Fvirkailijan-tyopoyta%2Fauthenticate&authToken=authtoken123"));
    }

    @Test
    public void onAuthenticationSuccessShouldFallbackToDefaultTargetUrl() throws Exception {
        when(httpSessionMock.getAttribute(eq(RequestSavingSAMLEntryPoint.REDIRECT_KEY)))
                .thenReturn(null);
        when(hakaAuthTokenProviderMock.createAuthenticationToken(any(), any()))
                .thenReturn("authtoken123");

        handler.onAuthenticationSuccess(httpRequestMock, httpResponseMock, authentication);

        verify(redirectStrategyMock).sendRedirect(eq(httpRequestMock), eq(httpResponseMock),
                eq("https://virkailija.opintopolku.fi/cas/login?authToken=authtoken123"));
    }

    @Test(expected = UnregisteredUserException.class)
    public void onAuthenticationSuccessShouldNotCatchUnregisteredHakaUserException() throws Exception {
        when(httpSessionMock.getAttribute(eq(RequestSavingSAMLEntryPoint.REDIRECT_KEY)))
                .thenReturn(null);
        when(hakaAuthTokenProviderMock.createAuthenticationToken(any(), any()))
                .thenThrow(new UnregisteredUserException("exception from mock", "haka"));

        handler.onAuthenticationSuccess(httpRequestMock, httpResponseMock, authentication);
    }

    @Test
    public void onAuthenticationSuccessShouldRedirectToPasswordChange() throws Exception {
        when(httpSessionMock.getAttribute(eq(RequestSavingSAMLEntryPoint.REDIRECT_KEY)))
                .thenReturn(null);
        when(hakaAuthTokenProviderMock.createAuthenticationToken(any(), any()))
                .thenThrow(new PasswordChangeException("oid"));
        when(onrClient.getAsiointikieli(eq("oid"))).thenReturn("sv");
        when(kayttooikeusRestClient.createLoginToken(eq("oid"))).thenReturn("logintokin");

        handler.onAuthenticationSuccess(httpRequestMock, httpResponseMock, authentication);

        verify(redirectStrategyMock).sendRedirect(eq(httpRequestMock), eq(httpResponseMock),
                eq("https://virkailija.opintopolku.fi/henkilo-ui/kayttaja/salasananvaihto/sv/logintokin"));
    }

}
