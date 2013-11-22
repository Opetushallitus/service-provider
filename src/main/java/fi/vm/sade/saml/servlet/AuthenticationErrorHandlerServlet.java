package fi.vm.sade.saml.servlet;

import fi.vm.sade.authentication.business.exception.IdentificationExpiredException;
import fi.vm.sade.saml.exception.UnregisteredHakaUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Juuso Makinen <juuso.makinen@gofore.com>
 */
public class AuthenticationErrorHandlerServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationErrorHandlerServlet.class);

    private static final String ERROR_ATTR = "error";
    private static final String ERROR_TITLE = "title";
    private static final String ERROR_DESC = "description";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        respond(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        respond(req, resp);
    }

    protected void respond(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Throwable e = (Throwable) req.getAttribute("javax.servlet.error.exception");
        String errorMsg = e.getMessage();

        Map<String, String> error = new HashMap<String, String>();
        req.setAttribute(ERROR_ATTR, error);

        if(e instanceof UnregisteredHakaUserException) {
            error.put(ERROR_TITLE, "Haka-tunnistautumista ei aktivoitu");
            error.put(ERROR_DESC, "Suoritit onnistuneen Haka-tunnistautumisen, mutta Haka-tunnuksiisi ei ole " +
                    "liitetty Opintopolku.fi-k&auml;ytt&auml;j&auml;tunnusta. Mik&auml;li haluat tunnistautua " +
                    "Haka-tunnuksilla, ole hyv&auml; ja ota yhteytt&auml; Opintopolku.fi-yhteyshenkil&ouml;&ouml;si.");
        }
        // this is bit fragile, but SoapFault stack trace is rather lacking
        else if(errorMsg.contains("IdentificationExpiredException")) {
            error.put(ERROR_TITLE, "Haka-tunnukset vanhentuneet");
            error.put(ERROR_DESC, "Haka tunnuksilla ei ole kirjauduttu Opintopolku.fi:hin yli 24 kuukauteen. " +
                    "Ole hyv&auml; ja ota yhteytt&auml; Opintopolku.fi-yhteyshenkil&ouml;&ouml;si.");
        }

        if(error.get(ERROR_TITLE) == null) {
            error.put(ERROR_TITLE, "Odottamaton virhe tunnistautumisessa");
            error.put(ERROR_DESC, "Tunnistautumisessa tapahtui odottamaton virhe: </p><p>" + e.getMessage());
        }

        logger.debug("Got a {}, sending following error page to user: {}: {}",
                new String[] { e.toString(), error.get(ERROR_TITLE), error.get(ERROR_DESC) });
        req.getRequestDispatcher("/error.jsp").forward(req, resp);
        //resp.sendRedirect("/error.jsp");

    }
}
