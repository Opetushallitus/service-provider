/**
 * 
 */
package fi.vm.sade.saml.userdetails;

import java.util.List;

import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.springframework.security.saml.SAMLCredential;

import fi.vm.sade.authentication.service.AuthenticationService;
import fi.vm.sade.authentication.service.UserManagementService;
import fi.vm.sade.authentication.service.types.AddHenkiloData;
import fi.vm.sade.authentication.service.types.dto.HenkiloDTO;

/**
 * @author tommiha
 * 
 */
public abstract class AbstractIdpBasedAuthTokenProvider implements IdpBasedAuthTokenProvider {

    private List<String> supportedProviders;
    private UserManagementService userManagementService;
    private AuthenticationService authenticationService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * fi.vm.sade.saml.userdetails.IdpBasedAuthTokenProvider#providesToken(java
     * .lang.String)
     */
    @Override
    public boolean providesToken(String idp) {
        if (supportedProviders != null) {
            return supportedProviders.contains(idp);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fi.vm.sade.saml.userdetails.IdpBasedAuthTokenProvider#
     * createAuthenticationToken
     * (org.springframework.security.saml.SAMLCredential)
     */
    @Override
    public String createAuthenticationToken(SAMLCredential credential) {
        HenkiloDTO henkilo = getUserManagementService().getHenkiloByIDPAndIdentifier(getIDPUniqueKey(), getUniqueIdentifier(credential));
        if (henkilo == null) {
            henkilo = createIdentity(credential);

            AddHenkiloData addHenkiloData = new AddHenkiloData();
            addHenkiloData.setEtunimet(henkilo.getEtunimet());
            addHenkiloData.setHetu(henkilo.getHetu());
            addHenkiloData.setKotikunta(henkilo.getKotikunta());
            addHenkiloData.setKutsumanimi(henkilo.getKutsumanimi());
            addHenkiloData.setSukunimi(henkilo.getSukunimi());
            addHenkiloData.setSukupuoli(henkilo.getSukupuoli());
            addHenkiloData.setTurvakielto(henkilo.isTurvakielto());
            henkilo = getUserManagementService().addHenkilo(addHenkiloData);
        }
        return getAuthenticationService().generateAuthTokenForHenkilo(henkilo, getIDPUniqueKey(), getUniqueIdentifier(credential));
    }

    protected String getFirstAttributeValue(SAMLCredential credential, String attributeName) {
        Attribute attrib = null;
        for (Attribute attr : credential.getAttributes()) {
            if (attr.getFriendlyName().equalsIgnoreCase(attributeName)) {
                attrib = attr;
                break;
            }
        }

        if (attrib == null) {
            return null;
        }

        XMLObject obj = attrib.getAttributeValues().get(0);
        if (obj instanceof XSString) {
            return ((XSString) obj).getValue();
        }
        return null;
    }

    /**
     * Creates Henkilo from SAMLCredentials.
     * 
     * @param credential
     * @return
     */
    protected abstract HenkiloDTO createIdentity(SAMLCredential credential);

    /**
     * Returns IDP unique key.
     * 
     * @return
     */
    protected abstract String getIDPUniqueKey();

    /**
     * 
     * @param credential
     * @return
     */
    protected abstract String getUniqueIdentifier(SAMLCredential credential);

    public UserManagementService getUserManagementService() {
        return userManagementService;
    }

    public void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public List<String> getSupportedProviders() {
        return supportedProviders;
    }

    /**
     * Entity IDs (from SAML provider metadata) supported by this token
     * provider.
     * 
     * @param supportedProviders
     */
    public void setSupportedProviders(List<String> supportedProviders) {
        this.supportedProviders = supportedProviders;
    }

}
