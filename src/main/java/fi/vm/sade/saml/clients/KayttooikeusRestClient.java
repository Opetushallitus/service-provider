package fi.vm.sade.saml.clients;

import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.properties.OphProperties;

public class KayttooikeusRestClient extends CachingRestClient {
    public KayttooikeusRestClient(OphProperties ophProperties) {
        this.setCasService(ophProperties.url("kayttooikeus-service.security_check"));
        this.setWebCasUrl(ophProperties.url("cas.base"));
    }

}
