/**
 * 
 */
package fi.vm.sade.saml.redirect;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import fi.vm.sade.saml.entry.RequestSavingSAMLEntryPoint;

/**
 * @author tommiha
 *
 */
public class AuthTokenAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String AUTH_TOKEN_PARAMETER = "authToken";
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        String targetUrl = getDefaultTargetUrl();
        String finalTargetUrl = (String) request.getSession().getAttribute(RequestSavingSAMLEntryPoint.REDIRECT_KEY);
        
        logger.info("Target url: " + targetUrl);
        
        if(authentication instanceof AbstractAuthenticationToken) {
            AbstractAuthenticationToken token = (AbstractAuthenticationToken) authentication;
            if(token.getDetails() != null && token.getDetails() instanceof String) {
                String authToken = (String) token.getDetails();
                String delimiter = "";
                if(targetUrl.contains("?")) {
                    delimiter = "&";
                } else {
                    delimiter = "?";
                }
                
                if (finalTargetUrl != null) {
                    logger.debug("Got final target url from session, adding to redirect url.");
                    targetUrl += URLEncoder.encode("/" + finalTargetUrl, "UTF-8");
                }
                targetUrl += delimiter + AUTH_TOKEN_PARAMETER + "=" + URLEncoder.encode(authToken, "UTF-8");
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

}
