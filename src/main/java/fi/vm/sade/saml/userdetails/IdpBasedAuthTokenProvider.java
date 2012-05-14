/**
 * 
 */
package fi.vm.sade.saml.userdetails;

import org.springframework.security.saml.SAMLCredential;

/**
 * Authentication token provider by SAML IDP interface.
 * 
 * @author tommiha
 */
public interface IdpBasedAuthTokenProvider {

    /**
     * Returns true if implementation can create authentication token for given IDP. 
     * @param idp
     * @return
     */
    boolean providesToken(String idp);
    
    /**
     * Creates authentication token by SAMLCredentials.
     * @param credential
     * @return
     */
    String createAuthenticationToken(SAMLCredential credential);
}
