package fi.vm.sade.saml;

import fi.vm.sade.saml.userdetails.UserDetailsDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import javax.servlet.http.HttpServletRequest;

public class SAMLLogoutFilterSkippingSingleLogoutForMpassid extends SAMLLogoutFilter {
    public SAMLLogoutFilterSkippingSingleLogoutForMpassid(LogoutSuccessHandler logoutSuccessHandler, LogoutHandler[] localHandler, LogoutHandler[] globalHandlers) {
        super(logoutSuccessHandler, localHandler, globalHandlers);
    }

    @Override
    protected boolean isGlobalLogout(HttpServletRequest request, Authentication auth) {
        // MPASSid doesn't support SingleLogoutService, so we can't do global logout
        if (isMpassid(auth)) return false;
        return super.isGlobalLogout(request, auth);
    }

    private boolean isMpassid(Authentication auth) {
        if (auth.getDetails() instanceof UserDetailsDto details) {
            return "mpassid".equals(details.getAuthenticationMethod());
        }
        return false;
    }
}
