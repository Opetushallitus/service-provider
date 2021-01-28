package fi.vm.sade.saml.entry;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.util.URLBuilder;
import org.opensaml.ws.transport.http.HTTPOutTransport;
import org.opensaml.xml.util.Pair;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml.SAMLConstants;
import org.springframework.security.saml.SAMLDiscovery;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.context.SAMLMessageContext;

/**
 * @author tommiha
 *
 */
public class RequestSavingSAMLEntryPoint extends SAMLEntryPoint {

	public static final String REDIRECT_KEY = "redirect";
	public static final String KUTSU_TEMP_TOKEN_KEY = "temporaryToken";

    private String returnUrl;
	
	@Override
	public void commence(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException e)
			throws IOException, ServletException {
		super.commence(request, response, e);

        String redirectFromReq = request.getParameter(REDIRECT_KEY);

        if (redirectFromReq != null) {
            // Only set to session if set in request
            logger.debug("Saving redirect url to session.");
            request.getSession().setAttribute(REDIRECT_KEY, redirectFromReq);
        }
        if (request.getParameter(KUTSU_TEMP_TOKEN_KEY) != null) {
            request.getSession().setAttribute(KUTSU_TEMP_TOKEN_KEY, request.getParameter(KUTSU_TEMP_TOKEN_KEY));
        }
	}


    /**
     * Method initializes IDP Discovery Profile as defined in http://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-idp-discovery.pdf
     * It is presumed that metadata of the local Service Provider contains discovery return address.
     *
     * @param context saml context also containing request and response objects
     * @throws ServletException          error
     * @throws IOException               io error
     * @throws org.opensaml.saml2.metadata.provider.MetadataProviderException in case metadata of the local entity can't be populated
     */
    protected void initializeDiscovery(SAMLMessageContext context) throws ServletException, IOException, MetadataProviderException {

        String discoveryURL = context.getLocalExtendedMetadata().getIdpDiscoveryURL();

        if (discoveryURL != null) {

            URLBuilder urlBuilder = new URLBuilder(discoveryURL);
            List<Pair<String, String>> queryParams = urlBuilder.getQueryParams();
            queryParams.add(new Pair<>(SAMLDiscovery.ENTITY_ID_PARAM, context.getLocalEntityId()));
            queryParams.add(new Pair<>(SAMLDiscovery.RETURN_ID_PARAM, IDP_PARAMETER));
            queryParams.add(new Pair<>(SAMLDiscovery.RETURN_URL_PARAM, returnUrl));
            discoveryURL = urlBuilder.buildURL();

            logger.debug("Using discovery URL from extended metadata");

        } else {

            String discoveryUrl = SAMLDiscovery.FILTER_URL;
            if (samlDiscovery != null) {
                discoveryUrl = samlDiscovery.getFilterProcessesUrl();
            }

            String contextPath = (String) context.getInboundMessageTransport().getAttribute(SAMLConstants.LOCAL_CONTEXT_PATH);
            discoveryURL = contextPath + discoveryUrl + "?" + SAMLDiscovery.RETURN_ID_PARAM + "=" + IDP_PARAMETER + "&" + SAMLDiscovery.ENTITY_ID_PARAM + "=" + context.getLocalEntityId();

            logger.debug("Using local discovery URL");

        }

        logger.debug("Redirecting to discovery URL " + discoveryURL);
        HTTPOutTransport response = (HTTPOutTransport) context.getOutboundMessageTransport();
        response.sendRedirect(discoveryURL);
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}
