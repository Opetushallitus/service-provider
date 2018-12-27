package fi.vm.sade.saml.clients;

import fi.vm.sade.javautils.http.OphHttpClient;
import fi.vm.sade.javautils.http.OphHttpEntity;
import fi.vm.sade.javautils.http.OphHttpRequest;
import fi.vm.sade.javautils.http.auth.CasAuthenticator;
import fi.vm.sade.properties.OphProperties;
import org.apache.http.entity.ContentType;

import java.util.Optional;
import java.util.function.Supplier;

import static fi.vm.sade.saml.clients.HttpClientUtil.CLIENT_SUBSYSTEM_CODE;
import static fi.vm.sade.saml.clients.HttpClientUtil.noContentOrNotFoundException;
import static java.util.function.Function.identity;

public class KayttooikeusRestClient {

    private final OphHttpClient httpClient;
    private final OphProperties properties;

    public KayttooikeusRestClient(OphProperties properties) {
        this(newHttpClient(properties), properties);
    }

    public KayttooikeusRestClient(OphHttpClient httpClient, OphProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    private static OphHttpClient newHttpClient(OphProperties properties) {
        CasAuthenticator authenticator = new CasAuthenticator.Builder()
                .username(properties.require("serviceprovider.app.username.to.usermanagement"))
                .password(properties.require("serviceprovider.app.password.to.usermanagement"))
                .webCasUrl(properties.url("cas.base"))
                .casServiceUrl(properties.url("kayttooikeus-service.security_check"))
                .build();
        return new OphHttpClient.Builder(CLIENT_SUBSYSTEM_CODE).authenticator(authenticator).build();
    }

    public String createLoginToken(String oid) {
        String url = properties.url("kayttooikeus-service.cas.create-login-token", oid);
        return httpClient.<String>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(identity())
                .orElseThrow(new Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return noContentOrNotFoundException(url);
                    }
                });
    }

    public void updateKutsuHakaIdentifier(String temporaryToken, String identifier) {
        String url = properties.url("kayttooikeus-service.kutsu.update-identifier", temporaryToken);
        OphHttpRequest request = OphHttpRequest.Builder
                .put(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content("{\"hakaIdentifier\": \"" + identifier + "\"}")
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        httpClient.<String>execute(request)
                .expectedStatus(200)
                .mapWith(identity())
                .orElseThrow(new Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return noContentOrNotFoundException(url);
                    }
                });
    }

    public Optional<String> getUserOidByIdentifier(String idpEntityId, String identifier) {
        String url = properties.url("kayttooikeus-service.cas.oidByIdp", idpEntityId, identifier);
        return httpClient.<String>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(identity());
    }

    public String getRedirectCodeByOid(String oid) {
        String url = properties.url("kayttooikeus-service.cas.login.redirect.oidHenkilo", oid);
        return httpClient.<String>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(identity())
                .orElseThrow(new Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return noContentOrNotFoundException(url);
                    }
                });
    }

    public String createAuthToken(String oid, String idpEntityId, String identifier) {
        String url = properties.url("kayttooikeus-service.cas.authTokenForOidAndIdp", oid, idpEntityId, identifier);
        return httpClient.<String>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(identity())
                .orElseThrow(new Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return noContentOrNotFoundException(url);
                    }
                });
    }

}
