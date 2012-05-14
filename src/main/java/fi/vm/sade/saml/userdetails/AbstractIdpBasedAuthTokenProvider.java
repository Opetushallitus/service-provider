/**
 * 
 */
package fi.vm.sade.saml.userdetails;

import java.util.List;

import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.springframework.security.saml.SAMLCredential;

import fi.vm.sade.auth.service.AuthenticationService;
import fi.vm.sade.auth.service.UserManagementService;

/**
 * @author tommiha
 *
 */
public abstract class AbstractIdpBasedAuthTokenProvider implements IdpBasedAuthTokenProvider {

    private List<String> supportedProviders;
    private UserManagementService userManagementService;
    private AuthenticationService authenticationService;
    
    /* (non-Javadoc)
     * @see fi.vm.sade.saml.userdetails.IdpBasedAuthTokenProvider#providesToken(java.lang.String)
     */
    @Override
    public boolean providesToken(String idp) {
        if(supportedProviders != null) {
            return supportedProviders.contains(idp);
        }
        return false;
    }
    
    protected String getFirstAttributeValue(SAMLCredential credential, String attributeName) {
        Attribute attrib = null;
        for(Attribute attr : credential.getAttributes()) {
            if(attr.getFriendlyName().equalsIgnoreCase(attributeName)) {
                attrib = attr;
                break;
            }
        }
        
        if(attrib == null) {
            return null;
        }
        
        XMLObject obj = attrib.getAttributeValues().get(0);
        if(obj instanceof XSString) {
            return ((XSString) obj).getValue();
        }
        return null;
    }
    
    /**
     * Returns IDP unique key.
     * @return
     */
    protected abstract String getIDPUniqueKey();
    

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
     * Entity IDs (from SAML provider metadata) supported by this token provider.
     * @param supportedProviders
     */
    public void setSupportedProviders(List<String> supportedProviders) {
        this.supportedProviders = supportedProviders;
    }

}
