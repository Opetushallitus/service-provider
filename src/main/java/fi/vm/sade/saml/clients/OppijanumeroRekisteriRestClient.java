package fi.vm.sade.saml.clients;

import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.properties.OphProperties;

public class OppijanumeroRekisteriRestClient extends CachingRestClient {
    public OppijanumeroRekisteriRestClient(OphProperties ophProperties) {
        this.setCasService(ophProperties.url("oppijanumerorekisteri-service.security_check"));
        this.setWebCasUrl(ophProperties.url("cas.base"));
    }

}
