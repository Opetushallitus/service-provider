package fi.vm.sade.saml.servlet;

import fi.vm.sade.saml.exception.RequiredSamlAttributeNotProvidedException;
import fi.vm.sade.saml.exception.UnregisteredUserException;
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
    private static final String ERROR_TITLE_SV = "title_sv";
    private static final String ERROR_DESC = "description";
    private static final String ERROR_DESC_SV = "description_sv";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        respond(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        respond(req, resp);
    }

    private void putError(Map<String, String> error, String title, String titleSv, String desc, String descSv) {
        error.putAll(Map.of(
            ERROR_TITLE, title,
            ERROR_TITLE_SV, titleSv,
            ERROR_DESC, desc,
            ERROR_DESC_SV, descSv
        ));
    }

    private Map<String, String> getErrorTranslations(Throwable exception) {
        Map<String, String> error = new HashMap<>();
        if (exception instanceof UnregisteredUserException e) {
            if ("mpassid".equals(e.getIdpType())) {
                putError(error,
                    "MPASSid-kirjautumista ei ole aktivoitu",
                    "MPASSid-inloggning har inte aktiverats",
                    "MPASSid-kirjautuminen onnistui, mutta Opintopolku-tiliisi ei ole liitetty MPASSid-tunnistetietoja. Mikäli haluat kirjautua MPASSid:llä Opintopolun virkailijapalveluihin, kirjaudu ensin Opintopolun tunnuksella tai suomi.fi-tunnistuksella. Lisää sitten MPASSid-tunnistetiedot omiin tietoihisi (https://virkailija.opintopolku.fi/henkilo-ui/omattiedot).",
                    "Du har loggat in med MPASSid, men dina MPASSid-identifikationsuppgifter har inte bifogats till dina användaruppgifter i Studieinfo. Om du vill logga in med MPASSid i Studieinfos administratörstjänster, logga då först in med Studieinfo-användarnamnet eller suomi.fi-identifiering. Lägg sedan till MPASSid-identifikationsuppgifterna till dina egna uppgifter (https://virkailija.opintopolku.fi/henkilo-ui/omattiedot).");
            } else {
                putError(error,
                    "Haka-tunnistautumista ei ole aktivoitu",
                    "Haka-identifiering har inte aktiverats",
                    "Haka-tunnistautuminen onnistui, mutta Opintopolku-tiliisi ei ole liitetty Haka-käyttäjätunnusta. Mikäli haluat tunnistautua Haka-tunnuksilla, ole hyvä ja ota yhteyttä organisaatiosi pääkäyttäjään.",
                    "Du har loggat in med Haka-identifiering, men Haka-användarnamnet har inte bifogats till dina användaruppgifter i Studieinfo för administratörer. Om du vill identifiera dig med ditt Haka-användarnamn, bör du ta kontakt med huvudanvändaren i din egen organisation.");
            }
        } else if (exception instanceof RequiredSamlAttributeNotProvidedException e) {
            if ("mpassid".equals(e.getIdpType())) {
                putError(error,
                    "MPASSid ei toimittanut vaadittuja tietoja",
                    "MPASSid förmedlade inte behövliga uppgifter",
                    "Palvelun käyttäminen vaatii, että sallit MPASSid:stä vaadittujen tietojen toimittamisen.",
                    "För att använda tjänsten krävs att du tillåter att behövliga uppgifter från MPASSid kan förmedlas.");
            } else {
                putError(error,
                    "Haka ei toimittanut vaadittuja tietoja",
                    "Haka förmedlade inte behövliga uppgifter",
                    "Palvelun käyttäminen vaatii, että sallit Haka:sta vaadittujen tietojen toimittamisen.",
                    "För att använda tjänsten krävs att du tillåter att behövliga uppgifter från Haka kan förmedlas.");
            }
        }

        if (error.get(ERROR_TITLE) == null) {
            putError(error, "Odottamaton virhe tunnistautumisessa", "Oväntat fel vid identifiering", "Tunnistautumisessa tapahtui odottamaton virhe: </p><p>" + exception.getMessage(), "Ett oväntat fel inträffade vid identifiering: </p><p>" + exception.getMessage());
        }

        return error;
    }

    protected void respond(HttpServletRequest req, HttpServletResponse resp) {
        Throwable exception = (Throwable) req.getAttribute("javax.servlet.error.exception");
        Map<String, String> error = getErrorTranslations(exception);
        req.setAttribute(ERROR_ATTR, error);

        logger.debug("Got a {}, sending following error page to user: {}: {}", exception.toString(), error.get(ERROR_TITLE), error.get(ERROR_DESC));
        resp.setStatus(HttpServletResponse.SC_CONFLICT);

        try {
            req.getRequestDispatcher("/error.jsp").forward(req, resp);
        } catch (ServletException e) {
            logger.error("failed to redirect to error page", e);
        } catch (IOException e) {
            logger.error("failed to redirect to error page", e);
        }
    }
}
