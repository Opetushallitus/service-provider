package fi.vm.sade.saml.userdetails.haka;

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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = HakaAuthTokenProvider.class)
public class HakaAuthTokenProviderTest {
    @Autowired
    private HakaAuthTokenProvider hakaAuthTokenProvider;

    private KayttooikeusRestClient kayttooikeusRestClient;

    private UserDetailsDto defaultUserDetailsDto;

    @Before
    public void setup() {
        this.kayttooikeusRestClient = mock(KayttooikeusRestClient.class);

        this.hakaAuthTokenProvider.setKayttooikeusRestClient(this.kayttooikeusRestClient);
        this.hakaAuthTokenProvider.setHakaRequireStrongIdentificationListAsString("");
        this.hakaAuthTokenProvider.setHakaEmailVerificationListAsString("");

        this.defaultUserDetailsDto = new UserDetailsDto("haka", "identifier");
    }

    @Test(expected = NoStrongIdentificationException.class)
    public void onStrongIdentificationListExistAndStrongIdentificationRedirectCodeShouldRedirectToStrongIdentification() throws Exception {
        this.hakaAuthTokenProvider.setHakaRequireStrongIdentificationListAsString("1.2.3.4.5,2.3.0.0.1");
        when(this.kayttooikeusRestClient.getUserOidByIdentifier(anyString(), anyString())).thenReturn(Optional.of("1.2.3.4.5"));
        when(this.kayttooikeusRestClient.getRedirectCodeByOid(anyString())).thenReturn(AbstractIdpBasedAuthTokenProvider.STRONG_IDENTIFICATION);
        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
    }

    @Test
    public void onStrongIdentificationDisabledAndEmailVerificationDisabledShouldNotRedirect() throws Exception {
        when(this.kayttooikeusRestClient.getUserOidByIdentifier(anyString(), anyString())).thenReturn(Optional.of("1.2.3.4.5"));
        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
        verify(this.kayttooikeusRestClient).getUserOidByIdentifier(anyString(), anyString());
        verify(this.kayttooikeusRestClient).createAuthToken(anyString(), anyString(), anyString());
    }

    @Test
    public void onRequireStrongIdentificationAndStrongIdentificationListExistShouldCallForRedirectCode() throws Exception {
        this.hakaAuthTokenProvider.setRequireStrongIdentification(true);
        this.hakaAuthTokenProvider.setHakaRequireStrongIdentificationListAsString("1.2.3.4.5,2.3.4.45.56,2.3.4.6");
        when(this.kayttooikeusRestClient.getUserOidByIdentifier(anyString(), anyString()))
                .thenReturn(Optional.of("1.2.3.4.5"));
        when(this.kayttooikeusRestClient.getRedirectCodeByOid(anyString())).thenReturn(null);
        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
        verify(this.kayttooikeusRestClient).getUserOidByIdentifier(anyString(), anyString());
        verify(this.kayttooikeusRestClient).getRedirectCodeByOid(eq("1.2.3.4.5"));
    }

    @Test(expected = EmailVerificationException.class)
    public void onEmailVerificationEnabledAndRedirectCodeShouldRedirectToEmailVerification() throws Exception {
        this.hakaAuthTokenProvider.setEmailVerificationEnabled(true);
        when(this.kayttooikeusRestClient.getUserOidByIdentifier(anyString(), anyString())).thenReturn(Optional.of("1.2.3.4.5"));
        when(this.kayttooikeusRestClient.getRedirectCodeByOid(anyString())).thenReturn(AbstractIdpBasedAuthTokenProvider.EMAIL_VERIFICATION);
        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
        verify(this.kayttooikeusRestClient).getUserOidByIdentifier(anyString(), anyString());
        verify(this.kayttooikeusRestClient).getRedirectCodeByOid(eq("1.2.3.4.5"));
    }

    @Test(expected = EmailVerificationException.class)
    public void onUserInEmailVerificationListAndEmailVerificationRedirectCodeShouldRedirectToEmailVerification() throws Exception {
        this.hakaAuthTokenProvider.setHakaEmailVerificationListAsString("2.3.4,1.2.3.4.5,3.7.5.23.67");
        when(this.kayttooikeusRestClient.getUserOidByIdentifier(anyString(), anyString())).thenReturn(Optional.of("1.2.3.4.5"));
        when(this.kayttooikeusRestClient.getRedirectCodeByOid(anyString())).thenReturn(AbstractIdpBasedAuthTokenProvider.EMAIL_VERIFICATION);
        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
    }
}
