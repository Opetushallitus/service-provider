/**
 * 
 */
package fi.vm.sade.saml.userdetails;

import fi.vm.sade.authentication.service.ServiceProviderService;
import fi.vm.sade.authentication.service.UserManagementService;
import fi.vm.sade.authentication.service.types.AddHenkiloData;
import fi.vm.sade.authentication.service.types.AddHenkiloToOrganisaatiosData;
import fi.vm.sade.authentication.service.types.dto.HenkiloDTO;
import fi.vm.sade.organisaatio.api.model.OrganisaatioService;
import fi.vm.sade.organisaatio.api.model.types.OrganisaatioDTO;
import fi.vm.sade.organisaatio.api.model.types.OrganisaatioSearchCriteriaDTO;
import fi.vm.sade.saml.userdetails.model.IdentityData;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml.SAMLCredential;

import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tommiha
 * 
 */
public abstract class AbstractIdpBasedAuthTokenProvider implements IdpBasedAuthTokenProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private List<String> supportedProviders;
    private ServiceProviderService serviceProviderService;
    private UserManagementService userManagementService;
    private OrganisaatioService organisaatioService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * fi.vm.sade.saml.userdetails.IdpBasedAuthTokenProvider#providesToken(java
     * .lang.String)
     */
    @Override
    public boolean providesToken(String idp) {
        if (supportedProviders != null) {
            return supportedProviders.contains(idp);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fi.vm.sade.saml.userdetails.IdpBasedAuthTokenProvider#
     * createAuthenticationToken
     * (org.springframework.security.saml.SAMLCredential)
     */
    @Override
    public String createAuthenticationToken(SAMLCredential credential) {
        HenkiloDTO henkilo = getServiceProviderService().getHenkiloByIDPAndIdentifier(getIDPUniqueKey(),
                getUniqueIdentifier(credential));
        if (henkilo == null) {
            IdentityData addHenkiloData = createIdentity(credential);
            henkilo = getUserManagementService().addHenkilo((AddHenkiloData) addHenkiloData);

            List<AddHenkiloToOrganisaatiosData> ohdatas = new ArrayList<AddHenkiloToOrganisaatiosData>();

            OrganisaatioSearchCriteriaDTO criteria = new OrganisaatioSearchCriteriaDTO();
            criteria.setOrganisaatioDomainNimi(addHenkiloData.getDomainNimi());
            List<OrganisaatioDTO> list = organisaatioService.searchOrganisaatios(criteria);

            // Ei pitäisi koskaan tulla yli yhtä kappaletta. Jos kumminkin
            // tulee, otetaan ensimmäinen..
            if (list != null && list.size() > 0) {
                AddHenkiloToOrganisaatiosData ohdata = new AddHenkiloToOrganisaatiosData();

                OrganisaatioDTO organisaatioDTO = list.get(0);
                ohdata.setOrganisaatioOid(organisaatioDTO.getOid());

                // ohdata.setMatkapuhelinnumero(oh.getMatkapuhelinnumero());
                // ohdata.setTehtavanimike(oh.getTehtavanimike());
                // ohdata.setPuhelinnumero(oh.getPuhelinnumero());
                ohdata.setSahkopostiosoite(addHenkiloData.getKayttajatunnus());

                ohdatas.add(ohdata);
            }

            henkilo = userManagementService.addHenkiloToOrganisaatios(henkilo.getOidHenkilo(), ohdatas);

        }
        return getServiceProviderService().generateAuthTokenForHenkilo(henkilo, getIDPUniqueKey(),
                getUniqueIdentifier(credential));
    }

    protected String getFirstAttributeValue(SAMLCredential credential, String attributeName) {
        Attribute attrib = null;
        for (Attribute attr : credential.getAttributes()) {
            if (attr.getFriendlyName().equalsIgnoreCase(attributeName)) {
                attrib = attr;
                break;
            }
        }

        if (attrib == null) {
            return null;
        }

        XMLObject obj = attrib.getAttributeValues().get(0);
        if (obj instanceof XSString) {
            return ((XSString) obj).getValue();
        }
        return null;
    }

    /**
     * Creates Henkilo from SAMLCredentials.
     * 
     * @param credential
     * @return
     */
    protected abstract IdentityData createIdentity(SAMLCredential credential);

    /**
     * Returns IDP unique key.
     * 
     * @return
     */
    protected abstract String getIDPUniqueKey();

    /**
     * 
     * @param credential
     * @return
     */
    protected abstract String getUniqueIdentifier(SAMLCredential credential);

    public List<String> getSupportedProviders() {
        return supportedProviders;
    }

    /**
     * Entity IDs (from SAML provider metadata) supported by this token
     * provider.
     * 
     * @param supportedProviders
     */
    public void setSupportedProviders(List<String> supportedProviders) {
        this.supportedProviders = supportedProviders;
    }

    public OrganisaatioService getOrganisaatioService() {
        return organisaatioService;
    }

    public void setOrganisaatioService(OrganisaatioService organisaatioService) {
        this.organisaatioService = organisaatioService;
    }

    public ServiceProviderService getServiceProviderService() {
        return serviceProviderService;
    }

    public void setServiceProviderService(ServiceProviderService serviceProviderService) {
        this.serviceProviderService = serviceProviderService;
    }

    public UserManagementService getUserManagementService() {
        return userManagementService;
    }

    public void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }
}
