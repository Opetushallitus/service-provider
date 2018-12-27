/**
 * 
 */
package fi.vm.sade.saml.userdetails.haka;

import fi.vm.sade.saml.userdetails.AbstractIdpBasedAuthTokenProvider;

public class HakaAuthTokenProvider extends AbstractIdpBasedAuthTokenProvider {

    private static final String HAKA_IDP_ID = "haka";

    @Override
    protected String getIDPUniqueKey() {
        return HAKA_IDP_ID;
    }

}
