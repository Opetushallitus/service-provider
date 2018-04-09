package fi.vm.sade.saml.userdetails.haka;

import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.saml.clients.KayttooikeusRestClient;
import fi.vm.sade.saml.exception.NoStrongIdentificationException;
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

        UserDetailsDto userDetailsDto = new UserDetailsDto();
        userDetailsDto.setIdentifier("identifier");
        this.defaultUserDetailsDto = userDetailsDto;
    }

    @Test
    public void hakaRequireStrongIdentificationOidlistIsRead() throws Exception {
        this.hakaAuthTokenProvider.setRequireStrongIdentification(true);
        this.hakaAuthTokenProvider.setHakaRequireStrongIdentificationListAsString("1.2.3.4.5,2.3.0.0.1");
        when(this.kayttooikeusRestClient.get(anyString(), eq(String.class))).thenReturn("1.2.3.4.5");

        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
        verify(this.ophProperties, times(1))
                .url(eq("kayttooikeus-service.cas.vahva-tunnistus"), eq("1.2.3.4.5"));
    }

    @Test
    public void hakaRequireStrongIdentificationOidlistDefaultIsRead() throws Exception {
        this.hakaAuthTokenProvider.setRequireStrongIdentification(true);
        when(this.kayttooikeusRestClient.get(anyString(), eq(String.class))).thenReturn("1.2.3.4.5");

        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
        verify(this.ophProperties, times(1))
                .url(eq("kayttooikeus-service.cas.vahva-tunnistus"), eq("1.2.3.4.5"));
    }

    @Test(expected = NoStrongIdentificationException.class)
    public void hakaUserNotStronglyIdentifiedButIsRequiredTo() throws Exception {
        this.hakaAuthTokenProvider.setRequireStrongIdentification(true);
        when(this.kayttooikeusRestClient.get(anyString(), eq(String.class))).thenReturn("1.2.3.4.5");
        when(this.kayttooikeusRestClient.get(anyString(), eq(Boolean.class))).thenReturn(false);

        this.hakaAuthTokenProvider.createAuthenticationToken(null, this.defaultUserDetailsDto);
    }
}
