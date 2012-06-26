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
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.StringUtils;

/**
 * @author tommiha
 *
 */
public class AuthTokenAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String AUTH_TOKEN_PARAMETER = "authToken";
    
    private RequestCache requestCache = new HttpSessionRequestCache();
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String targetUrl = getDefaultTargetUrl();
        
        if (savedRequest != null) {
            if(StringUtils.hasText(request.getParameter(getTargetUrlParameter()))) {
                requestCache.removeRequest(request, response);
            }
            
            targetUrl = savedRequest.getRedirectUrl();
        }
        
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
                targetUrl += delimiter + AUTH_TOKEN_PARAMETER + "=" + URLEncoder.encode(authToken, "UTF-8");
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

}
