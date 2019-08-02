package fi.vm.sade.saml.clients;

import fi.vm.sade.javautils.http.OphHttpClient;
import fi.vm.sade.javautils.http.OphHttpRequest;
import fi.vm.sade.javautils.http.auth.CasAuthenticator;
import fi.vm.sade.properties.OphProperties;

import java.util.function.Function;
import java.util.function.Supplier;

import static fi.vm.sade.saml.clients.HttpClientUtil.CALLER_ID;
import static fi.vm.sade.saml.clients.HttpClientUtil.noContentOrNotFoundException;

public class OppijanumeroRekisteriRestClient {

    private final OphHttpClient httpClient;
    private final OphProperties properties;

    public OppijanumeroRekisteriRestClient(OphProperties properties) {
        this(newHttpClient(properties), properties);
    }

    public OppijanumeroRekisteriRestClient(OphHttpClient httpClient, OphProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    private static OphHttpClient newHttpClient(OphProperties properties) {
        CasAuthenticator authenticator = new CasAuthenticator.Builder()
                .username(properties.require("serviceprovider.app.username.to.usermanagement"))
                .password(properties.require("serviceprovider.app.password.to.usermanagement"))
                .webCasUrl(properties.url("cas.base"))
                .casServiceUrl(properties.url("oppijanumerorekisteri-service.security_check"))
                .build();
        return new OphHttpClient.Builder(CALLER_ID).authenticator(authenticator).build();
    }

    private String jsonString(String json) {
        if (json == null) {
            return "";
        }
        return json.replaceAll("\"", "");
    }

    public String getAsiointikieli(String oid) {
        String url = properties.url("oppijanumerorekisteri.henkilo.kieliKoodi", oid);
        return httpClient.<String>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(new Function<String, String>() {
                    @Override
                    public String apply(String json) {
                        return jsonString(json);
                    }
                })
                .orElseThrow(new Supplier<RuntimeException>() {
                    @Override
                    public RuntimeException get() {
                        return noContentOrNotFoundException(url);
                    }
                });
    }

}
