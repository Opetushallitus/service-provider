package fi.vm.sade.saml.userdetails;

import org.apache.ws.security.WSPasswordCallback;
import org.springframework.beans.factory.annotation.Value;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * @author Eetu Blomqvist
 */
public class PropertyBackedCallback implements CallbackHandler {

    private @Value("${webservice.username}") String username;

    private @Value("${webservice.password}") String password;

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

        pc.setIdentifier(username);
        pc.setPassword(password);

    }
}
