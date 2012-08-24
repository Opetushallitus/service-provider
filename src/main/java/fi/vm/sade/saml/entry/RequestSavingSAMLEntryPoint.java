/**
 * 
 */
package fi.vm.sade.saml.entry;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml.SAMLEntryPoint;

/**
 * @author tommiha
 *
 */
public class RequestSavingSAMLEntryPoint extends SAMLEntryPoint {

	public static final String REDIRECT_KEY = "redirect";
	
	@Override
	public void commence(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException e)
			throws IOException, ServletException {
		super.commence(request, response, e);
		
		request.getSession().setAttribute(REDIRECT_KEY, request.getParameter(REDIRECT_KEY));
	}

}
