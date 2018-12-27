package fi.vm.sade.saml.clients;

public final class HttpClientUtil {

    private HttpClientUtil() {
    }

    public static final String CLIENT_SUBSYSTEM_CODE = "service-provider";

    public static RuntimeException noContentOrNotFoundException(String url) {
        return new RuntimeException(String.format("Service %s returned status 204 or 404", url));
    }

}
