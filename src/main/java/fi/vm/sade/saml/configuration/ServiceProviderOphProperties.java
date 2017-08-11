package fi.vm.sade.saml.configuration;

import fi.vm.sade.properties.OphProperties;
import java.nio.file.Paths;

public class ServiceProviderOphProperties extends OphProperties {

    public ServiceProviderOphProperties() {
        addFiles("/service-provider-oph.properties");
        addOptionalFiles(Paths.get(System.getProperties().getProperty("user.home"), "/oph-configuration/common.properties").toString());
        addOptionalFiles(Paths.get(System.getProperties().getProperty("user.home"), "/oph-configuration/service-provider-app.properties").toString());
    }

}
