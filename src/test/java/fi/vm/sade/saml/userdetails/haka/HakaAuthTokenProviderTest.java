package fi.vm.sade.saml.userdetails.haka;

import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.saml.clients.KayttooikeusRestClient;
import fi.vm.sade.saml.exception.EmailVerificationException;
import fi.vm.sade.saml.exception.NoStrongIdentificationException;
import fi.vm.sade.saml.userdetails.AbstractIdpBasedAuthTokenProvider;
import fi.vm.sade.saml.userdetails.UserDetailsDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = HakaAuthTokenProvider.class)
public class HakaAuthTokenProviderTest {
    @Autowired
    private HakaAuthTokenProvider hakaAuthTokenProvider;

    private KayttooikeusRestClient kayttooikeusRestClient;
    private OphProperties ophProperties;

    private UserDetailsDto defaultUserDetailsDto;

    @Before
    public void setup() {
        this.kayttooikeusRestClient = mock(KayttooikeusRestClient.class);
        this.ophProperties = mock(OphProperties.class);

        this.hakaAuthTokenProvider.setKayttooikeusRestClient(this.kayttooikeusRestClient);
        this.hakaAuthTokenProvider.setOphProperties(this.ophProperties);
        this.hakaAuthTokenProvider.setHakaRequireStrongIdentificationListAsString("");
        this.hakaAuthTokenProvider.setHakaEmailVerificationListAsString("");

        UserDetailsDto userDetailsDto = new UserDetailsDto();
        userDetailsDto.setIdentifier("identifier");
        this.defaultUserDetailsDto = userDetailsDto;
    }

    @Test(expected = NoStrongIdentificationException.class)
    public void onStrongIdentificationListExistAndStrongIdentificationRedirectCodeShouldRedirectToStrongIdentification() throws Exception {
        this.hakaAuthTokenProvider.setHakaRequireStrongIdentificationListAsString("1.2.3.4.5,2.3.0.0.1");
        when(this.ophProperties.url(eq("kayttooikeus-service.cas.oidByIdp"), anyString(), anyString())).thenReturn("oidByIdpUrl");
        when(this.kayttooikeusRestClient.get(eq("oidByIdpUrl"), eq(String.class))).thenReturn("1.2.3.4.5");
        when(this.ophProperties.url(eq("kayttooikeus-service.cas.login.redirect.oidHenkilo"), eq("1.2.3.4.5"))).thenReturn("loginRedirectUrl");
        when(this.kayttooikeusRestClient.get(eq("loginRedirectUrl"), eq(String.class))).thenReturn(AbstractIdpBasedAuthTokenProvider.STRONG_IDENTIFICATION);
        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
    }

    @Test
    public void onStrongIdentificationDisabledAndEmailVerificationDisabledShouldNotRedirect() throws Exception {
        when(this.ophProperties.url(eq("kayttooikeus-service.cas.oidByIdp"), anyString(), anyString())).thenReturn("oidByIdpUrl");
        when(this.kayttooikeusRestClient.get(eq("oidByIdpUrl"), eq(String.class))).thenReturn("1.2.3.4.5");
        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
        verify(this.ophProperties, times(1)).url(anyString(), anyString(), anyString());
        verify(this.kayttooikeusRestClient, times(2)).get(anyString(), eq(String.class));
    }

    @Test
    public void onRequireStrongIdentificationAndStrongIdentificationListExistShouldCallForRedirectCode() throws Exception {
        this.hakaAuthTokenProvider.setRequireStrongIdentification(true);
        this.hakaAuthTokenProvider.setHakaRequireStrongIdentificationListAsString("1.2.3.4.5,2.3.4.45.56,2.3.4.6");
        when(this.kayttooikeusRestClient.get(anyString(), eq(String.class)))
                .thenReturn("1.2.3.4.5");
        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
        verify(this.ophProperties).url(eq("kayttooikeus-service.cas.oidByIdp"), anyString(), anyString());
        verify(this.ophProperties).url(eq("kayttooikeus-service.cas.login.redirect.oidHenkilo"), eq("1.2.3.4.5"));
    }

    @Test(expected = EmailVerificationException.class)
    public void onEmailVerificationEnabledAndRedirectCodeShouldRedirectToEmailVerification() throws Exception {
        this.hakaAuthTokenProvider.setEmailVerificationEnabled(true);
        when(this.ophProperties.url(eq("kayttooikeus-service.cas.login.redirect.oidHenkilo"), anyString())).thenReturn("loginRedirectUrl");
        when(this.kayttooikeusRestClient.get(eq("loginRedirectUrl"), eq(String.class))).thenReturn(AbstractIdpBasedAuthTokenProvider.EMAIL_VERIFICATION);
        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
        verify(this.ophProperties).url(eq("kayttooikeus-service.cas.oidByIdp"), anyString(), anyString());
        verify(this.ophProperties).url(eq("kayttooikeus-service.cas.login.redirect.oidHenkilo"), eq("1.2.3.4.5"));
    }

    @Test(expected = EmailVerificationException.class)
    public void onUserInEmailVerificationListAndEmailVerificationRedirectCodeShouldRedirectToEmailVerification() throws Exception {
        this.hakaAuthTokenProvider.setHakaEmailVerificationListAsString("2.3.4,1.2.3.4.5,3.7.5.23.67");
        when(this.kayttooikeusRestClient.get(anyString(), eq(String.class))).thenReturn("1.2.3.4.5");
        when(this.ophProperties.url("kayttooikeus-service.cas.login.redirect.oidHenkilo","1.2.3.4.5")).thenReturn("redirectCodeUrl");
        when(this.kayttooikeusRestClient.get(eq("redirectCodeUrl"), eq(String.class))).thenReturn(AbstractIdpBasedAuthTokenProvider.EMAIL_VERIFICATION);
        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
    }
}
