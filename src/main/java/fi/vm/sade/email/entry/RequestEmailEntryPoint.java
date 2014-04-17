package fi.vm.sade.email.entry;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import fi.vm.sade.generic.rest.CachingRestClient;

public class RequestEmailEntryPoint {
    
    private CachingRestClient restClient = new CachingRestClient();
    private String redirectUrl;
    private String henkiloServiceUrl;

    @Path("{token}")
    @GET
    public Response initializeEmailRegistration(@PathParam("token") String base64EmailToken) {
        Response response = null;
        
        try {
            boolean found = restClient.get(henkiloServiceUrl + base64EmailToken, boolean.class);
            // TODO!! Tähän jotenkin se eteenpäin redirectaus CASiin ja siitä läpi!!
        }
        catch (IOException ioe) {
            // TODO!! Loggeriin!!
        }
        
        return response;
    }
    
    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getHenkiloServiceUrl() {
        return henkiloServiceUrl;
    }

    public void setHenkiloServiceUrl(String henkiloServiceUrl) {
        this.henkiloServiceUrl = henkiloServiceUrl;
    }
}
