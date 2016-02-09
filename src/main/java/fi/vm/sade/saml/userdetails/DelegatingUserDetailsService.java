/**
 * 
 */
package fi.vm.sade.saml.userdetails;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

/**
 * @author tommiha
 *
 */
public class DelegatingUserDetailsService implements SAMLUserDetailsService {

    private List<IdpBasedAuthTokenProvider> tokenProviders;
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    /* (non-Javadoc)
     * @see org.springframework.security.saml.userdetails.SAMLUserDetailsService#loadUserBySAML(org.springframework.security.saml.SAMLCredential)
     */
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        if(tokenProviders == null) {
            throw new RuntimeException("There are no authentication token providers set to DelegatingUserDetailsService.");
        }
        
        for(IdpBasedAuthTokenProvider provider : tokenProviders) {
            if(provider.providesToken(credential.getRemoteEntityID())) {
                try {
                    return provider.createAuthenticationToken(credential);
                }
                catch (Exception e) {
                    logger.error("Exception while creating authentication token for provider {}", credential.getRemoteEntityID(), e);
                    throw new UsernameNotFoundException("Exception while creating authentication token for provider " + credential.getRemoteEntityID(), e);
                }
            }
        }
        
        logger.error("Could not find authentication token provider for IDP: {}", credential.getRemoteEntityID());
        throw new UsernameNotFoundException("Could not find authentication token provider for IDP: " + credential.getRemoteEntityID());
    }

    public List<IdpBasedAuthTokenProvider> getTokenProviders() {
        return tokenProviders;
    }

    public void setTokenProviders(List<IdpBasedAuthTokenProvider> tokenProviders) {
        this.tokenProviders = tokenProviders;
    }

}
