package fi.vm.sade.saml.userdetails.mpassid;

import fi.vm.sade.saml.userdetails.AbstractIdpBasedAuthTokenProvider;

public class MpassidAuthTokenProvider extends AbstractIdpBasedAuthTokenProvider {

    private static final String HAKA_IDP_ID = "mpassid";

    @Override
    protected String getIDPUniqueKey() {
        return HAKA_IDP_ID;
    }

}
