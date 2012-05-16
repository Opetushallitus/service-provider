/**
 * 
 */
package fi.vm.sade.saml.redirect;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

/**
 * @author tommiha
 *
 */
public class AuthTokenAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String AUTH_TOKEN_PARAMETER = "authToken";
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if(authentication instanceof AbstractAuthenticationToken) {
            AbstractAuthenticationToken token = (AbstractAuthenticationToken) authentication;
            if(token.getDetails() != null && token.getDetails() instanceof String) {
                String authToken = (String) token.getDetails();
                String targetUrl = getDefaultTargetUrl();
                if(targetUrl.contains("?")) {
                    targetUrl += "&" + AUTH_TOKEN_PARAMETER + "=" + authToken;
                }
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

}
