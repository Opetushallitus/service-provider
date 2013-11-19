package fi.vm.sade.saml.redirect;

import fi.vm.sade.saml.exception.UnregisteredHakaUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Juuso
 * Date: 19.11.2013
 * Time: 12:37
 * To change this template use File | Settings | File Templates.
 */
public class HakaAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private Logger logger = LoggerFactory.getLogger(HakaAuthenticationFailureHandler.class);

    private String hakaAuthFailureUrl;

    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        if(exception instanceof UnregisteredHakaUserException) {
            logger.debug("Redirecting user to {}", hakaAuthFailureUrl);
            getRedirectStrategy().sendRedirect(request, response, hakaAuthFailureUrl);
            //request.getRequestDispatcher(hakaAuthFailureUrl).forward(request, response);
        }
        else {
            super.onAuthenticationFailure(request, response, exception);
        }
    }

    public String getHakaAuthFailureUrl() {
        return hakaAuthFailureUrl;
    }

    public void setHakaAuthFailureUrl(String hakaAuthFailureUrl) {
        this.hakaAuthFailureUrl = hakaAuthFailureUrl;
    }
}
