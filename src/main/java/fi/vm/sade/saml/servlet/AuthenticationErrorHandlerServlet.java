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

    protected void respond(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Throwable e = (Throwable) req.getAttribute("javax.servlet.error.exception");
        Map<String, String> error = new HashMap<>();
        req.setAttribute(ERROR_ATTR, error);

        if (e instanceof UnregisteredUserException) {
            String idpType = ((UnregisteredUserException) e).getIdpType();
            if ("mpassid".equals(idpType)) {
                error.put(ERROR_TITLE, "MPASSid-kirjautumista ei ole aktivoitu");
                error.put(ERROR_TITLE_SV, "MPASSid-inloggning har inte aktiverats");
                error.put(ERROR_DESC, "MPASSid-kirjautuminen onnistui, mutta Opintopolku-tiliisi ei ole liitetty MPASSid-tunnistetietoja. Mikäli haluat kirjautua MPASSid:llä Opintopolun virkailijapalveluihin, kirjaudu ensin Opintopolun tunnuksella tai suomi.fi-tunnistuksella. Lisää sitten MPASSid-tunnistetiedot omiin tietoihisi (https://virkailija.opintopolku.fi/henkilo-ui/omattiedot).");
                error.put(ERROR_DESC_SV, "Du har loggat in med MPASSid, men dina MPASSid-identifikationsuppgifter har inte bifogats till dina användaruppgifter i Studieinfo. Om du vill logga in med MPASSid i Studieinfos administratörstjänster, logga då först in med Studieinfo-användarnamnet eller suomi.fi-identifiering. Lägg sedan till MPASSid-identifikationsuppgifterna till dina egna uppgifter (https://virkailija.opintopolku.fi/henkilo-ui/omattiedot).");
            } else {
                error.put(ERROR_TITLE, "Haka-tunnistautumista ei ole aktivoitu");
                error.put(ERROR_TITLE_SV, "Haka-identifiering har inte aktiverats");
                error.put(ERROR_DESC, "Haka-tunnistautuminen onnistui, mutta Opintopolku-tiliisi ei ole liitetty Haka-käyttäjätunnusta. Mikäli haluat tunnistautua Haka-tunnuksilla, ole hyvä ja ota yhteyttä organisaatiosi pääkäyttäjään.");
                error.put(ERROR_DESC_SV, "Du har loggat in med Haka-identifiering, men Haka-användarnamnet har inte bifogats till dina användaruppgifter i Studieinfo för administratörer. Om du vill identifiera dig med ditt Haka-användarnamn, bör du ta kontakt med huvudanvändaren i din egen organisation.");
            }
        }
        else if (e instanceof RequiredSamlAttributeNotProvidedException) {
            String idpType = ((RequiredSamlAttributeNotProvidedException) e).getIdpType();
            if ("mpassid".equals(idpType)) {
                error.put(ERROR_TITLE, "MPASSid ei toimittanut vaadittuja tietoja");
                error.put(ERROR_TITLE_SV, "MPASSid förmedlade inte behövliga uppgifter");
                error.put(ERROR_DESC, "Palvelun käyttäminen vaatii, että sallit MPASSid:stä vaadittujen tietojen toimittamisen.");
                error.put(ERROR_DESC_SV, "För att använda tjänsten krävs att du tillåter att behövliga uppgifter från MPASSid kan förmedlas.");
            } else {
                error.put(ERROR_TITLE, "Haka ei toimittanut vaadittuja tietoja");
                error.put(ERROR_TITLE_SV, "Haka förmedlade inte behövliga uppgifter");
                error.put(ERROR_DESC, "Palvelun käyttäminen vaatii, että sallit Haka:sta vaadittujen tietojen toimittamisen.");
                error.put(ERROR_DESC_SV, "För att använda tjänsten krävs att du tillåter att behövliga uppgifter från Haka kan förmedlas.");
            }
        }

        if (error.get(ERROR_TITLE) == null) {
            error.put(ERROR_TITLE, "Odottamaton virhe tunnistautumisessa");
            error.put(ERROR_TITLE_SV, "Oväntat fel vid identifiering");
            error.put(ERROR_DESC, "Tunnistautumisessa tapahtui odottamaton virhe: </p><p>" + e.getMessage());
            error.put(ERROR_DESC_SV, "Ett oväntat fel inträffade vid identifiering: </p><p>" + e.getMessage());
        }

        logger.debug("Got a {}, sending following error page to user: {}: {}",
                e.toString(), error.get(ERROR_TITLE), error.get(ERROR_DESC));
        resp.setStatus(HttpServletResponse.SC_CONFLICT);
        req.getRequestDispatcher("/error.jsp").forward(req, resp);
    }
}
